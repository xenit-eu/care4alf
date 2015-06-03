package xenit.care4alf.module

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

import org.alfresco.repo.dictionary.DictionaryDAO
import com.github.dynamicextensionsalfresco.webscripts.annotations.Transaction
import org.alfresco.repo.policy.BehaviourFilter
import javax.annotation.Resource
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.annotations.AlfrescoService
import com.github.dynamicextensionsalfresco.annotations.ServiceType
import org.alfresco.service.cmr.repository.NodeService
import org.alfresco.service.cmr.dictionary.DictionaryService
import org.alfresco.repo.admin.SysAdminParams
import org.alfresco.repo.transaction.RetryingTransactionHelper
import java.lang
import java.io.Serializable
import org.alfresco.model.ContentModel
import org.springframework.extensions.webscripts.WebScriptResponse
import org.alfresco.service.namespace.QName
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable
import com.github.dynamicextensionsalfresco.webscripts.annotations.TransactionType
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod
import org.alfresco.service.cmr.repository.NodeRef
import eu.xenit.care4alf.web.LogHelper
import eu.xenit.care4alf.json
import eu.xenit.care4alf.JsonRoot
import org.alfresco.util.UrlUtil
import java.util.HashMap
import com.github.dynamicextensionsalfresco.webscripts.annotations.Attribute
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam
import org.alfresco.repo.security.authentication.AuthenticationUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Tools for validating/cleaning document models.
 *
 * author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/documentmodels", families = arrayOf("care4alf"), description = "validate document models")
Authentication(AuthenticationType.ADMIN)
class DocumentModels @Autowired constructor(
        dataSource: DataSource,
        AlfrescoService(ServiceType.LOW_LEVEL) val nodeService: NodeService,
        val dictionaryService: DictionaryService,
        val sysAdminParams: SysAdminParams,
        val transactionHelper: RetryingTransactionHelper,
        val dictionaryDAO: DictionaryDAO
) : LogHelper {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    val jdbcTemplate = JdbcTemplate(dataSource)

    Autowired Resource(name = "policyBehaviourFilter") var behaviourFilter: BehaviourFilter? = null

    Uri(value = "/invalidtypes")
    Transaction(readOnly = true)
    fun list() = json {
        val nodeIds = jdbcTemplate.queryForList("select id from alf_node", javaClass<lang.Long>())
        iterable(nodeIds) { id ->
            val nodeRef = nodeService.getNodeRef(id as Long)
            scanNode(nodeRef)
        }
    }

    Uri(value = "/node/{id}", method = HttpMethod.DELETE)
    Transaction(TransactionType.NONE)
    fun deleteDocument(UriVariable id: Long, response: WebScriptResponse) {
        val nodeRef = nodeService.getNodeRef(id)
        if (nodeRef != null) {
            try {
                AuthenticationUtil.runAsSystem {
                    behaviourFilter?.disableBehaviour()
                    transactionHelper.doInTransaction {
                        nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, HashMap<QName, Serializable>(0))
                        nodeService.deleteNode(nodeRef)
                    }
                }
            } catch (e: Exception) {
                logger.error("failed to delete invalid node $nodeRef", e)
                response.getWriter().append(e.getMessage())
                response.setStatus(500)
            }
        }
    }

    fun scanNode(node: NodeRef): JsonRoot.(NodeRef) -> Unit {
        return {
            val typeDefinition = dictionaryService.getType(nodeService.getType(node))
            if (typeDefinition == null) {
                obj {
                    entry("type", nodeService.getType(node))
                    entry("name", nodeService.getProperty(node, ContentModel.PROP_NAME))
                    entry("id", nodeService.getProperty(node, ContentModel.PROP_NODE_DBID))
                    entry("url", (UrlUtil.getShareUrl(sysAdminParams) + "/page/folder-details?nodeRef=" + node))
                }
            }
        }
    }

    Uri(value = "/models", defaultFormat = "json", method = HttpMethod.GET)
    fun listModels() = json {
        // API change in 5.0 getModels() -> getModels(boolean)
        val getter = dictionaryDAO.javaClass.getMethods().filter({ it.getName() == "getModels"}).firstOrNull()
        if (getter != null) {
            val models = if (getter.getParameterTypes().isEmpty()) {
                getter.invoke(dictionaryDAO) as Collection<Any>
            } else {
                getter.invoke(dictionaryDAO, true) as Collection<Any>
            }
            iterable(models) { model ->
                value(model.toString())
            }
        }
    }

    Uri(value = "/model", defaultFormat = "json", method = HttpMethod.DELETE)
    fun removeModel(RequestParam modelQName: String) {
        dictionaryDAO.removeModel(QName.createQName(modelQName))
    }
}

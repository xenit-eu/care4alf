package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService
import com.github.dynamicextensionsalfresco.annotations.ServiceType
import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import eu.xenit.care4alf.JsonRoot
import eu.xenit.care4alf.json
import eu.xenit.care4alf.web.LogHelper
import org.alfresco.model.ContentModel
import org.alfresco.repo.admin.SysAdminParams
import org.alfresco.repo.dictionary.DictionaryDAO
import org.alfresco.repo.policy.BehaviourFilter
import org.alfresco.repo.security.authentication.AuthenticationUtil
import org.alfresco.repo.transaction.RetryingTransactionHelper
import org.alfresco.service.cmr.dictionary.DictionaryService
import org.alfresco.service.cmr.repository.NodeRef
import org.alfresco.service.cmr.repository.NodeService
import org.alfresco.service.namespace.QName
import org.alfresco.util.UrlUtil
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.extensions.webscripts.WebScriptResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.*
import javax.annotation.Resource
import javax.sql.DataSource

/**
 * Tools for validating/cleaning document models.
 *
 * author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/documentmodels", families = arrayOf("care4alf"), description = "validate document models")
@Authentication(AuthenticationType.ADMIN)
class DocumentModels @Autowired constructor(
        dataSource: DataSource,
        @AlfrescoService(ServiceType.LOW_LEVEL) val nodeService: NodeService,
        val dictionaryService: DictionaryService,
        val sysAdminParams: SysAdminParams,
        val transactionHelper: RetryingTransactionHelper,
        val dictionaryDAO: DictionaryDAO
) : LogHelper() {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    val jdbcTemplate = JdbcTemplate(dataSource)

    @Autowired @Resource(name = "policyBehaviourFilter") var behaviourFilter: BehaviourFilter? = null

    @Uri("/invalidtypes")
    @Transaction(readOnly = true)
    fun list() = json {
        val nodeIds = jdbcTemplate.queryForList("select id from alf_node", Long::class.java)
        iterable(nodeIds) { id ->
            val nodeRef = nodeService.getNodeRef(id as Long)
            scanNode(nodeRef)
        }
    }

    @Uri("/node/{id}", method = HttpMethod.DELETE)
    @Transaction(TransactionType.NONE)
    fun deleteDocument(@UriVariable id: Long, response: WebScriptResponse) {
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
                response.getWriter().append(e.message)
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

    @Uri("/models", defaultFormat = "json", method = HttpMethod.GET)
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

    @Uri("/model", defaultFormat = "json", method = HttpMethod.DELETE)
    fun removeModel(@RequestParam modelQName: String) {
        dictionaryDAO.removeModel(QName.createQName(modelQName))
    }
}

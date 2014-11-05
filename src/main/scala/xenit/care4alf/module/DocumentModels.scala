package xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

import scala.collection.JavaConversions._
import org.alfresco.service.cmr.repository.{NodeRef, NodeService}
import com.github.dynamicextensionsalfresco.annotations.{Transactional, ServiceType, AlfrescoService}

import xenit.care4alf.spring.ContextAware
import xenit.care4alf.web.{JsonHelper, Json}
import org.alfresco.service.cmr.dictionary.DictionaryService
import org.json.JSONWriter
import org.alfresco.model.ContentModel
import org.alfresco.repo.admin.SysAdminParams
import org.alfresco.util.UrlUtil
import java.sql.ResultSet
import xenit.care4alf.jdbc.Implicits._
import java.util
import org.alfresco.service.namespace.QName
import java.io.Serializable
import org.springframework.extensions.webscripts.WebScriptResponse
import org.alfresco.repo.security.authentication.AuthenticationUtil._
import xenit.care4alf.alfresco.Implicits._
import org.alfresco.repo.policy.BehaviourFilter
import javax.annotation.Resource
import org.alfresco.repo.transaction.{RetryingTransactionHelper, TransactionUtil}
import com.typesafe.scalalogging.slf4j.Logging
import org.alfresco.repo.dictionary.DictionaryDAO

/**
 * Tools for validating/cleaning document models.
 *
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/documentmodels", families = Array("care4alf"), description = "validate document models")
@Authentication(AuthenticationType.ADMIN)
class DocumentModels @Autowired()(
            dataSource: DataSource,
            @AlfrescoService(ServiceType.LOW_LEVEL) nodeService: NodeService,
            dictionaryService: DictionaryService,
            sysAdminParams: SysAdminParams,
            transactionHelper: RetryingTransactionHelper
        ) extends ContextAware with Logging with Json {

    // cannot specify @Resource parameters on constructor
    @Autowired @Resource(name = "policyBehaviourFilter") var behaviourFilter: BehaviourFilter = null
    @Autowired var dictionaryDAO: DictionaryDAO = null

    private[this] val jdbc = new JdbcTemplate(dataSource)

    @Uri(value = Array("/invalidtypes"))
    @Transaction(readOnly = true)
    def list(@Attribute jsonHelper: JsonHelper) {
        implicit val json = jsonHelper.json
        val nodeIds = jdbc.query("select id from alf_node", (set: ResultSet) => {set.getLong(1)})
        json.array()
        for (id <- nodeIds) {
            val nodeRef = nodeService.getNodeRef(id)
            if (nodeRef != null) scanNode(nodeRef)
        }

        json.endArray()
    }

    @Uri(value = Array("/node/{id}"), method = HttpMethod.DELETE)
    @Transaction(TransactionType.NONE)
    def deleteDocument(@UriVariable id: Long, response: WebScriptResponse) {
        val nodeRef = nodeService.getNodeRef(id)
        if (nodeRef != null) {
            try {
                runAsSystem {
                    behaviourFilter.disableBehaviour()
                    transactionHelper.doInTransaction {
                        nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, new util.HashMap[QName, Serializable](0))
                        nodeService.deleteNode(nodeRef)
                    }
                }
            }
            catch {
                case e: Exception => {
                    logger.error(s"failed to delete invalid node $nodeRef", e)
                    response.getWriter.append(e.getMessage)
                    response.setStatus(500)
                }
            }
        }
    }

    def scanNode(node: NodeRef)(implicit json: JSONWriter) {
        val typeDefinition = dictionaryService.getType(nodeService.getType(node))
        if (typeDefinition == null) {
            json.`object`()
                .key("type").value(nodeService.getType(node))
                .key("name").value(nodeService.getProperty(node, ContentModel.PROP_NAME).asInstanceOf[String])
                .key("id").value(nodeService.getProperty(node, ContentModel.PROP_NODE_DBID).asInstanceOf[Long])
                .key("url").value(UrlUtil.getShareUrl(sysAdminParams) + "/page/folder-details?nodeRef=" + node)
                .endObject()
        }
    }

    @Uri(value = Array("/models"), defaultFormat = "json", method = HttpMethod.GET)
    def listModels(@Attribute jsonHelper: JsonHelper) {
        val json = jsonHelper.json
        json.array()
        for (model <- dictionaryDAO.getModels()) {
            json.value(model.toString)
        }
        json.endArray()
    }

    @Uri(value = Array("/model"), defaultFormat = "json", method = HttpMethod.DELETE)
    def removeModel(@RequestParam modelQName: String) {
        dictionaryDAO.removeModel(QName.createQName(modelQName))
    }
}

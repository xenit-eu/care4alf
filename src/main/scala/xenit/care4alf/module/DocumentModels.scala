package xenit.care4alf.module

import org.springframework.stereotype.Component
import nl.runnable.alfresco.webscripts.annotations._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

import scala.collection.JavaConversions._
import org.alfresco.service.cmr.repository.{NodeRef, NodeService}
import nl.runnable.alfresco.annotations.{ServiceType, AlfrescoService}

import xenit.care4alf.spring.ContextAware
import xenit.care4alf.Logger
import xenit.care4alf.web.{JsonHelper, Json}
import org.alfresco.service.cmr.dictionary.DictionaryService
import org.json.JSONWriter
import org.alfresco.model.ContentModel
import org.alfresco.repo.admin.SysAdminParams
import org.alfresco.util.UrlUtil
import java.sql.ResultSet
import xenit.care4alf.jdbc.JdbcTemplateExtensions._

/**
 * Tools for validating/cleaning document models.
 *
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/documentmodels", families = Array("care4alf"), description = "validate document models")
class DocumentModels @Autowired()(
            dataSource: DataSource,
            @AlfrescoService(ServiceType.LOW_LEVEL) nodeService: NodeService,
            dictionaryService: DictionaryService,
            sysAdminParams: SysAdminParams
        ) extends ContextAware with Logger with Json {

    private[this] val jdbc = new JdbcTemplate(dataSource)

    @Uri(value = Array("/invalidtypes"))
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
    def deleteDocument(@UriVariable id: Long) {
        nodeService.deleteNode(nodeService.getNodeRef(id))
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
}

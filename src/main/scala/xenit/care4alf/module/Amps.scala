package xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations._
import org.json.JSONObject
import org.springframework.extensions.webscripts.WebScriptRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

import scala.collection.JavaConversions._
import org.alfresco.service.cmr.repository.NodeService
import org.alfresco.service.namespace.QName
import com.github.dynamicextensionsalfresco.annotations.{ServiceType, AlfrescoService}
import java.sql.ResultSet

import xenit.care4alf.jdbc.Implicits._
import xenit.care4alf.spring.ContextAware
import xenit.care4alf.Logger
import xenit.care4alf.web.{JsonHelper, Json}
import org.alfresco.util.VersionNumber
import xenit.care4alf.web.Json._

/**
 * Update Alfresco's AMP version in case you want to downgrade.
 *
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/amps", families = Array("care4alf"), description = "update AMP module versions")
class Amps @Autowired()(
            dataSource: DataSource,
            @AlfrescoService(ServiceType.LOW_LEVEL) nodeService: NodeService
        ) extends ContextAware with Logger with Json {

    private[this] val jdbc = new JdbcTemplate(dataSource)

    @Uri(value = Array("/list"), defaultFormat = "json")
    @Transaction(readOnly = true)
    def list(@Attribute jsonHelper: JsonHelper) {
        implicit  val json = jsonHelper.json
        json.array()
        val versionIds = getVersionQnameIds.mkString(",")
        jdbc.query(s"select distinct(node_id) from alf_node_properties where qname_id in ($versionIds)", (set: ResultSet) => {
            set.getLong(1)
        }).foreach { nodeId =>
            nodeService.getNodeRef(nodeId).toJson(nodeService)
        }
        json.endArray()
    }

    @Uri(value = Array("/save"), method = HttpMethod.POST)
    def save(request: WebScriptRequest) {
        val json = new JSONObject(request.getContent.getContent)
        val dbid = json.getLong("{http://www.alfresco.org/model/system/1.0}node-dbid")
        val currentVersion = json.getString("{http://www.alfresco.org/system/modules/1.0}currentVersion")
        val installedVersion = json.getString("{http://www.alfresco.org/system/modules/1.0}installedVersion")

        val nodeRef = nodeService.getNodeRef(dbid)
        nodeService.setProperty(
            nodeRef,
            QName.createQName("{http://www.alfresco.org/system/modules/1.0}currentVersion"),
            new VersionNumber(currentVersion)
        )
        nodeService.setProperty(
            nodeRef,
            QName.createQName("{http://www.alfresco.org/system/modules/1.0}installedVersion"),
            new VersionNumber(installedVersion)
        )
    }

    private def getVersionQnameIds = {
        jdbc.query(
            "SELECT id FROM alf_qname WHERE local_name IN ('currentVersion','installedVersion')"
            , (set: ResultSet) => set.getString(1)
        )
    }
}

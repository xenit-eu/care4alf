package xenit.care4alf.module

import org.springframework.stereotype.Component
import org.json.JSONObject
import org.springframework.extensions.webscripts.WebScriptRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

import org.alfresco.service.cmr.repository.NodeService
import org.alfresco.service.namespace.QName
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import com.github.dynamicextensionsalfresco.webscripts.annotations.Transaction
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import org.slf4j.LoggerFactory
import com.github.dynamicextensionsalfresco.annotations.AlfrescoService
import com.github.dynamicextensionsalfresco.annotations.ServiceType
import eu.xenit.care4alf.web.LogHelper
import eu.xenit.care4alf.json
import kotlin.jdbc.query
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod
import java.sql.ResultSet
import org.alfresco.util.VersionNumber
import java.lang

/**
 * Update Alfresco's AMP version in case you want to downgrade.
 *
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/amps", families = array("care4alf"), description = "update AMP module versions")
Authentication(AuthenticationType.ADMIN)
class Amps [Autowired](
            dataSource: DataSource,
            AlfrescoService(ServiceType.LOW_LEVEL) val nodeService: NodeService
        ) : LogHelper {

    protected override val logger = LoggerFactory.getLogger(javaClass)

    private val jdbc = JdbcTemplate(dataSource)

    Uri(value = array("/list"), defaultFormat = "json")
    Transaction(readOnly = true)
    fun list() = json {
        val versionIds = getVersionQnameIds().join(",")
        val ids = jdbc.queryForList("select distinct(node_id) from alf_node_properties where qname_id in ($versionIds)", javaClass<lang.Long>())
        val nodeRefs = ids.map({ id ->
            nodeService.getNodeRef(id as Long)
        })
        iterable(nodeRefs) { nodeRef ->
            obj {
                nodeService.getProperties(nodeRef).forEach { pair ->
                    entry(pair.getKey().toString(), pair.getValue())
                }
            }
        }
    }

    Uri(value = array("/save"), method = HttpMethod.POST)
    fun save(json: JSONObject) {
        val dbid = json.getLong("{http://www.alfresco.org/model/system/1.0}node-dbid")
        val currentVersion = json.getString("{http://www.alfresco.org/system/modules/1.0}currentVersion")
        val installedVersion = json.getString("{http://www.alfresco.org/system/modules/1.0}installedVersion")

        val nodeRef = nodeService.getNodeRef(dbid)
        nodeService.setProperty(
            nodeRef,
            QName.createQName("{http://www.alfresco.org/system/modules/1.0}currentVersion"),
            VersionNumber(currentVersion)
        )
        nodeService.setProperty(
            nodeRef,
            QName.createQName("{http://www.alfresco.org/system/modules/1.0}installedVersion"),
            VersionNumber(installedVersion)
        )
    }

    private fun getVersionQnameIds(): List<String> = jdbc.queryForList(
        "SELECT id FROM alf_qname WHERE local_name IN ('currentVersion','installedVersion')",
        javaClass<String>()
    )
}

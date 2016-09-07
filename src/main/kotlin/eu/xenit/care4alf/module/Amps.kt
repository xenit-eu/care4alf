package xenit.care4alf.module

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService
import com.github.dynamicextensionsalfresco.annotations.ServiceType
import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import eu.xenit.care4alf.json
import eu.xenit.care4alf.web.LogHelper
import org.alfresco.service.cmr.repository.NodeService
import org.alfresco.service.namespace.QName
import org.alfresco.util.VersionNumber
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * Update Alfresco's AMP version in case you want to downgrade.
 *
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/amps", families = arrayOf("care4alf"), description = "update AMP module versions")
@Authentication(AuthenticationType.ADMIN)
class Amps @Autowired constructor(
            dataSource: DataSource,
            @AlfrescoService(ServiceType.LOW_LEVEL) val nodeService: NodeService
        ) : LogHelper() {

    override val logger = LoggerFactory.getLogger(javaClass)

    private val jdbc = JdbcTemplate(dataSource)

    @Uri(value = "/list", defaultFormat = "json")
    @Transaction(readOnly = true)
    fun list() = json {
        val versionIds = getVersionQnameIds().joinToString(",")
        val ids = jdbc.queryForList("select distinct(node_id) from alf_node_properties where qname_id in ($versionIds)", Long::class.java)
        val nodeRefs = ids.map({ id ->
            nodeService.getNodeRef(id as Long)
        })
        iterable(nodeRefs) { nodeRef ->
            obj {
                nodeService.getProperties(nodeRef).forEach { pair ->
                    entry(pair.key.toString(), pair.value)
                }
            }
        }
    }

    @Uri(value = "/save", method = HttpMethod.POST)
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
        String::class.java
    )
}

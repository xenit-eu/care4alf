package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService
import com.github.dynamicextensionsalfresco.annotations.ServiceType
import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution
import eu.xenit.care4alf.json
import eu.xenit.care4alf.web.LogHelper
import org.alfresco.model.ContentModel
import org.alfresco.query.PagingRequest
import org.alfresco.repo.domain.node.ContentDataWithId
import org.alfresco.service.cmr.notification.NotificationService
import org.alfresco.service.cmr.repository.ContentService
import org.alfresco.service.cmr.repository.NodeRef
import org.alfresco.service.cmr.repository.NodeService
import org.alfresco.service.cmr.security.PersonService
import org.alfresco.service.namespace.QName
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import java.io.Serializable
import java.util.*
import javax.sql.DataSource

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/contentstore", families = arrayOf("care4alf"), description = "Content store verification")
@Authentication(AuthenticationType.ADMIN)
public class ContentStore @Autowired constructor(
                                      private val applicationContext: ApplicationContext,
                                      dataSource: DataSource,
                                      private val personService: PersonService,
                                      private val notification: NotificationService,
                                      private val contentService: ContentService,
                                      @AlfrescoService(ServiceType.LOW_LEVEL) private val nodeService: NodeService
                            ) : LogHelper() {

    override val logger: Logger = LoggerFactory.getLogger(javaClass)

    private val jdbcTemplate = JdbcTemplate(dataSource)

    private val WorkspaceDiskUsage = QName.createQName("WorkspaceDiskUsage")
    private val ArchiveDiskUsage = QName.createQName("ArchiveDiskUsage")

    @Uri(value = "/diskusagebyowner")
    @Transaction(readOnly = true)
    fun list() = json {
        val people = personService.getPeople(null, true, null, PagingRequest(Integer.MAX_VALUE, null)).getPage().map({it.getNodeRef()})
        iterable(people) { person ->
            val workspace = nodeService.getProperty(person, WorkspaceDiskUsage) as? Long
            val archive = nodeService.getProperty(person, ArchiveDiskUsage) as? Long
            if (workspace != null || archive != null) {
                obj {
                  entry("username", nodeService.getProperty(person, ContentModel.PROP_USERNAME))
                  entry("workspace", workspace)
                  entry("archive", archive)
                }
            }
        }
    }

    @Uri(value = "/updatediskusage", method = HttpMethod.PUT)
    fun update() {
        val workspace = HashMap<String, Long>()
        val archive = HashMap<String, Long>()
        jdbcTemplate.queryForList("select id from alf_node", Long::class.java).forEach { id ->
            val nodeRef = nodeService.getNodeRef(id as Long)
            if (nodeRef != null) {
                val content = nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT) as? ContentDataWithId
                val owner = nodeService.getProperty(nodeRef, ContentModel.PROP_OWNER) as? String
                if (content != null && owner != null) {
                    if (nodeRef.getStoreRef().getProtocol().equals("archive")) {
                        archive.put(owner, archive.getOrElse(owner, {0L}) + content.getSize())
                    } else {
                        workspace.put(owner, workspace.getOrElse(owner, {0L}) + content.getSize())
                    }
                }
            }
        }

        workspace.forEach { ou ->
            val person = personService.getPerson(ou.key)
            nodeService.setProperty(person, WorkspaceDiskUsage, ou.value as Serializable)
            nodeService.let {  }
        }

        archive.forEach { ou ->
            val person = personService.getPerson(ou.key)
            nodeService.setProperty(person, ArchiveDiskUsage, ou.value as Serializable)
        }
    }

    @Uri(value = "/checkintegrity", method = HttpMethod.GET)
    fun checkintegrity(): Resolution {
        val missingContent = arrayListOf<MissingContent>()
        jdbcTemplate.queryForList("select id from alf_node", Long::class.java).forEach { id ->
            val nodeRef = nodeService.getNodeRef(id as Long)
            if (nodeRef != null) {
                val content = nodeService.getProperty(nodeRef, ContentModel.PROP_CONTENT) as? ContentDataWithId
                if (content != null) {
                    try {
                        contentService.getReader(nodeRef, ContentModel.PROP_CONTENT).getContentInputStream().close()
                    } catch(ex: Exception) {
                        var contentUrl="<none>";
                        if(content !=null && content.getContentUrl() != null)
                            contentUrl=content.getContentUrl();
                        missingContent.add(MissingContent(nodeRef, contentUrl, ex.message))
                    }
                }
            }
        }

        return json {
            iterable(missingContent) { missingContent ->
                obj {
                    entry("noderef", missingContent.noderef)
                    entry("contentUrl", missingContent.contentUrl)
                    entry("cause", missingContent.cause)
                }
            }
        }
    }
}

class MissingContent(val noderef: NodeRef, val contentUrl: String?, val cause: String?)

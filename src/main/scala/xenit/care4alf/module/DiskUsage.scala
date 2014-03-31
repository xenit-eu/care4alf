package xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate


import java.sql.ResultSet
import xenit.care4alf.jdbc.Implicits._
import xenit.care4alf.spring.ContextAware
import com.typesafe.scalalogging.slf4j.Logging
import javax.sql.DataSource
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution
import org.json.JSONWriter
import xenit.care4alf.alfresco.HasNodeService
import org.alfresco.model.ContentModel
import scala.collection.mutable

import scala.collection.JavaConversions._
import org.alfresco.repo.domain.node.ContentDataWithId
import org.alfresco.service.cmr.security.PersonService
import org.alfresco.service.cmr.notification.NotificationService
import org.alfresco.service.namespace.QName
import org.alfresco.query.PagingRequest

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/diskusage", families = Array("care4alf"), description = "display disk usage by owner")
@Authentication(AuthenticationType.ADMIN)
class DiskUsage extends ContextAware with Logging with HasNodeService {

    @Autowired var dataSource: DataSource = null
    @Autowired var personService: PersonService = null
    @Autowired var notification: NotificationService = null

    private[this] lazy val jdbc = new JdbcTemplate(dataSource)

    @Uri(value = Array("/byowner"))
    @Transaction(readOnly = true)
    def list() = new JsonWriterResolution {
        override def writeJson(json: JSONWriter): Unit = {
            json.array()
            val people = personService.getPeople(null, true, null, new PagingRequest(Integer.MAX_VALUE, null)).getPage.map(_.getNodeRef)
            for (person <- people) {
                val workspace = person(QName.createQName("WorkspaceDiskUsage")).asInstanceOf[Long]
                val archive = person(QName.createQName("ArchiveDiskUsage")).asInstanceOf[Long]
                if (workspace > 0 || archive > 0) {
                    json
                        .`object`()
                            .key("username").value(person(ContentModel.PROP_USERNAME).asInstanceOf[String])
                            .key("workspace").value(workspace)
                            .key("archive").value(archive)
                        .endObject()
                }
            }
            json.endArray()
        }
    }

    @Uri(value = Array("/update"), method = HttpMethod.PUT)
    def update() {
        val workspace = new mutable.HashMap[String,Long]()
        val archive = new mutable.HashMap[String,Long]()
        val nodeIds = jdbc.query("select id from alf_node", (set: ResultSet) => {set.getLong(1)})
        for (id <- nodeIds) {
            val nodeRef = nodeService.getNodeRef(id)
            if (nodeRef != null) {
                val content = nodeRef(ContentModel.PROP_CONTENT).asInstanceOf[ContentDataWithId]
                val owner = nodeRef(ContentModel.PROP_OWNER).asInstanceOf[String]
                if (content != null && owner != null) {
                    if (nodeRef.getStoreRef.getProtocol.equals("archive")) {
                        archive.put(owner, archive.getOrElse(owner, 0l) + content.getSize)
                    } else {
                        workspace.put(owner, workspace.getOrElse(owner, 0l) + content.getSize)
                    }
                }
            }
        }

        for (ou <- workspace) {
            val person = personService.getPerson(ou._1)
            person(QName.createQName("WorkspaceDiskUsage")) = ou._2
        }

        for (ou <- archive) {
            val person = personService.getPerson(ou._1)
            person(QName.createQName("ArchiveDiskUsage")) = ou._2
        }
    }
}

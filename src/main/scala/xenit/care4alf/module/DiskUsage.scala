package xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations._
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate


import java.sql.ResultSet
import xenit.care4alf.jdbc.Implicits._
import org.alfresco.repo.policy.BehaviourFilter
import xenit.care4alf.spring.ContextAware
import com.typesafe.scalalogging.slf4j.Logging
import javax.annotation.Resource
import org.alfresco.repo.dictionary.DictionaryDAO
import javax.sql.DataSource
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution
import org.json.JSONWriter
import xenit.care4alf.alfresco.HasNodeService
import org.alfresco.model.ContentModel
import org.alfresco.service.cmr.dictionary.DictionaryService
import scala.collection.mutable

import scala.collection.JavaConversions._
import org.alfresco.repo.domain.node.ContentDataWithId

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/diskusage", families = Array("care4alf"), description = "display disk usage by owner")
@Authentication(AuthenticationType.ADMIN)
class DiskUsage extends ContextAware with Logging with HasNodeService {

    // cannot specify @Resource parameters on constructor
    @Autowired @Resource(name = "policyBehaviourFilter") var behaviourFilter: BehaviourFilter = null
    @Autowired var dictionaryDAO: DictionaryDAO = null
    @Autowired var dataSource: DataSource = null
    @Autowired var dictionaryService: DictionaryService = null

    private[this] lazy val jdbc = new JdbcTemplate(dataSource)

    @Uri(value = Array("/byowner"))
    @Transaction(readOnly = true)
    def list() = new JsonWriterResolution {
        override def writeJson(json: JSONWriter): Unit = {
            val usage = new mutable.HashMap[String,Long]()
            val nodeIds = jdbc.query("select id from alf_node", (set: ResultSet) => {set.getLong(1)})
            for (id <- nodeIds) {
                val nodeRef = nodeService.getNodeRef(id)
                if (nodeRef != null) {
                    val content = nodeRef(ContentModel.PROP_CONTENT).asInstanceOf[ContentDataWithId]
                    val owner = nodeRef(ContentModel.PROP_OWNER).asInstanceOf[String]
                    if (content != null && owner != null) {
                        usage.put(owner, usage.getOrElse(owner, 0l) + content.getSize)
                    }
                }
            }
            json.`object`()
            for (ou <- usage) {
                json.key(ou._1).value(ou._2)
            }
            json.endObject()
        }
    }
}

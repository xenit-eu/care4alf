package xenit.care4alf.web

import com.github.dynamicextensionsalfresco.webscripts.annotations.{Attribute, Uri, WebScript}
import org.springframework.stereotype.Component
import scala.collection.JavaConversions._
import xenit.care4alf.spring.ContextAware
import xenit.care4alf.web.Json._
import xenit.care4alf._

/**
 * Find module webscripts by familiy
 *
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf")
class Care4Alf extends ContextAware with Json with Versioned {
    @Uri(value = Array("/"), defaultFormat = "html")
    def index = {
        JavaMap("modules" -> JavaList(getModuleWebScripts.map((entry) => {
            val description = entry._2.getClass.getAnnotation(classOf[WebScript]).description()
            JavaMap("id" -> entry._1.toLowerCase, "description" -> description)
        })))
    }

    def getModuleWebScripts = {
        applicationContext.getBeansWithAnnotation(classOf[WebScript]).filter {
            _._2.getClass.getAnnotation(classOf[WebScript]).families().contains("care4alf")
        }
    }
}

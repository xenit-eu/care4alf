package xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations._
import org.springframework.beans.factory.annotation.Autowired

import scala.collection.JavaConversions._

import xenit.care4alf.spring.ContextAware
import xenit.care4alf.web.Json
import com.typesafe.scalalogging.slf4j.Logging
import org.alfresco.repo.security.authentication.TicketComponent
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution
import org.json.JSONWriter

/**
 * Update Alfresco's AMP version in case you want to downgrade.
 *
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/tickets", families = Array("care4alf"), description = "manage session tickets")
@Authentication(AuthenticationType.ADMIN)
class Tickets extends ContextAware with Logging with Json {

    @Autowired
    var ticketService: TicketComponent = null

    @Uri(value = Array("/list"), defaultFormat = "json")
    @Transaction(readOnly = true)
    def list = new JsonWriterResolution {
        def writeJson(json: JSONWriter) {
            json.array()
            for (ticket <- ticketService.getUsersWithTickets(false)) {
                json.value(ticket)
            }
            json.endArray()
        }
    }

    @Uri(value = Array("/expire/{username}"), method = HttpMethod.DELETE)
    def expire(@UriVariable username: String) {
        ticketService.invalidateTicketByUser(username)
    }
}

package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.repo.security.authentication.TicketComponent
import eu.xenit.care4alf.json
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/tickets", families = arrayOf("care4alf"), description = "manage session tickets")
Authentication(AuthenticationType.ADMIN)
public class Tickets @Autowired constructor(private val ticketService: TicketComponent) {
    Uri(value = "/list", defaultFormat = "json")
    fun list() = json {
        iterable(ticketService.getUsersWithTickets(false)) { ticket ->
            value(ticket)
        }
    }

    Uri(value = "/expire/{username}", method = HttpMethod.DELETE)
    fun expire(UriVariable username: String) {
        ticketService.invalidateTicketByUser(username)
    }
}
package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import eu.xenit.care4alf.json
import org.alfresco.repo.security.authentication.TicketComponent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/tickets", families = arrayOf("care4alf"), description = "manage session tickets")
@Authentication(AuthenticationType.ADMIN)
public class Tickets @Autowired constructor(private val ticketService: TicketComponent) {
    @Uri(value = ["/list"], defaultFormat = "json")
    fun list() = json {
        iterable(ticketService.getUsersWithTickets(false)) { ticket ->
            value(ticket)
        }
    }

    @Uri(value = ["/expire/{username}"], method = HttpMethod.DELETE)
    fun expire(@UriVariable username: String) {
        ticketService.invalidateTicketByUser(username)
    }
}
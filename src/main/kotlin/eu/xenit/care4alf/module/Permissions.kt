package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import eu.xenit.care4alf.json
import org.alfresco.service.cmr.security.AuthorityService
import org.alfresco.service.cmr.security.AuthorityType
import org.alfresco.service.cmr.security.PermissionService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/permissions", families = arrayOf("care4alf"), description = "Permissions")
@Authentication(AuthenticationType.ADMIN)
public class Permission @Autowired constructor(private val permissionService: PermissionService, private val authorityService: AuthorityService) {
    val logger = LoggerFactory.getLogger(javaClass)

    @Uri()
    fun get(@RequestParam group: String) = json {
        val results = authorityService.getContainingAuthorities(AuthorityType.GROUP, group, false)
        iterable(results) { authority ->
            value(authority)
        }
    }
}
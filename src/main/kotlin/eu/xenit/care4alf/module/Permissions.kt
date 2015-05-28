package eu.xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam
import eu.xenit.care4alf.json
import org.springframework.beans.factory.annotation.Autowired
import org.slf4j.LoggerFactory
import org.alfresco.service.cmr.security.AuthorityService
import org.alfresco.service.cmr.security.PermissionService
import org.alfresco.service.cmr.security.AuthorityType

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/permissions", families = array("care4alf"), description = "Permissions")
Authentication(AuthenticationType.ADMIN)
public class Permission [Autowired](private val permissionService: PermissionService, private val authorityService: AuthorityService) {
    val logger = LoggerFactory.getLogger(javaClass)

    Uri()
    fun get(RequestParam group: String) = json {
        val results = authorityService.getContainingAuthorities(AuthorityType.GROUP, group, false)
        iterable(results) { authority ->
            value(authority)
        }
    }
}
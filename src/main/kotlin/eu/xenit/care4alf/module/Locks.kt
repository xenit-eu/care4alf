package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import eu.xenit.care4alf.json
import org.alfresco.service.cmr.repository.NodeRef
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam
import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.cmr.lock.LockService
import org.alfresco.repo.security.authentication.AuthenticationUtil

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/locks", families = array("care4alf"), description = "manage locks")
Authentication(AuthenticationType.ADMIN)
public class Locks [Autowired](val lockService: LockService) {
    Uri(array("/"))
    fun getLockInfo(RequestParam noderef: NodeRef) = json {
        AuthenticationUtil.runAsSystem {
            val lockState = lockService.getLockState(noderef)
            obj {
                entry("owner", lockState.getOwner())
                entry("locktype", lockState.getLockType())
                entry("lifetime", lockState.getLifetime())
                entry("expires", lockState.getExpires())
                entry("additionalinfo", lockState.getAdditionalInfo())
            }
        }
    }
}
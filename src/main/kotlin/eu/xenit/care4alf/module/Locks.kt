package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import eu.xenit.care4alf.json
import org.alfresco.repo.security.authentication.AuthenticationUtil
import org.alfresco.service.cmr.lock.LockService
import org.alfresco.service.cmr.repository.NodeRef
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author Laurent Van der Linden
 */
//Component
//WebScript(baseUri = "/xenit/care4alf/locks", families = arrayOf("care4alf"), description = "manage locks")
//Authentication(AuthenticationType.ADMIN)
public class Locks @Autowired constructor(val lockService: LockService) {
    @Uri("/")
    fun getLockInfo(@RequestParam noderef: NodeRef) = json {
        AuthenticationUtil.runAsSystem {
            //LockState s = null
            lockService
                val lockState = lockService.getLockState(noderef)

                /*obj {
                    entry("owner", lockState.getOwner())
                    entry("locktype", lockState.getLockType())
                    entry("lifetime", lockState.getLifetime())
                    entry("expires", lockState.getExpires())
                    entry("additionalinfo", lockState.getAdditionalInfo())
                }*/
                obj { entry("hello",lockState.getExpires())}

        }
    }
}
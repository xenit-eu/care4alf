package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.*
import org.springframework.stereotype.Component
import eu.xenit.care4alf.json
import org.alfresco.service.cmr.repository.NodeRef
import org.alfresco.model.ContentModel
import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.cmr.lock.LockService
import org.alfresco.repo.security.authentication.AuthenticationUtil
import org.alfresco.repo.transaction.RetryingTransactionHelper
import org.alfresco.service.namespace.QName
import java.io.Serializable
import java.util.*
import org.alfresco.repo.lock.mem.LockState

/**
 * @author Laurent Van der Linden
 */
//Component
//WebScript(baseUri = "/xenit/care4alf/locks", families = arrayOf("care4alf"), description = "manage locks")
//Authentication(AuthenticationType.ADMIN)
public class Locks @Autowired constructor(val lockService: LockService) {
    Uri("/")
    fun getLockInfo(RequestParam noderef: NodeRef) = json {
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
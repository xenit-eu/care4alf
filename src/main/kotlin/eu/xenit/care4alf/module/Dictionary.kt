package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import eu.xenit.care4alf.json
import org.alfresco.service.namespace.NamespaceService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

/**
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/dictionary", families = arrayOf("care4alf"), description = "Dictionary info")
@Authentication(AuthenticationType.ADMIN)
class Dictionary @Autowired constructor(val namespaceService: NamespaceService) {
    @Uri(value = "namespaces", defaultFormat = "json")
    fun namespaces() = json {
        obj {
            for (namespace in namespaceService.getURIs().filterNot({it.isEmpty()})) {
                key(namespace) {
                    iterable(namespaceService.getPrefixes(namespace)) { prefix ->
                        value(prefix)
                    }
                }
            }
        }
    }
}

package eu.xenit.care4alf.module

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.namespace.NamespaceService
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.webscripts.annotations.Attribute
import eu.xenit.care4alf.json

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/dictionary", families = array("care4alf"), description = "Dictionary info")
Authentication(AuthenticationType.ADMIN)
class Dictionary [Autowired](val namespaceService: NamespaceService) {
    Uri(value = array("namespaces"), defaultFormat = "json")
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

package eu.xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam
import eu.xenit.care4alf.json
import org.apache.log4j.Logger
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod
import org.json.JSONObject
import org.apache.log4j.Level
import java.util.Collections
import org.apache.log4j.LogManager
import java.util.Enumeration
import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.cmr.attributes.AttributeService
import org.alfresco.service.cmr.attributes.AttributeService.AttributeQueryCallback
import java.util.ArrayList
import java.io.Serializable
import java.util.concurrent.FutureTask
import org.osgi.framework.BundleContext
import javax.annotation.PostConstruct
import org.slf4j.LoggerFactory

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/attributes", families = array("care4alf"), description = "Attribute service")
Authentication(AuthenticationType.ADMIN)
public class Attributes [Autowired](private val attributeService: AttributeService) {
    val logger = LoggerFactory.getLogger(javaClass)

    Uri()
    fun get(RequestParam key: String) = json {
        val results = ArrayList<Attribute>()
        attributeService.getAttributes({ (id, value, keys) ->
            results.add(Attribute(keys.toList(), value))
            true
        }, array(key as Serializable))
        iterable(results) { attribute ->
            obj {
                key("keys") {
                    iterable(attribute.keys) { key ->
                        value(key)
                    }
                }
                entry("value", attribute.value)
            }
        }
    }

    data class Attribute(val keys: List<Serializable>, val value: Serializable)
}
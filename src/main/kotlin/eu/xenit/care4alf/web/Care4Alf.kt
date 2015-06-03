package eu.xenit.care4alf.web

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.osgi.framework.BundleContext
import org.springframework.context.ApplicationContextAware

/**
 * Find module webscripts by family
 *
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf", families = arrayOf("care4alf"))
Authentication(AuthenticationType.ADMIN)
class Care4Alf @Autowired constructor(private val bundleContext: BundleContext) : ApplicationContextAware {
    private var applicationContext: ApplicationContext? = null

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        this.applicationContext = applicationContext
    }

    Uri(value = "/", defaultFormat = "html")
    fun index(): Map<String, Any> {
        val lastModified = bundleContext.getBundle().getHeaders().get("Bnd-LastModified")

        return mapOf(
                "modules" to getModuleWebScripts().toSortedMap().map({ entry ->
                    val description = entry.getValue().javaClass.getAnnotation(javaClass<WebScript>()).description
                    mapOf("id" to entry.getKey().toLowerCase(), "description" to description)
                }),
                "version" to lastModified.toLong()
        )
    }

    fun getModuleWebScripts(): Map<String, Any> {
        return applicationContext?.getBeansWithAnnotation(javaClass<WebScript>())?.filter { entry ->
            entry.getValue().javaClass != javaClass<Care4Alf>() &&
            entry.getValue().javaClass.getAnnotation(javaClass<WebScript>()).families.contains("care4alf")
        } ?: mapOf()
    }
}

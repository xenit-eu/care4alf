package eu.xenit.care4alf.web

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import org.osgi.framework.BundleContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

/**
 * Find module webscripts by family
 *
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf", families = arrayOf("care4alf"))
@Authentication(AuthenticationType.ADMIN)
class Care4Alf @Autowired constructor(private val bundleContext: BundleContext) : ApplicationContextAware {
    private var applicationContext: ApplicationContext? = null

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        this.applicationContext = applicationContext
    }

    @Uri("/", defaultFormat = "html")
    fun index(): Map<String, Any> {
        val lastModified = bundleContext.bundle.headers.get("Bnd-LastModified")
        val version = bundleContext.bundle.version

        return mapOf(
                "modules" to getModuleWebScripts().toSortedMap().map { entry ->
                    val description = entry.value.javaClass.getAnnotation(WebScript::class.java).description
                    mapOf("id" to entry.key.toLowerCase(), "description" to description)
                },
                "versionDate" to lastModified.toLong(),
                "version" to version.toString()
        )
    }

    fun getModuleWebScripts(): Map<String, Any> {
        return applicationContext?.getBeansWithAnnotation(WebScript::class.java)?.filter { entry ->
            entry.value.javaClass != Care4Alf::class.java &&
            entry.value.javaClass.getAnnotation(WebScript::class.java).families.contains("care4alf")
        } ?: mapOf()
    }
}

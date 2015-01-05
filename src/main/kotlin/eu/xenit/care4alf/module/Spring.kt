package eu.xenit.care4alf.module

import org.springframework.context.ApplicationContextAware
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication
import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri
import com.github.dynamicextensionsalfresco.webscripts.annotations.Transaction
import org.springframework.context.support.AbstractRefreshableApplicationContext
import org.springframework.context.ApplicationContext
import eu.xenit.care4alf.json
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable

/**
 * @author Laurent Van der Linden
 */
Component
WebScript(baseUri = "/xenit/care4alf/spring", families = array("care4alf"), description = "Inspect Spring beans")
Authentication(AuthenticationType.ADMIN)
class Spring : ApplicationContextAware {
    private var applicationContext: ApplicationContext? = null

    Uri(value = array("/beannames"), defaultFormat = "json")
    Transaction(readOnly = true)
    fun list(): Resolution {
        return beansToJson(applicationContext?.getParent()?.getParent() as AbstractRefreshableApplicationContext)
    }

    Uri(value = array("/beannames/{child}"), defaultFormat = "json")
    Transaction(readOnly = true)
    fun listForChild(UriVariable child: String): Resolution {
        val context = (applicationContext!!.getParent().getParent().getBean(child)
                as org.alfresco.repo.management.subsystems.ChildApplicationContextFactory)
                .getApplicationContext() as AbstractRefreshableApplicationContext
        return beansToJson(context)
    }

    fun beansToJson(context: AbstractRefreshableApplicationContext) = json {
        iterable(context.getBeanDefinitionNames().toList()) { name ->
            obj {
                entry("name", name)
                entry("type", context.getBeanFactory().getBeanDefinition(name).getBeanClassName())
            }
        }
    }

    override fun setApplicationContext(applicationContext: ApplicationContext?) {
        this.applicationContext = applicationContext
    }
}
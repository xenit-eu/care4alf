package xenit.care4alf.module

import org.springframework.stereotype.Component
import com.github.dynamicextensionsalfresco.webscripts.annotations._


import xenit.care4alf.spring.ContextAware
import xenit.care4alf.web.{JsonHelper, Json}
import com.typesafe.scalalogging.slf4j.Logging
import org.springframework.context.support.{AbstractRefreshableApplicationContext, AbstractRefreshableConfigApplicationContext}
import org.alfresco.repo.management.subsystems.ChildApplicationContextFactory

/**
 * Update Alfresco's AMP version in case you want to downgrade.
 *
 * @author Laurent Van der Linden
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/spring", families = Array("care4alf"), description = "Inspect Spring beans")
@Authentication(AuthenticationType.ADMIN)
class Spring extends ContextAware with Logging with Json {
    @Uri(value = Array("/beannames"), defaultFormat = "json")
    @Transaction(readOnly = true)
    def list(@Attribute jsonHelper: JsonHelper) {
        val context = applicationContext.getParent.getParent.asInstanceOf[AbstractRefreshableApplicationContext]
        beansToJson(jsonHelper, context)
    }

    @Uri(value = Array("/beannames/{child}"), defaultFormat = "json")
    @Transaction(readOnly = true)
    def listForChild(@Attribute jsonHelper: JsonHelper, @UriVariable child: String) {
        val context = applicationContext.getParent.getParent.getBean(child)
            .asInstanceOf[org.alfresco.repo.management.subsystems.ChildApplicationContextFactory]
            .getApplicationContext.asInstanceOf[AbstractRefreshableApplicationContext]
        beansToJson(jsonHelper, context)
    }

    def beansToJson(jsonHelper: JsonHelper, context: AbstractRefreshableApplicationContext) {
        implicit val json = jsonHelper.json
        json.array()
        val factory = context.getBeanFactory
        val names = context.getBeanDefinitionNames
        for (name <- names) {
            json.`object`()
                .key("name").value(name)
                .key("type").value(factory.getBeanDefinition(name).getBeanClassName)
            .endObject()
        }
        json.endArray()
    }
}

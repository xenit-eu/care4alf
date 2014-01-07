package xenit.care4alf.spring

import org.springframework.context.{ApplicationContextAware, ApplicationContext}

/**
 * @author Laurent Van der Linden
 */
trait ContextAware extends ApplicationContextAware {
    var applicationContext: ApplicationContext = null

    def setApplicationContext(applicationContext: ApplicationContext) {
        this.applicationContext = applicationContext
    }
}

package xenit.care4alf.alfresco

import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork

/**
 * @author Laurent Van der Linden
 */
object Implicits {
    implicit def runAsWork[T](block: => T) = new RunAsWork[T] {
        def doWork(): T = block
    }
}

package xenit.care4alf.alfresco

import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback
import scala.language.implicitConversions

/**
 * @author Laurent Van der Linden
 */
object Implicits {
    implicit def runAsWork[T](block: => T) = new RunAsWork[T] {
        def doWork(): T = block
    }

    implicit def doInTransaction[T](block: => T) = new RetryingTransactionCallback[T] {
        def execute(): T = block
    }
}

package xenit.care4alf

import org.slf4j.LoggerFactory

/**
 * @author Laurent Van der Linden
 */
trait Logger {
    protected lazy val logger = LoggerFactory.getLogger(this.getClass)
}

package xenit.care4alf.alfresco

import org.springframework.beans.factory.annotation.Autowired
import org.alfresco.service.namespace.NamespaceService

/**
 * @author Laurent Van der Linden
 */
trait HasNamespaceService {
    @Autowired
    implicit var namespaceService: NamespaceService = null
}

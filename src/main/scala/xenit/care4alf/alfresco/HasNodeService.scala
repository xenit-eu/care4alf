package xenit.care4alf.alfresco

import org.springframework.beans.factory.annotation.Autowired
import com.github.dynamicextensionsalfresco.annotations.{ServiceType, AlfrescoService}
import org.alfresco.service.cmr.repository.{NodeRef, NodeService}
import org.alfresco.service.namespace.{NamespaceService, QName}
import java.io.Serializable
import java.util
import scala.language.implicitConversions
import org.alfresco.model.ContentModel

trait HasNodeService extends HasNamespaceService {
    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    implicit var nodeService: NodeService = null

    implicit def toRichNode(nodeRef: NodeRef)(implicit nodeService: NodeService) = new RichNodeRef(nodeRef)
}

class RichNodeRef(val nodeRef: NodeRef) extends AnyVal {
    def apply(property: QName)(implicit nodeService: NodeService) = nodeService.getProperty(nodeRef, property)

    def update(property: QName, newValue: Serializable)(implicit nodeService: NodeService) {
        nodeService.setProperty(nodeRef, property, newValue)
    }

    def addAspect(aspect: QName, properties: (QName,Serializable)*)(implicit nodeService: NodeService) {
        val propertiesMap = new util.HashMap[QName,Serializable](properties.length)
        for (property <- properties ) {
            propertiesMap.put(property._1, property._2)
        }
        nodeService.addAspect(nodeRef, aspect, propertiesMap)
    }

    def hasAspect(aspect: QName)(implicit nodeService: NodeService) = nodeService.hasAspect(nodeRef, aspect)

    def parent(implicit nodeService: NodeService) = nodeService.getPrimaryParent(nodeRef).getParentRef

    def nodeType(implicit nodeService: NodeService) = nodeService.getType(nodeRef)

    def nodeType_=(nodeType: QName)(implicit nodeService: NodeService) {
        nodeService.setType(nodeRef, nodeType)
    }

    def path(implicit nodeService: NodeService, namespaceService: NamespaceService) = nodeService.getPath(nodeRef).toPrefixString(namespaceService)

    def name(implicit nodeService: NodeService) = this(ContentModel.PROP_NAME).asInstanceOf[String]

    def name_=(name: String)(implicit nodeService: NodeService) {
        nodeService.setProperty(nodeRef, ContentModel.PROP_NAME, name)
    }
}
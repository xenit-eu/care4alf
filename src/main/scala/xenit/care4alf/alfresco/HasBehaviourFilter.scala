package xenit.care4alf.alfresco

import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.Resource
import org.alfresco.repo.policy.BehaviourFilter
import org.alfresco.service.namespace.QName
import scala.language.implicitConversions

trait HasBehaviourFilter {
    @Autowired
    @Resource(name = "policyBehaviourFilter")
    var behaviourFilter: BehaviourFilter = null

    implicit def toRichBehaviour(behaviourFilter: BehaviourFilter) = new RichBehaviourFilter(behaviourFilter)

}

class RichBehaviourFilter(val behaviourFilter: BehaviourFilter) extends AnyVal {
    def withoutBehaviour[T](policy: QName)(block: => T) = {
        behaviourFilter.disableBehaviour(policy)
        try {
            block
        } finally {
            behaviourFilter.enableBehaviour(policy)
        }
    }
}

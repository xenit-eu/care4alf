package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class IsolatedNodeProblem extends NodeProblem {

    public IsolatedNodeProblem(NodeRef noderef) {
        super(noderef);
    }

    @Override
    public String getMessage() {
        return "Node is isolated, is not the child of any parent-child association";
    }

    @Override
    public String toString() {
        return String.format("Node %s is isolated, it is not the child of any parent-child association", getNoderef());
    }
}

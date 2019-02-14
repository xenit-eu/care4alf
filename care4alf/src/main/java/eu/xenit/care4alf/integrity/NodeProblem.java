package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;

public abstract class NodeProblem implements Problem {
    private NodeRef noderef;

    public NodeProblem(NodeRef noderef) {
        this.noderef = noderef;
    }

    public NodeRef getNoderef() {
        return noderef;
    }

    public void setNoderef(NodeRef noderef) {
        this.noderef = noderef;
    }
}

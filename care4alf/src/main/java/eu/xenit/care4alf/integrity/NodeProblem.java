package eu.xenit.care4alf.integrity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.alfresco.service.cmr.repository.NodeRef;

public abstract class NodeProblem implements Problem {
    private NodeRef noderef;
    private String extraMessage;

    public NodeProblem(NodeRef noderef) {
        this.noderef = noderef;
    }

    @JsonSerialize(using = NoderefValueSerializer.class)
    public NodeRef getNoderef() {
        return noderef;
    }

    public void setNoderef(NodeRef noderef) {
        this.noderef = noderef;
    }

    public void setExtraMessage(String extraMessage) {
        this.extraMessage = extraMessage;
    }

    @JsonInclude(Include.NON_EMPTY)
    public String getExtraMessage() {
        return extraMessage;
    }
}

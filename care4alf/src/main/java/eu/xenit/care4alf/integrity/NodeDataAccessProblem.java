package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;

public class NodeDataAccessProblem extends NodeProblem {
    private String whichData;

    public NodeDataAccessProblem(NodeRef noderef, String whichData) {
        super(noderef);
        this.whichData = (whichData != null && whichData.length() > 0) ? whichData : "properties";
    }

    @Override
    public String getMessage() {
        return String.format("Unable to get %s relating to node, likely a problem with data in db", whichData);
    }

    @Override
    public String toString() {
        return String.format("Unable to get %s relating to node %s, likely a problem with data in db",
                whichData, getNoderef());
    }
}

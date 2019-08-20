package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class UnknownPropertyProblem extends NodePropertyProblem {

    public UnknownPropertyProblem(NodeRef noderef, QName property) {
        super(noderef, property);
    }

    @Override
    public String getMessage() {
        return "Unknown property found on node";
    }

    @Override
    public String toString() {
        return String.format("Unknown property %s found on node %s", property.toPrefixString(), getNoderef());
    }
}

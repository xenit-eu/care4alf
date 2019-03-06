package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class MissingPropertyProblem extends NodePropertyProblem {

    public MissingPropertyProblem(NodeRef noderef, QName property) {
        super(noderef, property);
    }

    @Override
    public String getMessage() {
        return "Mandatory property not present on node";
    }

    @Override
    public String toString() {
        return String.format("Mandatory property %s not present on node %s", property.toPrefixString(), getNoderef());
    }
}

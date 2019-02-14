package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class UnknownPropertyProblem extends NodeProblem {
    private QName property;

    public UnknownPropertyProblem(NodeRef noderef, QName property) {
        super(noderef);
        this.property = property;
    }

    @Override
    public String getMessage() {
        return "Unknown property found on node";
    }

    public QName getProperty() {
        return property;
    }

    @Override
    public String toString() {
        return String.format("Unknown property %s found on node %s", property.toPrefixString(), getNoderef());
    }
}

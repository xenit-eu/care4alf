package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class MissingPropertyProblem extends NodeProblem {
    private QName property;

    public MissingPropertyProblem(NodeRef noderef, QName property) {
        super(noderef);
        this.property = property;
    }

    @Override
    public String getMessage() {
        return "Mandatory property not present on node";
    }

    public QName getProperty() {
        return property;
    }

    @Override
    public String toString() {
        return String.format("Mandatory property %s not present on node %s", property.toPrefixString(), getNoderef());
    }
}

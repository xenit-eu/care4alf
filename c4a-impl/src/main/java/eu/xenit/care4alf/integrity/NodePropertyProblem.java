package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public abstract class NodePropertyProblem extends NodeProblem {
    protected QName property;

    public NodePropertyProblem(NodeRef noderef, QName property) {
        super(noderef);
        this.property = property;
    }

    public String getProperty() {
        return property.toString();
    }
}

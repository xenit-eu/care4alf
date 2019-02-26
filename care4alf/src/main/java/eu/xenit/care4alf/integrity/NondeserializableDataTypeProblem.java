package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class NondeserializableDataTypeProblem extends NodeProblem {
    private QName property;
    String className;

    public NondeserializableDataTypeProblem(NodeRef noderef, QName property, String className) {
        super(noderef);
        this.property = property;
        this.className = className;
    }

    @Override
    public String getMessage() {
        return "Could not get class " + className + " for deserialization of property " + property.toString();
    }

    @Override
    public String toString() {
        return String.format("Value for node %s property %s wants to be deserialized to unknown class %s",
                getNoderef(), property, className);
    }
}

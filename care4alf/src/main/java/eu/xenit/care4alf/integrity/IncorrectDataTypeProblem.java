package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;

public class IncorrectDataTypeProblem extends NodeProblem {
    private QName property;
    private DataTypeDefinition dataType;
    private String className;

    public IncorrectDataTypeProblem(NodeRef noderef, QName property, DataTypeDefinition dataType, String className) {
        super(noderef);
        this.property = property;
        this.dataType = dataType;
        this.className = className;
    }

    @Override
    public String getMessage() {
        return String.format("Value of property %s is of datatype %s but doesn't deserialize to class %s",
                property, dataType.getName().getPrefixString(), className);
    }

    @Override
    public String toString() {
        return String.format("Value of property %s on node %s is of datatype %s but doesn't deserialize to class %s",
                property, getNoderef(), dataType.getName().getPrefixString(), className);
    }
}

package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;

public class FileNotFoundProblem extends FileProblem {
    private NodeRef noderef;
    public FileNotFoundProblem(String path, NodeRef noderef) {
        super(path);
        this.noderef = noderef;
    }

    @Override
    public String getMessage() {
        return String.format("Noderef %s points to this path but it does not exist on disk", noderef);
    }
}

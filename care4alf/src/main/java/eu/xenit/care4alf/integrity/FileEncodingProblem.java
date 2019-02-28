package eu.xenit.care4alf.integrity;

import org.alfresco.service.cmr.repository.NodeRef;

public class FileEncodingProblem extends FileProblem {
    private NodeRef noderef;
    private String encoding;

    public FileEncodingProblem(String path, String encoding, NodeRef noderef) {
        super(path);
        this.noderef = noderef;
        this.encoding = encoding;
    }

    @Override
    public String getMessage() {
        return String.format("%s specifies encoding %s but %s cannot be decoded as this", noderef, encoding, getPath());
    }
}

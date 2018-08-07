package eu.xenit.care4alf;

import org.alfresco.service.cmr.repository.NodeRef;

public class MissingContent {
    private NodeRef nodeRef;
    private String contentUrl;
    private String cause;

    public MissingContent(NodeRef nodeRef, String contentUrl, String cause) {
        this.nodeRef = nodeRef;
        this.contentUrl = contentUrl;
        this.cause = cause;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}

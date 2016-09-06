package eu.xenit.care4alf.module.bulk;

import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.json.JSONObject;

/**
 * Created by willem on 5/12/15.
 */
public abstract class AbstractWorker extends BatchProcessor.BatchProcessWorkerAdaptor<NodeRef> {

    protected NodeService nodeService;
    protected NamespacePrefixResolver nameSpacePrefixResolver;
    protected JSONObject parameters;
    protected NamespaceService namespaceService;
    protected PermissionService permissionService;


    public AbstractWorker(JSONObject parameters) {
        this.parameters = parameters;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Override
    public void beforeProcess() throws Throwable {
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();
    }

    @Override
    public void afterProcess() throws Throwable {

    }

    public void setNameSpacePrefixResolver(NamespacePrefixResolver nameSpacePrefixResolver) {
        this.nameSpacePrefixResolver = nameSpacePrefixResolver;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }
}

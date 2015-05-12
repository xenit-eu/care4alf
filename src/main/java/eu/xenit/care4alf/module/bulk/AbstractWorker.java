package eu.xenit.care4alf.module.bulk;

import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.json.JSONObject;

/**
 * Created by willem on 5/12/15.
 */
public abstract class AbstractWorker extends BatchProcessor.BatchProcessWorkerAdaptor<NodeRef> {

    protected NodeService nodeService;
    protected JSONObject parameters;

    public AbstractWorker(JSONObject parameters)
    {
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
}

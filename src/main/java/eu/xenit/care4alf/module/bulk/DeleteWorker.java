package eu.xenit.care4alf.module.bulk;

import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorkerAdaptor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


/**
 * Created by willem on 3/12/15.
 */
@Component
public class DeleteWorker extends BatchProcessWorkerAdaptor<NodeRef> {
//    private static Log logger = LogFactory.getLog(DeleteWorker.class);

    private NodeService nodeService;

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

    public void process(final NodeRef nodeRef) throws Throwable {
//      logger.debug("About to delete " + nodeRef);
        this.nodeService.deleteNode(nodeRef);
    }

}
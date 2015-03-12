package eu.xenit.care4alf.module.bulk;

import org.alfresco.repo.batch.BatchProcessor.BatchProcessWorkerAdaptor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.springframework.stereotype.Component;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;


/**
 * Created by willem on 3/12/15.
 */
@Component
public class ArchiveWorker extends BatchProcessWorkerAdaptor<NodeRef> {
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
package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;



/**
 * Created by willem on 3/12/15.
 */
public class DeleteWorker extends AbstractWorker {
//    private static Log logger = LogFactory.getLog(DeleteWorker.class);

    public DeleteWorker(JSONObject parameters)
    {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
//      logger.debug("About to delete " + nodeRef);
        this.nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY ,null);
        this.nodeService.deleteNode(nodeRef);
    }

}
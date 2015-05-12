package eu.xenit.care4alf.module.bulk;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;


/**
 * Created by willem on 3/12/15.
 */
public class ArchiveWorker extends AbstractWorker {
//    private static Log logger = LogFactory.getLog(DeleteWorker.class);

    public ArchiveWorker(JSONObject parameters)
    {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
//      logger.debug("About to delete " + nodeRef);
        this.nodeService.deleteNode(nodeRef);
    }

}
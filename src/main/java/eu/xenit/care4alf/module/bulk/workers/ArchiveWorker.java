package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;


/**
 * Created by willem on 3/12/15.
 */
@Component
@Worker(action = "archive")
public class ArchiveWorker extends AbstractWorker {
//    private static Log logger = LogFactory.getLog(DeleteWorker.class);

    public ArchiveWorker(){
        super(null) ;
    }

    public ArchiveWorker(JSONObject parameters)
    {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
//      logger.debug("About to delete " + nodeRef);
        this.nodeService.deleteNode(nodeRef);
    }

}
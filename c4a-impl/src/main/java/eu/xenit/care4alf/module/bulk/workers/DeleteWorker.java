package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * Created by willem on 3/12/15.
 */
@Component
@Worker(action = "delete")
public class DeleteWorker extends AbstractWorker {

    private static Logger logger = LoggerFactory.getLogger(DeleteWorker.class);

    public DeleteWorker() {
        super(null);
    }

    public DeleteWorker(JSONObject parameters) {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
        logger.debug("About to delete {}", nodeRef);
        this.nodeService.addAspect(nodeRef, ContentModel.ASPECT_TEMPORARY, null);
        this.nodeService.deleteNode(nodeRef);
    }

}
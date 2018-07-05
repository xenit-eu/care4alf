package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Bulk;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Worker(action = "unarchive")
public class UnarchiveWorker extends AbstractWorker {
    private final Logger logger = LoggerFactory.getLogger(UnarchiveWorker.class);

    public UnarchiveWorker(){
        super(null) ;
    }

    public UnarchiveWorker(JSONObject parameters)
    {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
        logger.debug("About to restore " + nodeRef);
        this.nodeArchiveService.restoreArchivedNode(nodeRef);
    }

}
package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 5/13/15.
 */
@Component
@Worker( action = "dummy")
public class DummyWorker extends AbstractWorker {
    private final static Logger logger = LoggerFactory.getLogger(DummyWorker.class);

    public DummyWorker(){
        super(null) ;
    }
    public DummyWorker(JSONObject parameters)
    {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
        logger.info("Processing node: "  + nodeRef);
    }

}

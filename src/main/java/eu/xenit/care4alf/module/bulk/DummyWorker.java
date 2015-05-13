package eu.xenit.care4alf.module.bulk;

import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by willem on 5/13/15.
 */
public class DummyWorker extends AbstractWorker{
    private final static Logger logger = LoggerFactory.getLogger(DummyWorker.class);

    public DummyWorker(JSONObject parameters)
    {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
        logger.info("Processing node: "  + nodeRef);
    }

}
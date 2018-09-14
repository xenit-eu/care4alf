package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Worker(action = "Remove Aspect", parameterNames = {"Aspect"})
public class RemoveAspectWorker extends AbstractWorker {

    private static final Logger log = LoggerFactory.getLogger(RemoveAspectWorker.class);

    public RemoveAspectWorker() {
        super(null);
    }

    public RemoveAspectWorker(JSONObject parameters) {
        super(parameters);
    }

    @Override
    public void process(NodeRef entry) throws Throwable {
        String aspectName = this.parameters.getString("Aspect");
        log.info("Preparing to remove Aspect {} on nodeRef {}", aspectName, entry);
        QName aspectQname = QName.createQName(aspectName, this.nameSpacePrefixResolver);
        log.debug("Aspect qname: {}", aspectQname);
        if (this.nodeService.hasAspect(entry, aspectQname)) {
            this.nodeService.removeAspect(entry, aspectQname);
            log.debug("Aspect removed");
        }
    }
}

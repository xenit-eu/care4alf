package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Thomas.Straetmans on 02/09/2016.
 */


@Component
@Worker(action = "update", parameterNames = {"Regex", "Property", "Format"})
public class UpdateWorker extends AbstractWorker {

    private final Logger logger = LoggerFactory.getLogger(UpdateWorker.class);

    public UpdateWorker() {
        super(null);
    }

    public UpdateWorker(JSONObject parameters) {
        super(parameters);
    }

    @Override
    public void process(NodeRef entry) throws Throwable {
        String regex = parameters.getString("Regex");
        String property = parameters.getString("Property");
        String format = parameters.getString("Format");
//        String path = this.nodeService.getPath(entry).toPrefixString(namespaceService);
        String path = this.nodeService.getPath(entry).toDisplayPath(this.nodeService, this.permissionService) + "/";
        logger.debug("Processing the following: Regex={}, Property={}, Format={}, Path={}", regex, property, format, path);
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(path);
        while (m.find()) {
            logger.debug("inside loop");
            if (m.group(1) != null) {
                String value = m.group(1);
                String propString = String.format(format, value);
                nodeService.setProperty(entry, QName.createQName(property, this.nameSpacePrefixResolver), propString);
            }
        }
    }
}

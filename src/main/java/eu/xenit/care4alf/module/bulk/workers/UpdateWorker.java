package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Bulk;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Thomas.Straetmans on 02/09/2016.
 */


@Component
@Worker(action = "update", parameterNames = {"Regex", "Property", "Format"})
public class UpdateWorker extends AbstractWorker {

    @Autowired
    ServiceRegistry services;

    private final Logger logger = LoggerFactory.getLogger(Bulk.class);

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
        String path = this.nodeService.getPath(entry).toPrefixString(services.getNamespaceService());
        Pattern pattern = Pattern.compile(regex);
        Matcher m = pattern.matcher(path);
        while (m.find()) {
            if (m.group(1) != null) {
                String value = m.group(1);
                String propString = String.format(Locale.ENGLISH, format, value);
                nodeService.setProperty(entry, QName.createQName(property, this.nameSpacePrefixResolver), propString);
            }
        }
    }
}

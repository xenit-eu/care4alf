package eu.xenit.care4alf.module.bulk;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONObject;

/**
 * Created by willem on 5/11/15.
 */
public class SetPropertyWorker extends AbstractWorker {

    public SetPropertyWorker(JSONObject parameters)
    {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
        String property = this.parameters.getString("Property");
        String value = this.parameters.getString("Value");
        this.nodeService.setProperty(nodeRef, QName.createQName(property), value);
    }

}
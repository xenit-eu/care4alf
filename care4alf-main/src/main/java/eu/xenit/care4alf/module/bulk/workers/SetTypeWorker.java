package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 4/27/15.
 */
@Component
@Worker(action = "settype",parameterNames = {"Type"})
public class SetTypeWorker extends AbstractWorker {

    public SetTypeWorker(){
        super(null) ;
    }

    public SetTypeWorker(JSONObject parameters)
    {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
        String type = this.parameters.getString("Type");
        this.nodeService.setType(nodeRef, QName.createQName(type));
    }

}
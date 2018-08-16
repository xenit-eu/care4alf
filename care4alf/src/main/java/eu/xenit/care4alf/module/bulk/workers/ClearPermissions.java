package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.OwnableService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Created by Younes on 11/03/16.
 *
 * Clears node-specific permissions and/or change node ownership
 */
@Component
@Worker( action = "clear permissions", parameterNames = {"newOwner", "purgeNodeSpeceficPermissions"})
public class ClearPermissions extends AbstractWorker {
    private final static Logger logger = LoggerFactory.getLogger(ClearPermissions.class);

    public ClearPermissions(){
        super(null) ;
    }
    public ClearPermissions(JSONObject parameters)
    {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
        logger.info("Processing node: "  + nodeRef);

        String newOwner = (this.parameters.has("newOwner"))?this.parameters.getString("newOwner"):null;
        Boolean purgeNodeSpeceficPermissions = (this.parameters.has("purgeNodeSpeceficPermissions"))?"true".equals(this.parameters.getString("purgeNodeSpeceficPermissions")):false;

        OwnableService ownableService = this.serviceRegistry.getOwnableService();
        if (newOwner != null ) {
            ownableService.setOwner(nodeRef, newOwner);
        }
        if (purgeNodeSpeceficPermissions){
            permissionService.deletePermissions(nodeRef);
        }
    }

}

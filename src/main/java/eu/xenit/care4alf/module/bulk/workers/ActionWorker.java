package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Thomas S on 02/08/2017.
 */
@Component
@Worker(action = "action", parameterNames = {"Action-name", "Params"})
public class ActionWorker extends AbstractWorker {

    public ActionWorker(){
        super(null);
    }

    public ActionWorker(JSONObject parameters) {
        super(parameters);
    }

    @Override
    public void process(NodeRef entry) throws Throwable {
        ActionService service = serviceRegistry.getActionService();
        Action action = service.createAction(parameters.getString("Action-name"), getparams());
        service.evaluateAction(action, entry);

    }

    private Map<String, Serializable> getparams() throws JSONException {
        Map<String, Serializable> params = new HashMap<>();
        JSONArray array = parameters.getJSONArray("Params");
        for(int i = 0; i<array.length();i++){
            JSONObject obj = array.getJSONObject(i);
            params.put(obj.getString("name"), (Serializable) obj.get("value"));
        }
        return params;
    }
}

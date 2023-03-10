package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.QName;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * FIXME sounds dirty/hacky - Definitely need to revisit &amp; clean
 *
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
        String actionName = parameters.getString("Action-name");
        Map<String, Serializable> params = getparams();
        putValuesInParams(params, actionName);

        ActionService actionService = serviceRegistry.getActionService();
        Action action = actionService.createAction(actionName, params);
        actionService.executeAction(action, entry);

    }

    protected void putValuesInParams(Map<String, Serializable> params, String actionName) {
        // params is what comes from the care4alf webpage, paramDefs is what the Action's function signature expects
        ActionService actionService = serviceRegistry.getActionService();
        List<ParameterDefinition> paramDefs = actionService.getActionDefinition(actionName).getParameterDefinitions();
        for (String key : params.keySet()){
            QName paramType = null;
            for (ParameterDefinition paramDef : paramDefs){
                if (paramDef.getName().equals(key)){
                    paramType = paramDef.getType();
                    break;
                }
            }
            if (paramType == null){
                throw new AlfrescoRuntimeException("Trying to assign a value to a non existing parameter");
            }
            params.put(key, (Serializable) DefaultTypeConverter.INSTANCE.convert(serviceRegistry.getDictionaryService().getDataType(paramType), params.get(key)));
        }
    }

    protected Map<String, Serializable> getparams() throws JSONException {
        Map<String, Serializable> params = new HashMap<>();
        String arraystring = parameters.getString("Params");
        if (arraystring == null || arraystring.isEmpty()){
            arraystring = "[]";
        }
        JSONArray array = new JSONArray(arraystring);
        for(int i = 0; i<array.length();i++){
            JSONObject obj = array.getJSONObject(i);
            String key = obj.keys().next().toString();
            params.put(key, (Serializable) obj.get(key));
        }
        return params;
    }
}

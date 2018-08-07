package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionDefinition;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@WebScript(baseUri = "/xenit/care4alf/actions", families = "care4alf", description = "Execute actions")
@Authentication(AuthenticationType.ADMIN)
public class Actions {

    @Autowired
    ActionService actionService;

    @Uri(value = "/")
    public JsonWriterResolution list(){
        return new JsonWriterResolution() {
            protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                jsonWriter.array();
                for (ActionDefinition action : actionService.getActionDefinitions()){
                    jsonWriter.object();
                    jsonWriter.key("name").value(action.getName());
                    jsonWriter.key("description").value(action.getDescription());
                    jsonWriter.key("title").value(action.getTitle());
                    jsonWriter.key("params");jsonWriter.array();
                    for(ParameterDefinition param : action.getParameterDefinitions()){
                        jsonWriter.object();
                        jsonWriter.key("name").value(param.getName());
                        jsonWriter.key("label").value(param.getDisplayLabel());
                        jsonWriter.key("type").value(param.getType());
                        jsonWriter.key("mandatory").value(param.isMandatory());
                        jsonWriter.key("multivalue").value(param.isMultiValued());
                        jsonWriter.endObject();
                    }
                    jsonWriter.endArray();
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            }
        };
    }

    @Uri(value = "/{name}/run", method = HttpMethod.POST)
    public void run(@UriVariable String name, JSONObject json) throws JSONException {
        Action action = this.actionService.createAction(name);
        NodeRef noderef = json.has("noderef") ? new NodeRef(json.getString("noderef")) : null;
        this.actionService.executeAction(action, noderef, false, false);
    }
}
package eu.xenit.care4alf.webscripts;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import org.alfresco.repo.policy.Policy;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.repo.policy.PolicyDefinition;
import org.json.JSONException;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

/**
 * Created by Thomas S on 04/07/2017.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/behaviour", families = {"care4alf"}, description = "Show and execute scheduled jobs")
@Authentication(AuthenticationType.ADMIN)
public class BehaviourList {

    @Autowired
    private PolicyComponent policyComponent;

    @Uri(value = "list")
    public void getPolicies(WebScriptResponse response) throws IOException, JSONException {
        JSONWriter writer = new JSONWriter(response.getWriter());
        Collection<PolicyDefinition> policies = policyComponent.getRegisteredPolicies();
        writer.array();
        for(PolicyDefinition policydef : policies){
            writer.object();
            writer.key("type");
            writer.value(policydef.getType().toString());
            writer.key("name");
            writer.value(policydef.getName().toString());
            writer.key("Args");
            StringBuilder sb = new StringBuilder();
            for(Policy.Arg arg : policydef.getArguments()) {
                sb.append(arg).append(";");
            }
            writer.value(sb.toString());
            writer.endObject();
        }
        writer.endArray();
    }
}

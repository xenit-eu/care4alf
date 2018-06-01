package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import eu.xenit.care4alf.web.WebscriptDefaults;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Thomas S on 04/07/2017.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/impersonate", families = {"care4alf"}, description = "Impersonate")
@Authentication(AuthenticationType.ADMIN)
public class Impersonate {

    private final static Logger logger = LoggerFactory.getLogger(Impersonate.class);

    @Autowired
    private ServiceRegistry services;
    @Autowired
    private PersonService personService;
    @Autowired
    private AuthenticationService authenticationService;

    @Uri(value = "ticket/{username}", method = HttpMethod.GET)
    public JsonWriterResolution getTicket(@UriVariable final String username) throws JSONException, IOException {
        return new JsonWriterResolution() {
            protected void writeJson(final JSONWriter json) throws JSONException {
                if (username.toLowerCase().equals("system")) {
                    throw new WebScriptException(Status.STATUS_FORBIDDEN, "Cannot impersonate System user");
                }

                final String userId = personService.getUserIdentifier(username);
                if (userId == null) {
                    throw new WebScriptException(Status.STATUS_NOT_FOUND, "User not found: " + username);
                }

                if (!authenticationService.getAuthenticationEnabled(userId)) {
                    throw new WebScriptException(Status.STATUS_FORBIDDEN, "The user is disabled in Alfresco");
                }
                String originalUser = personService.getUserIdentifier(authenticationService.getCurrentUserName());

                final NodeRef personRef = services.getPersonService().getPerson(userId, false);
                logger.info("Resolved username '" + username + "' to noderef '" + personRef + "'");

                AuthenticationUtil.setRunAsUser(userId);
                AuthenticationUtil.setFullyAuthenticatedUser(userId);

                json.object();

                json.key("name");
                json.value(authenticationService.getCurrentUserName());

                json.key("ticket");
                json.value(authenticationService.getCurrentTicket());

                json.key("noderef");
                json.value(personRef.toString());

                json.endObject();

                AuthenticationUtil.setRunAsUser(originalUser);
                AuthenticationUtil.setFullyAuthenticatedUser(originalUser);
            }
        };
    }


    @Uri(value = "exec", method = HttpMethod.POST)
    protected void excecute(WebScriptResponse response, @RequestParam(defaultValue = "admin") String username) throws IOException, JSONException {

        String requestingUser = services.getAuthenticationService().getCurrentUserName();

        if(username.toLowerCase().equals("system")){
            throw new WebScriptException(Status.STATUS_FORBIDDEN, "Cannot impersonate System user");
        }

        String userId = services.getPersonService().getUserIdentifier(username);
        if (userId == null)
            throw new WebScriptException(Status.STATUS_NOT_FOUND, "User not found: "+username);

        if (!this.services.getAuthenticationService().getAuthenticationEnabled(userId))
            throw new WebScriptException(Status.STATUS_FORBIDDEN, "The user is disabled in Alfresco");

        NodeRef personRef = services.getPersonService().getPerson(userId, false);
        logger.info("Resolved username '"+username+"' to noderef '"+personRef+"'");

        Map<String, Object> model = new HashMap<String, Object>();

        model.put("currentuser", this.services.getAuthenticationService().getCurrentUserName());
        model.put("currentticket", this.services.getAuthenticationService().getCurrentTicket());

        AuthenticationUtil.setRunAsUser(userId);
        AuthenticationUtil.setFullyAuthenticatedUser(userId);

        model.put("impersonateduser", this.services.getAuthenticationService().getCurrentUserName());
        model.put("impersonatedticket", this.services.getAuthenticationService().getCurrentTicket());
        model.put("impersonatedpersonnoderef", personRef.toString());

        logger.info("[" + requestingUser + "] Impersonating user '" + username + "'");
        //response.getWriter().append("[" + requestingUser + "] Impersonating user '" + username + "'");
        JSONWriter json = new JSONWriter(response.getWriter());
        json.object();
        for (String k : model.keySet()) {
            json.key(k);
            json.value(model.get(k));
        }
        json.endObject();
    }

    @Uri(value = "stop")
    public void stop(WebScriptResponse response) throws IOException {
        String requestingUser = services.getAuthenticationService().getCurrentUserName();
        String userId = services.getPersonService().getUserIdentifier(requestingUser);

        AuthenticationUtil.setRunAsUser(userId);
        AuthenticationUtil.setFullyAuthenticatedUser(userId);

        response.getWriter().append("Ended impersonation.");
    }
}

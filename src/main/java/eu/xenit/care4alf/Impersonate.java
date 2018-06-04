package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.Serializable;

/**
 * Something to note: The original impersonate webscript has two endpoints, one for starting the impersonation, another
 * for ending it. The implication there was that you could make a call to /start, do some requests which would then be
 * automatically impersonated, and call /end. However I couldn't get this to work in testing, and only managed imperso-
 * nation within a clearly defined RunAs scope, or by using ?alf_ticket=...
 *
 * If you want the original Impersonate.java script that provides these endpoints, look in ovam-custom.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/impersonate", families = {"care4alf"}, description = "Impersonate")
@Authentication(AuthenticationType.ADMIN)
public class Impersonate {

    private final static Logger logger = LoggerFactory.getLogger(Impersonate.class);

    @Autowired
    private ServiceRegistry services;
    @Autowired
    private NodeService nodeService;
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

                json.key("userid");
                json.value(userId);

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

    @Uri(value="/prop/{space}/{store}/{guid}/{user}", method = HttpMethod.GET)
    public JsonWriterResolution tryPropertyAs(@UriVariable final String space, @UriVariable final String store,
                              @UriVariable final String guid, @UriVariable final String user) {
        return new JsonWriterResolution() {
            protected void writeJson(final JSONWriter json) throws JSONException {
                final NodeRef node = new NodeRef(space, store, guid);

                Serializable result = AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Serializable>() {
                    @Override
                    public Serializable doWork() {
                        return nodeService.getProperty(node, ContentModel.PROP_NAME);
                    }
                }, user);

                String name = (String) result;
                json.object();
                json.key("cm:name");
                json.value(name);
                json.endObject();
            }
        };
    }
}

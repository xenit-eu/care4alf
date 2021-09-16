package eu.xenit.care4alf.authorityexplorer;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;

/**
 * Created by raven on 3/2/16.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/authorityexplorer", families = {"care4alf"}, description = "Explore Authorities")
@Authentication(AuthenticationType.ADMIN)
public class AuthorityExplorer {

    private final Logger logger = LoggerFactory.getLogger(AuthorityExplorer.class);

    @Autowired
    private AuthorityService authorityService;

    public void groups(final WebScriptResponse response) throws IOException, JSONException {

        response.setHeader("Content-Type", "application/json");
        final JSONWriter json = new JSONWriter(response.getWriter());

        final Set<String> rootAuthoritiesGroup = authorityService.getAllRootAuthoritiesInZone(AuthorityService.ZONE_APP_DEFAULT, AuthorityType.GROUP);
        json.array();
        for (String rootAuthority : rootAuthoritiesGroup) {
            this.recurseGroups(json, rootAuthority);
        }
        json.endArray();

    }

    private void recurseGroups(JSONWriter json, String parentGroup) throws JSONException {
        json.object();

            json.key("name").value(parentGroup);

            final Set<String> containedAuthoritiesGroups = authorityService.getContainedAuthorities(AuthorityType.GROUP, parentGroup, true);
            json.key("groups");
            json.array();
            for (String containedAuthoritiesGroup : containedAuthoritiesGroups) {
                this.recurseGroups(json, containedAuthoritiesGroup);
            }
            json.endArray();

            final Set<String> containedAuthoritiesUsers = authorityService.getContainedAuthorities(AuthorityType.USER, parentGroup, true);
            json.key("users");
            json.array();
            for (String containedAuthoritiesUser : containedAuthoritiesUsers) {
                json.value(containedAuthoritiesUser);
            }
            json.endArray();

        json.endObject();
    }

    @Uri(value="groups")
    public void groupsWrapper(WebScriptRequest req, WebScriptResponse res) throws IOException, JSONException {
        try {
            this.groups(res);
        } catch (Exception e) {
            res.reset();
            logger.error(e.getMessage(), e);

            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            final JSONWriter json = new JSONWriter(res.getWriter());
            json.object();
            json.key("errorIntro").value("Error while executing webscript:");
            json.key("errorMessage").value(e.getMessage());
            json.key("stacktrace");
//            res.getWriter().append("Error while executing webscript:");
//            res.getWriter().append("\n\n\t");
//            res.getWriter().append();
//            res.getWriter().append("\n\n\t");
            final StackTraceElement[] stackTrace = e.getStackTrace();
            String stackTraceString = "";
            for (StackTraceElement stackTraceElement : stackTrace) {
                stackTraceString += stackTraceElement + "\n";
            }
            json.value(stackTraceString);

            json.endObject();
        }
    }
}
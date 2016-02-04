package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashSet;

/**
 * Created by raven on 3/2/16.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/authorityimporter", families = {"care4alf"}, description = "Explore Authorities")
@Authentication(AuthenticationType.ADMIN)
public class AuthorityImporter {

    private final Logger logger = LoggerFactory.getLogger(AuthorityImporter.class);

    @Autowired
    private AuthorityService authorityService;

    @Uri(value="import", method = HttpMethod.POST)
    private void authorityImport(WebScriptRequest req, WebScriptResponse res) {
        try {
            logger.info(
                 "\n --- INCOMING REQUEST --- "
                +"\n"+String.valueOf(req.getContent().getContent())
                +"\n --- --- ---  --- --- --- "
                +"\n"+String.valueOf(req.getContentType())
            );

            JSONArray json = new JSONArray(req.getContent().getContent());
            for (int i = 0; i < json.length(); i++) {
                JSONObject jsonObj = json.getJSONObject(i);
                final String    name   = (String)    jsonObj.get("name");
                final JSONArray groups = (JSONArray) jsonObj.get("groups");
                final JSONArray users  = (JSONArray) jsonObj.get("users");

                logger.info(
                     "\n\n --- --- ---  --- --- --- "
                    + "\n Name:   "+name
                    + "\n Groups: "+groups
                    + "\n Users:  "+users
                    + "\n"
                );

                final String authorityDisplayName = name.replaceFirst("GROUP_", "");
                final HashSet<String> authorityZones = new HashSet<String>();
                authorityZones.add(AuthorityService.ZONE_APP_DEFAULT);
                try {
                    final String authority = authorityService.createAuthority(AuthorityType.GROUP, authorityDisplayName, authorityDisplayName, authorityZones);
                    logger.info(" >> Created Authority "+authority);
                } catch (Exception e) {
                    logger.error(" >> Authority "+authorityDisplayName+" already exists!");
                }

            }

            final JSONWriter jsonRes = new JSONWriter(res.getWriter());
            jsonRes.object();
            jsonRes.key("response").value(true);
            jsonRes.endObject();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
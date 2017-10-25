package eu.xenit.care4alf.authorityimporter;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.service.cmr.repository.DuplicateChildNodeNameException;
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
import java.util.Set;

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
                String groupAuthority = null;

                Set<String> groupAuthorities = authorityService.findAuthorities(AuthorityType.GROUP, null, false, authorityDisplayName, null);
                if(groupAuthorities.size() == 0){
                    groupAuthority = authorityService.createAuthority(AuthorityType.GROUP, authorityDisplayName, authorityDisplayName, authorityZones);
                    logger.info(" >> Created Authority "+groupAuthority);
                } else if(groupAuthorities.size() > 1){
                    throw new IllegalArgumentException("More than one authority for group "+authorityDisplayName);
                } else if(groupAuthorities.size() == 1){
                    groupAuthority = groupAuthorities.iterator().next();
                    logger.debug("Found group authority: "+groupAuthority);
                }

                for(int j = 0; j <users.length(); j++){
                    String user = users.getString(j);
                    Set<String> authorities = authorityService.findAuthorities(AuthorityType.USER, null, false, user, null);
                    if(authorities.size() == 0){
                        throw new IllegalArgumentException("No authority for user "+user);
                    } else if(authorities.size() > 1){
                        throw new IllegalArgumentException("More than one authority for user "+user);
                    }
                    String userAuthority = authorities.iterator().next();
                    logger.debug("Adding user "+ userAuthority+ " to group "+groupAuthority);
                    authorityService.addAuthority(groupAuthority, userAuthority);
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
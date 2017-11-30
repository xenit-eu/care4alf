package eu.xenit.care4alf.authorityimporter;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import eu.xenit.care4alf.AuthorityHelper;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
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
import java.util.List;

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

    @Autowired
    private SearchService searchService;

    @Autowired
    private NodeService nodeService;

    @Uri(value="import", method = HttpMethod.POST)
    private void authorityImport(WebScriptRequest req, WebScriptResponse res) {

        AuthorityHelper authorityHelper = new AuthorityHelper(searchService);

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

//                Set<String> groupAuthorities = authorityService.findAuthorities(AuthorityType.GROUP, null, false, authorityDisplayName, null);
                List<NodeRef> groupAuthorityNodeRefs = authorityHelper.getNodeGroupNodeRefs(authorityDisplayName);

                if(groupAuthorityNodeRefs.size() == 0){
                    groupAuthority = authorityService.createAuthority(AuthorityType.GROUP, authorityDisplayName, authorityDisplayName, authorityZones);
                    logger.info(" >> Created Authority "+groupAuthority);
                } else if(groupAuthorityNodeRefs.size() > 1){
                    throw new IllegalArgumentException("More than one authority for group "+authorityDisplayName);
                } else if(groupAuthorityNodeRefs.size() == 1){
                    groupAuthority = (String) nodeService.getProperty(groupAuthorityNodeRefs.get(0), ContentModel.PROP_AUTHORITY_NAME);
                    logger.debug("Found group authority: "+groupAuthority);
                }


                for(int j = 0; j <users.length(); j++){
                    String user = users.getString(j);
                    List<NodeRef> userAuthorityNodeRefs = authorityHelper.getUserNodeRefs(user);
                    if(userAuthorityNodeRefs.size() == 0){
                        throw new IllegalArgumentException("No authority for user "+user);
                    } else if(userAuthorityNodeRefs.size() > 1){
                        throw new IllegalArgumentException("More than one authority for user "+user);
                    }
                    String userAuthority = user;
                    logger.debug("Adding user "+ userAuthority+ " to group "+groupAuthority);
                    if(!authorityService.getContainedAuthorities(AuthorityType.USER, groupAuthority, true).contains(user)){
                        authorityService.addAuthority(groupAuthority, userAuthority);
                    } else {
                        logger.debug("The user "+user+" is already in the group "+groupAuthority);
                    }
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
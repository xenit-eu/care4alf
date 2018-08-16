package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Worker(action = "delete person")
public class DeletePersonWorker extends AbstractWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeletePersonWorker.class);

    public DeletePersonWorker() {
        super(null);
    }

    public DeletePersonWorker (JSONObject parameters)
    {
        super(parameters);
    }

    @Override
    public void process(final NodeRef entry) throws Throwable {
        AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Boolean>() {
            @Override
            public Boolean doWork() throws Exception {
                AuthorityService authorityService = serviceRegistry.getAuthorityService();
                PersonService personService = serviceRegistry.getPersonService();

                if (!nodeService.getType(entry).equals(ContentModel.TYPE_PERSON)){
                    return false;
                }

                String username = (String) nodeService.getProperty(entry, ContentModel.PROP_USERNAME);
                if (username == null || username.isEmpty()){
                    return false;
                }

                Set<String> authorities = authorityService.getAuthoritiesForUser(username);
                LOGGER.debug("checking authorities for user: " + username);
                for(String authority : authorities){
                    if (authority.equals(PermissionService.ALL_AUTHORITIES)){
                        continue;
                    }

                    if (authorityService.isAdminAuthority(authority)){
                        LOGGER.debug(String.format("-- user %s has admin authority, will not delete.", username));
                        return false;
                    }

                    if (authorityService.isGuestAuthority(authority)){
                        LOGGER.debug(String.format("-- user %s has guest authority, will not delete.", username));
                        return false;
                    }
                }

                personService.deletePerson(entry);
                LOGGER.debug("deleted user " + username);

                return true;
            }
        });


    }
}

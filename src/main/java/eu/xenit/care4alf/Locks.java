package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.repo.jscript.Actions;
import org.alfresco.repo.jscript.Search;
import org.alfresco.repo.lock.JobLockService;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.lock.LockStatus;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.json.JSONException;
import org.json.JSONWriter;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;
//import eu.xenit.care4alf.json;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.model.ContentModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.alfresco.service.cmr.lock.LockService;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.namespace.QName;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

import org.alfresco.repo.lock.mem.LockState;

/**
 * Created by mhgam on 20/10/2015.
 */
@WebScript(baseUri = "/xenit/care4alf/locks", families = "care4alf", description = "manage locks")
@Authentication(AuthenticationType.ADMIN)
@Component
public class Locks {

    @Autowired
    private SearchService searchService;
    @Autowired
    private LockService lockService;
    @Autowired
    private CheckOutCheckInService ccService;

    /**
     * @author Laurent Van der Linden
     */
    public Locks() {

    }

    @Uri("/list")
    public void getAllLocks(final WebScriptResponse response) throws IOException, JSONException {

        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);

        sp.setQuery("ASPECT:\"cm:lockable\"");
//        sp.addSort(SearchParameters.SortDefinition.SortType.SCORE); //is default already
//        sp.addSort("score", false);
        sp.addStore(new StoreRef("workspace", "SpacesStore"));
        ResultSet result = searchService.query(sp);


        final JSONWriter json = new JSONWriter(response.getWriter());
        json.array();

        for (ResultSetRow res : result) {
            json.object();
            json.key("QName");
            json.value(res.getQName().toString());
            json.key("noderef");
            json.value(res.getNodeRef());
            json.endObject();
        }
        json.endArray();


        //Class<LockState> cls = LockState.class;
        //LockStatus status = lockService.getLockStatus(noderef);

        //return "hi";
        /*String result = AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<String>() {
            @Override
            public String doWork() throws Exception {

                //LockState lockState = lockService.getLockState(noderef);

                //return cls.getProtectionDomain().getCodeSource().getLocation().toString();
                //return lockState.getExpires().toString();

            }
        }); *//*{
            //LockState s = null
            val lockState = lockService.getLockState(noderef)

                obj {
                    entry("owner", lockState.getOwner())
                    entry("locktype", lockState.getLockType())
                    entry("lifetime", lockState.getLifetime())
                    entry("expires", lockState.getExpires())
                    entry("additionalinfo", lockState.getAdditionalInfo())
                }
            obj { entry("hello",lockState.getExpires())}

        }*/


    }

    @Uri("/unlock")
    public void unlock(@RequestParam final NodeRef noderef, final WebScriptResponse response) throws IOException, JSONException {

        response.getWriter().write("Unlocking " + noderef);
        AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Object>() {
            @Override
            public Object doWork() throws Exception {
                try {
                    ccService.cancelCheckout(noderef);
                    response.getWriter().write("Unlocked " + noderef);

                } catch (Exception ex) {
                    response.getWriter().write(ex.getLocalizedMessage());
                    response.getWriter().write("Unlocked " + noderef);

                    try {
                        lockService.unlock(noderef, true, true);

                    } catch (Exception ex2)
                    {
                        response.getWriter().write(ex2.getLocalizedMessage());

                    }
                }
                return null;
            }

        });

    }
}

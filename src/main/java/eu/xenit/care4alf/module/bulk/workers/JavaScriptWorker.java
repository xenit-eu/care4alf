package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import eu.xenit.care4alf.script.C4AStringScriptLocation;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Younes on 10/31/16.
 *
 * Executes a javascript snippet on each and every node
 */
@Component
@Worker( action = "javaScript", parameterNames = {"script", "runAs"})
public class JavaScriptWorker extends AbstractWorker {
    private final static Logger logger = LoggerFactory.getLogger(JavaScriptWorker.class);
    public static final StoreRef STORE_REF = new StoreRef("workspace", "SpacesStore");
    public static final String APP_COMPANY_HOME_PATH = "/app:company_home";


    public JavaScriptWorker(){
        super(null) ;
    }
    public JavaScriptWorker(JSONObject parameters)
    {
        super(parameters);
    }

    public void process(final NodeRef nodeRef) throws Throwable {
        logger.info("Processing node: "  + nodeRef);

        // Bulk action parameters actually only support textinput controls; the javascript snippet needs to be entered
        // in an escaped format using some tool like : http://www.freeformatter.com/javascript-escape.html
        // FIXME add support for textarea as a parameter formcontrol and remove the unnecessary escaping
        final String script = (this.parameters.has("script"))?
                StringEscapeUtils.unescapeJavaScript(this.parameters.getString("script")):"";

        final String runAs = (this.parameters.has("runAs"))?this.parameters.getString("runAs"):"admin";

        AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Void>() {

            public Void doWork() {
                processScriptExec(nodeRef, script, runAs);
                return null;
            }
        }, runAs);


    }

    private void processScriptExec(NodeRef nodeRef, String script, String runAs){


        try {
            Map<String, Object> scriptModel = new HashMap<String, Object>();

            String userName = this.serviceRegistry.getAuthenticationService().getCurrentUserName();
            NodeRef personRef = this.personService.getPerson(userName);
            NodeRef homeSpaceRef = (NodeRef)nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);

            scriptModel.putAll(this.serviceRegistry.getScriptService().buildDefaultModel(
                    personRef,
                    getCompanyHome(),
                    homeSpaceRef,
                    null,
                    nodeRef,
                    null));

            ScriptLocation scriptLocation = C4AStringScriptLocation.getC4AStringScriptLocationForString(script);
            this.scriptService.executeScript(scriptLocation, scriptModel);
        } catch (Throwable e) {
            if (logger.isDebugEnabled()) {
                StringWriter stack = new StringWriter();
                e.printStackTrace(new PrintWriter(stack));
                logger.debug("Caught exception : " + stack.toString());
            }

            throw new AlfrescoRuntimeException("care4alf.bulk.javaScript.exception", e);
        }


    }


    /**
     * Gets the company home node
     *
     * @return  the company home node ref
     */
    private NodeRef getCompanyHome()
    {
        NodeRef companyHomeRef;

        List<NodeRef> refs = this.serviceRegistry.getSearchService().selectNodes(
                this.serviceRegistry.getNodeService().getRootNode(STORE_REF),
                APP_COMPANY_HOME_PATH,
                null,
                this.serviceRegistry.getNamespaceService(),
                false);
        if (refs.size() != 1)
        {
            throw new IllegalStateException("Invalid company home path: " + APP_COMPANY_HOME_PATH + " - found: "
                    + refs.size());
        }
        companyHomeRef = refs.get(0);

        return companyHomeRef;
    }
}

package eu.xenit.care4alf.module.bulk.workers;

import eu.xenit.care4alf.module.bulk.AbstractWorker;
import eu.xenit.care4alf.module.bulk.Worker;
import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.util.MD5;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Created by Younes on 10/31/16.
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

        final String script = (this.parameters.has("script"))?this.parameters.getString("script"):"";

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

            ScriptLocation scriptLocation = new C4AStringScriptLocation(script);
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
            throw new IllegalStateException("Invalid company home path: " + APP_COMPANY_HOME_PATH + " - found: " + refs.size());
        }
        companyHomeRef = refs.get(0);

        return companyHomeRef;
    }


    private class C4AStringScriptLocation implements ScriptLocation{
        private String script;
        private String path;

        public C4AStringScriptLocation(String script){
            this.script = script;
            path = MD5.Digest(this.script.getBytes(Charset.forName("UTF-8"))) + ".js";
        }
        @Override
        public InputStream getInputStream() {
            return new ByteArrayInputStream(script.getBytes(Charset.forName("UTF-8")));
        }

        @Override
        public Reader getReader() {
            return new StringReader(this.script);
        }

        @Override
        public String getPath() {
            return path;
        }

        @Override
        public boolean isCachable() {
            return true;
        }

        @Override
        public boolean isSecure() {
            return false;
        }
    }
}

package eu.xenit.care4alf.module.bulk;

import eu.xenit.care4alf.BetterBatchProcessor;
import eu.xenit.care4alf.search.SolrAdmin;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.json.JSONObject;

/**
 * Created by willem on 5/12/15.
 */
public abstract class AbstractWorker extends BetterBatchProcessor.BatchProcessWorkerAdaptor<NodeRef> {

    protected NodeService nodeService;
    protected NamespacePrefixResolver nameSpacePrefixResolver;
    protected JSONObject parameters;
    protected NamespaceService namespaceService;
    protected PermissionService permissionService;
    protected ScriptService scriptService;
    protected ServiceRegistry serviceRegistry;
    protected PersonService personService;
    protected SolrAdmin solrAdmin;
    protected MimetypeService mimetypeService;


    public AbstractWorker(JSONObject parameters) {
        this.parameters = parameters;
    }

    public void setNodeService(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @Override
    public void beforeProcess() throws Throwable {
        AuthenticationUtil.setAdminUserAsFullyAuthenticatedUser();
    }

    @Override
    public void afterProcess() throws Throwable {

    }

    public void setNameSpacePrefixResolver(NamespacePrefixResolver nameSpacePrefixResolver) {
        this.nameSpacePrefixResolver = nameSpacePrefixResolver;
    }

    public void setNamespaceService(NamespaceService namespaceService) {
        this.namespaceService = namespaceService;
    }

    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    public void setScriptService(ScriptService scriptService) {
        this.scriptService = scriptService;
    }

    public void setPersonService(PersonService personService) {
        this.personService = personService;
    }

    public void setServiceRegistery(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void setSolrAdmin(SolrAdmin solrAdmin) {
        this.solrAdmin = solrAdmin;
    }

    public void setMimetypeService(MimetypeService mimetypeService) {
        this.mimetypeService = mimetypeService;
    }
}

package eu.xenit.care4alf.export;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import eu.xenit.care4alf.helpers.NodeHelper;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.impl.AllowPermissionServiceImpl;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.repository.*;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

@Component
@Authentication(AuthenticationType.ADMIN)
@WebScript(baseUri = "/xenit/care4alf/export", families = {"care4alf"}, description = "Export Alfresco")
public class Export {
    public static final String PROPS_PREFIX = "eu.xenit.care4alf.export.";
    private final Logger logger = LoggerFactory.getLogger(Export.class);
    final NodeRef STOP_INDICATOR = new NodeRef("workspace://STOP_INDICATOR/STOP_INDICATOR");
    final int QUEUE_SIZE = 5000;


    @Autowired
    private SearchService searchService;
    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private NodeService nodeService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private PermissionService permissionService;
    @Autowired
    private NodeHelper nodeHelper;
    @Autowired
    private ContentService contentService;
    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private DictionaryService dictionaryService;
    @Autowired
    @Qualifier("global-properties")
    private Properties globalProps;

    private String aclStart = "(";
    private String aclEnd = ")";
    private String aclAuthorityPermissionSeparator = ",";
    private String aclSeparator = ",";
    private Boolean includeAccessStatus;

    @PostConstruct
    private void init(){
        aclStart = globalProps.getProperty(PROPS_PREFIX +"aclStart", aclStart);
        aclEnd = globalProps.getProperty(PROPS_PREFIX +"aclEnd", aclEnd);
        aclAuthorityPermissionSeparator = globalProps.getProperty(PROPS_PREFIX +"aclAuthorityPermissionSeparator",
                aclAuthorityPermissionSeparator);
        aclSeparator = globalProps.getProperty(PROPS_PREFIX +"aclSeparator", aclSeparator);
        includeAccessStatus = Boolean.valueOf(globalProps.getProperty(PROPS_PREFIX +"includeAccessStatus", "false"));
    }

    @Uri(value="/query", method = HttpMethod.GET)
    @Transaction(readOnly = false)
    public void exportQuery(@RequestParam(required = false)                    String query,
                            @RequestParam(defaultValue = ",")            final String separator,
                            @RequestParam(defaultValue = "null")         final String nullValue,
                            @RequestParam(defaultValue = "no_name.csv")  final String documentName,
                            @RequestParam(defaultValue = "cm:name,path") final String columns,
                            @RequestParam(defaultValue = "-1")           final String amountDoc,
                            @RequestParam(required = false)                    String path, // unused?
                            @RequestParam(defaultValue = "false")        final boolean localSave,
                            final WebScriptResponse response) throws IOException, ExecutionException, InterruptedException {
        if(query == null)
            query = "PATH:\"/app:company_home/cm:Projects_x0020_Home//*\" AND TYPE:\"cm:content\"";
        int nbDocuments = Integer.parseInt(amountDoc);
        int totalDocsProcessed = 0;


        final HashMap<String,Boolean> hardcodedNames = new HashMap<String,Boolean>();
        hardcodedNames.put("path",true);
        hardcodedNames.put("text",true);
        hardcodedNames.put("type",true);
        hardcodedNames.put("noderef",true);
        hardcodedNames.put("permissions",true);
        hardcodedNames.put("direct-permissions",true);
        hardcodedNames.put("permissions-inheritance",true);

        SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);

        sp.setQuery(query);
        sp.addStore(new StoreRef("workspace", "SpacesStore"));

        final ArrayBlockingQueue<NodeRef> nodeQueue = new ArrayBlockingQueue<NodeRef>(QUEUE_SIZE);
        ResultSet resultSet = null;
        int start = 0;
        logger.info("Fetching noderefs");
        final long startTime = System.currentTimeMillis();
        final SecurityContext securityContext = SecurityContextHolder.getContext(); // The thread we start needs this


        // All of the stuff above set to final needs to be final so we can access it in the Callable
        // Other languages have closures that capture the scope when the closure is created,
        // so all of the variables are implicitly final. Why can't Java have this? :(
        class OutputHandler implements Callable<Long> {
            @Override
            public Long call() throws Exception {
                // This needs to be here so this thread has the correct permissions to access the services we need
                return AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Long>(){
                    @Override
                    public Long doWork() throws Exception {
                        Writer outputStreamWriter;
                        final String[] column = columns.split(",");

                        if (localSave) {
                            logger.debug("Saving file locally. Creating parent folder CSVExports...");
                            final NodeRef finalParentFolder = nodeHelper.createFolderIfNotExists(nodeHelper.getCompanyHome(), "CSVExports");
                            logger.debug("Folder created. Creating document {}...", documentName);
                            NodeRef ref = nodeHelper.createDocument(finalParentFolder, documentName);
                            logger.debug("Document created. Getting ContentWriter...");
                            ContentWriter contWriter = contentService.getWriter(ref, ContentModel.PROP_CONTENT, true);
                            contWriter.setMimetype(MimetypeMap.MIMETYPE_TEXT_CSV);
                            outputStreamWriter = new OutputStreamWriter(contWriter.getContentOutputStream());
                        } else {
                            response.setContentType("application/CSV");
                            response.setContentEncoding(null);
                            response.addHeader("Content-Disposition", "inline;filename=" + documentName);
                            outputStreamWriter = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
                        }

                        for (int i = 0; i < column.length; i++) {
                            String el = column[i].trim();
                            if (hardcodedNames.containsKey(el)) {
                                outputStreamWriter.write(el);
                            } else {
                                outputStreamWriter.write(dictionaryService.getProperty(QName.createQName(el, namespaceService)).getTitle());
                            }
                            if (i != column.length - 1) {
                                outputStreamWriter.write(separator);
                            }
                        }
                        outputStreamWriter.write("\n");

                        logger.info("Start writing csv");
                        int n = 1;

                        while (true) {
                            NodeRef nRef = nodeQueue.poll(300, TimeUnit.SECONDS);
                            if (nRef == null) {
                                logger.warn("Waiting on nodeQueue timed out after 300s, aborting...");
                                break;
                            }
                            if (nRef == STOP_INDICATOR) {
                                logger.info("STOP_INDICATOR found, consumed {} items", n);
                                break;
                            }
                            StringBuilder result = new StringBuilder();
                            for (int i = 0; i < column.length; i++) {
                                try {
                                    String element = column[i].trim();
                                    if ("path".equals(element)) {
                                        result.append(StringEscapeUtils.escapeCsv(nodeService.getPath(nRef).toDisplayPath(
                                                nodeService, new AllowPermissionServiceImpl())));
                                    } else if ("type".equals(element)) {
                                        QName type = nodeService.getType(nRef);
                                        result.append(dictionaryService.getType(type).getTitle());
                                    } else if ("noderef".equals(element)) {
                                        result.append(nRef);
                                    } else if ("permissions".equals(element)) {
                                        // All permissions
                                        Set<AccessPermission> permissions = permissionService.getAllSetPermissions(nRef);
                                        result.append(StringEscapeUtils.escapeCsv(getFormattedPermissions(permissions)));
                                    } else if ("direct-permissions".equals(element)) {
                                        // Direct permissions on a node (excludes inherited permissions)
                                        Set<AccessPermission> allPermissions = permissionService.getAllSetPermissions(nRef);
                                        result.append(StringEscapeUtils.escapeCsv(getFormattedPermissions(getDirectPermissions(allPermissions))));
                                    } else if ("permissions-inheritance".equals(element)) {
                                        // Flag to determine if permission inheritance is enabled
                                        result.append(StringEscapeUtils.escapeCsv(((Boolean) permissionService.getInheritParentPermissions(nRef)).toString()));
                                    } else {
                                        Serializable property = nodeService.getProperty(nRef, QName.createQName(element, namespaceService));
                                        result.append(StringEscapeUtils.escapeCsv((property==null)?nullValue:property.toString()));
                                    }
                                } catch (RuntimeException e) {
                                    logger.error("Runtime Exception: ", e);
                                }
                                if (i < column.length - 1)
                                    result.append(separator);
                            }
                            String str = result.toString();
                            outputStreamWriter.write(str);
                            outputStreamWriter.write("\n");
                            n++;
                            if (n % 1000 == 0) {
                                logger.info("#noderefs written to csv: " + n);
                                outputStreamWriter.flush();
                            }
                        }
                        outputStreamWriter.flush();
                        outputStreamWriter.close();
                        long endTime = System.currentTimeMillis();
                        long duration = endTime - startTime;

                        // This is here because a Callable must return something.
                        // We use Callable instead of Runnable because we need to be able to throw IOExceptions along.
                        return duration;
                    }
                });
            }
        };

        OutputHandler outputHandler = new OutputHandler();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Long> durationFuture = executor.submit(outputHandler);

        try {
            do {
                sp.setSkipCount(start);
                if (start+1000 > nbDocuments){
                    // Do not get more results than requested.
                    sp.setMaxItems(nbDocuments%1000);
                }
                resultSet = this.searchService.query(sp);
                List<NodeRef> chunk = resultSet.getNodeRefs();
                totalDocsProcessed += chunk.size();
                for(NodeRef n : chunk) {
                    nodeQueue.put(n);
                }
                logger.info("Added {}/{} noderefs to the queue", chunk.size(), nodeQueue.size());
                start += 1000;
                resultSet.close();
                logger.info("#noderefs in query chunk: " + chunk.size());
            }while(resultSet.getNodeRefs().size() > 0 && (totalDocsProcessed < nbDocuments || nbDocuments == -1));//TODO: nodeRefs.size <= nbDocuments
        } finally {
            logger.debug("Placing STOP_INDICATOR, might block");
            nodeQueue.put(STOP_INDICATOR);
            logger.debug("Placed STOP_INDICATOR");
            if (resultSet != null) resultSet.close();
        }


        long duration = durationFuture.get();
        logger.info("Duration in seconds: " + duration / 1000d);
        logger.info((totalDocsProcessed / (duration / 1000d)) + " docs/s");
    }

    private String getFormattedPermissions(Set<AccessPermission> permissions) {
        if (permissions == null){
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Iterator<AccessPermission> permissionIterator = permissions.iterator();
        if (permissionIterator.hasNext()){
            AccessPermission accessPermission = permissionIterator.next();
            sb.append(includeAccessStatus?accessPermission.getAccessStatus():"")
                    .append(aclStart)
                    .append(accessPermission.getAuthority())
                    .append(aclAuthorityPermissionSeparator)
                    .append(accessPermission.getPermission())
                    .append(aclEnd);
        }
        while (permissionIterator.hasNext()){
            AccessPermission accessPermission=permissionIterator.next();
            sb.append(aclSeparator)
                    .append(includeAccessStatus?accessPermission.getAccessStatus():"")
                    .append(aclStart)
                    .append(accessPermission.getAuthority())
                    .append(aclAuthorityPermissionSeparator)
                    .append(accessPermission.getPermission())
                    .append(aclEnd);

        }
        return sb.toString();
    }

    @NotNull
    private Set<AccessPermission> getDirectPermissions(Set<AccessPermission> permissions) {
        if (permissions == null){
            return null;
        }
        Set<AccessPermission> directPermissions = new HashSet<>();
        for (AccessPermission accessPermission: permissions){
            if (accessPermission.isSetDirectly()){
                directPermissions.add(accessPermission);
            }
        }
        return directPermissions;
    }

}
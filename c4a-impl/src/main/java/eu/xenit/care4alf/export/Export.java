package eu.xenit.care4alf.export;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Transaction;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import eu.xenit.care4alf.helpers.NodeHelper;
import eu.xenit.care4alf.helpers.contentdata.ContentDataComponent;
import eu.xenit.care4alf.helpers.contentdata.ContentDataHelper;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.permissions.impl.AllowPermissionServiceImpl;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.dictionary.TypeDefinition;
import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.WrappingWebScriptResponse;
import org.springframework.stereotype.Component;

@Component
@Authentication(AuthenticationType.ADMIN)
@WebScript(baseUri = "/xenit/care4alf/export", families = {"care4alf"}, description = "Export Alfresco")
public class Export {

    public static final String PROPS_PREFIX = "eu.xenit.care4alf.export.";
    private final static Set<String> NON_RESOLVED_AUTHORITIES = new HashSet<String>(
            Arrays.asList("GROUP_EVERYONE", "GROUP_ALFRESCO_ADMINISTRATORS", "guest"));
    private static final char[] CSV_SEARCH_CHARS = new char[]{',', ';', '"', '\r', '\n'};
    final NodeRef STOP_INDICATOR = new NodeRef("workspace://STOP_INDICATOR/STOP_INDICATOR");
    final int QUEUE_SIZE = 50000;
    final int MAX_QUERY_SIZE = 1000;
    final String CONTENTDATACOMPONENT_PREFIX = "content.";
    private final Logger logger = LoggerFactory.getLogger(Export.class);
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
    private AuthorityService authorityService;
    @Autowired
    private PersonService personService;
    @Autowired
    private NodeHelper nodeHelper;
    @Autowired
    private ContentService contentService;
    @Autowired
    private ContentDataHelper contentDataHelper;
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
    // Flag to indicate if permissions breakdown column should be CSV escaped
    private Boolean escapePermissionBreakdown;

    @PostConstruct
    private void init() {
        aclStart = globalProps.getProperty(PROPS_PREFIX + "aclStart", aclStart);
        aclEnd = globalProps.getProperty(PROPS_PREFIX + "aclEnd", aclEnd);
        aclAuthorityPermissionSeparator = globalProps.getProperty(PROPS_PREFIX + "aclAuthorityPermissionSeparator",
                aclAuthorityPermissionSeparator);
        aclSeparator = globalProps.getProperty(PROPS_PREFIX + "aclSeparator", aclSeparator);
        includeAccessStatus = Boolean.valueOf(globalProps.getProperty(PROPS_PREFIX + "includeAccessStatus", "false"));
        escapePermissionBreakdown = Boolean
                .valueOf(globalProps.getProperty(PROPS_PREFIX + "escapePermissionBreakdown", "true"));
    }

    @Uri(value = "/query", method = HttpMethod.GET)
    @Transaction(readOnly = false)
    public void exportQuery(@RequestParam(required = false) String query,
            @RequestParam(defaultValue = ",") final String separator,
            @RequestParam(defaultValue = "null") final String nullValue,
            @RequestParam(defaultValue = "no_name.csv") final String documentName,
            @RequestParam(defaultValue = "cm:name,path") final String columns,
            @RequestParam(defaultValue = "-1") final String amountDoc,
            @RequestParam(required = false) String path, // unused?
            @RequestParam(defaultValue = "false") final boolean localSave,
            WebScriptResponse wsResponse) throws IOException {
        /* The WrappingWebScriptResponse messes up with the interaction with client request making it impossible
           to buffer response, we need to access the wrapped WebScriptResponse and write directly to it in order
           to avoid having timeouts on the client side. Note: doing this implicitly returns a '200 OK' response,
           possibly resulting in inconsistent result if some error happens halfway through.
         */
        boolean isWrapped = wsResponse instanceof WrappingWebScriptResponse;
        WebScriptResponse next = isWrapped ? ((WrappingWebScriptResponse) wsResponse).getNext() : null;
        boolean shouldBypassWrappingWSResponse = isWrapped && next != null;
        final WebScriptResponse response = shouldBypassWrappingWSResponse ? next : wsResponse;

        if (query == null) {
            query = "PATH:\"/app:company_home/cm:Projects_x0020_Home//*\" AND TYPE:\"cm:content\"";
        }
        final int nbDocuments = Integer.parseInt(amountDoc);

        final HashMap<String, Boolean> hardcodedNames = new HashMap<String, Boolean>();
        hardcodedNames.put("path", true);
        hardcodedNames.put("text", true);
        hardcodedNames.put("type", true);
        hardcodedNames.put("noderef", true);
        hardcodedNames.put("permissions", true);
        hardcodedNames.put("permissions-breakdown", true);
        hardcodedNames.put("direct-permissions", true);
        hardcodedNames.put("permissions-inheritance", true);

        final SearchParameters sp = new SearchParameters();
        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);

        sp.setQuery(query);
        sp.addStore(new StoreRef("workspace", "SpacesStore"));

        final ArrayBlockingQueue<NodeRef> nodeQueue = new ArrayBlockingQueue<NodeRef>(QUEUE_SIZE);
        logger.info("Fetching noderefs");
        final long startTime = System.currentTimeMillis();

        // All of the stuff above set to final needs to be final so we can access it in the Callable
        // Other languages have closures that capture the scope when the closure is created,
        // so all of the variables are implicitly final. Why can't Java have this? :(

        // OutputHandler is the consumer thread that will read from the nodeQueue and write to CSV
        class OutputHandler implements Callable<Void> {

            @Override
            public Void call() {
                // This needs to be here so this thread has the correct permissions to access the services we need
                return AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Void>() {
                    @Override
                    public Void doWork() throws Exception {
                        Writer outputStreamWriter;
                        final String[] column = columns.split(",");

                        if (localSave) {
                            logger.debug("Saving file locally. Creating parent folder CSVExports...");
                            final NodeRef finalParentFolder = nodeHelper
                                    .createFolderIfNotExists(nodeHelper.getCompanyHome(), "CSVExports");
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
                            if (hardcodedNames.containsKey(el) || el.startsWith(CONTENTDATACOMPONENT_PREFIX)) {
                                    outputStreamWriter.write(el);
                            } else {
                                QName qName = QName.createQName(el, namespaceService);
                                PropertyDefinition propDef = dictionaryService.getProperty(qName);
                                String headerString = propDef.getTitle(dictionaryService);
                                if (headerString == null || headerString.isEmpty()) {
                                    headerString = propDef.getName().toPrefixString();
                                }
                                outputStreamWriter.write(escapeCsv(headerString, separator));
                            }
                            if (i != column.length - 1) {
                                outputStreamWriter.write(separator);
                            }
                        }
                        outputStreamWriter.write("\n");

                        logger.info("Start writing csv");
                        int n = 0;

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
                            Map<String, ContentDataComponent> cDataCached = null;
                            boolean hasContentData = true;
                            for (int i = 0; i < column.length; i++) {
                                try {
                                    String element = column[i].trim();
                                    if ("path".equals(element)) {
                                        result.append(escapeCsv(nodeService.getPath(nRef).toDisplayPath(
                                                nodeService, new AllowPermissionServiceImpl()), separator));
                                    } else if ("type".equals(element)) {
                                        QName type = nodeService.getType(nRef);
                                        TypeDefinition typeDef = dictionaryService.getType(type);
                                        String typeString = typeDef.getTitle();
                                        if (typeString == null || typeString.isEmpty()) {
                                            typeString = typeDef.getName().toPrefixString();
                                        }
                                        result.append(typeString);
                                    } else if ("noderef".equals(element)) {
                                        result.append(nRef);
                                    } else if (element.startsWith(CONTENTDATACOMPONENT_PREFIX)) {
                                        String targetComponent = element.split("\\.", 2)[1];
                                        if (cDataCached == null && hasContentData) {
                                            QName contentQname = ContentModel.PROP_CONTENT;
                                            ContentData cData = (ContentData) nodeService.getProperty(nRef, contentQname);
                                            if (cData != null) {
                                                cDataCached = contentDataHelper.getContentDataComponents(contentQname.getPrefixString(), cData);
                                            } else {
                                                hasContentData = false;
                                            }
                                        }
                                        if (hasContentData) {
                                            ContentDataComponent retrievedComponent = cDataCached.get(targetComponent);
                                            if (retrievedComponent != null) {
                                                result.append(
                                                        escapeCsv(retrievedComponent.getValue().toString(), separator));
                                            } else {
                                                result.append(escapeCsv("Unable to retrieve contentdata component",
                                                        separator));
                                            }
                                        } else {
                                            result.append(escapeCsv("N/A", separator));
                                        }
                                    } else if ("permissions".equals(element)) {
                                        // All permissions
                                        Set<AccessPermission> permissions = permissionService
                                                .getAllSetPermissions(nRef);
                                        result.append(escapeCsv(getFormattedPermissions(permissions), separator));
                                    } else if ("permissions-breakdown".equals(element)) {
                                        // Breakdown of all permissions
                                        Set<AccessPermission> permissions = permissionService
                                                .getAllSetPermissions(nRef);
                                        result.append(renderColumnWithConfiguredEscaping(
                                                getFormattedPermissionsBreakdown(permissions), separator));
                                    } else if ("direct-permissions".equals(element)) {
                                        // Direct permissions on a node (excludes inherited permissions)
                                        Set<AccessPermission> allPermissions = permissionService
                                                .getAllSetPermissions(nRef);
                                        result.append(
                                                escapeCsv(getFormattedPermissions(getDirectPermissions(allPermissions)),
                                                        separator));
                                    } else if ("permissions-inheritance".equals(element)) {
                                        // Flag to determine if permission inheritance is enabled
                                        result.append(escapeCsv(
                                                ((Boolean) permissionService.getInheritParentPermissions(nRef))
                                                        .toString(), separator));
                                    } else {
                                        Serializable property = nodeService
                                                .getProperty(nRef, QName.createQName(element, namespaceService));
                                        result.append(escapeCsv((property == null) ? nullValue : property.toString(),
                                                separator));
                                    }
                                } catch (RuntimeException e) {
                                    logger.error("Runtime Exception: ", e);
                                }
                                if (i < column.length - 1) {
                                    result.append(separator);
                                }
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

                        logger.info("Duration in seconds: " + duration / 1000d);
                        logger.info((n / (duration / 1000d)) + " docs/s");
                        // This is here because a Callable must return something.
                        // We use Callable instead of Runnable because we need to be able to throw IOExceptions along.
                        return null;
                    }
                });
            }
        }

        // QueryRunner is the producer thread that executes the query and places the results in nodeQueue
        class QueryRunner implements Callable<Void> {

            int totalDocsProcessed = 0;
            ResultSet resultSet = null;
            int start = 0;

            @Override
            public Void call() throws Exception {
                // This needs to be here so this thread has the correct permissions to access the services we need
                return AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Void>() {
                    @Override
                    public Void doWork() throws Exception {
                        try {
                            do {
                                //Adding sorting to guarantee order. This is required because paging does not work with
                                //cursors or cache, therefore any request with more results than the max returned items
                                //will result in a new query. This implies that the result of the second query may include
                                //results that were already returned by the first query. By ordering both queries, the
                                //skipcount will guarantee 'new' results for each.
                                //The property node-dbid has been chosen since it is semantically meaningless, but is
                                //present on every node in the system.
                                sp.addSort("sys:node-dbid", true);
                                sp.setSkipCount(start);
                                if (nbDocuments == -1) {
                                    sp.setMaxItems(MAX_QUERY_SIZE);
                                } else if (start + MAX_QUERY_SIZE > nbDocuments) {
                                    // Do not get more results than requested.
                                    sp.setMaxItems(nbDocuments % MAX_QUERY_SIZE);
                                } else {
                                    sp.setMaxItems(MAX_QUERY_SIZE);
                                }
                                sp.addLocale(new Locale("*"));
                                logger.debug("About to search...");
                                resultSet = Export.this.searchService.query(sp);
                                logger.debug("Search completed");
                                List<NodeRef> chunk = resultSet.getNodeRefs();
                                totalDocsProcessed += chunk.size();
                                logger.info("Adding {}/{} noderefs to the queue...", chunk.size(), nodeQueue.size());
                                for (NodeRef n : chunk) {
                                    nodeQueue.put(n);
                                }
                                logger.info("#Added {} noderefs.", chunk.size());
                                start += MAX_QUERY_SIZE;
                                resultSet.close();
                            }
                            while (resultSet.getNodeRefs().size() > 0 && (totalDocsProcessed < nbDocuments
                                    || nbDocuments == -1));//TODO: nodeRefs.size <= nbDocuments
                        } catch (Exception e) {
                            logger.error("Exception in producer thread", e);
                        } finally {
                            logger.debug("Placing STOP_INDICATOR, might block");
                            nodeQueue.put(STOP_INDICATOR);
                            logger.debug("Placed STOP_INDICATOR");
                            if (resultSet != null) {
                                resultSet.close();
                            }
                        }
                        return null;
                    }
                });
            }
        }

        QueryRunner queryRunner = new QueryRunner();
        OutputHandler outputHandler = new OutputHandler();
        ExecutorService producerExecutor = Executors.newSingleThreadExecutor();
        ExecutorService consumerExecutor = Executors.newSingleThreadExecutor();
        Future<Void> future = consumerExecutor.submit(outputHandler);
        producerExecutor.submit(queryRunner);

        if (localSave) {
            response.setContentType("application/json");
            response.setContentEncoding("UTF-8");
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
            String jsonOutput = "{\n    \"success\": true,\n    \"message\": \"The operation has been scheduled.\"\n}";
            outputStreamWriter.write(jsonOutput);
            outputStreamWriter.close();
            response.getOutputStream().close();
        } else {
            // Wait for the operation to actually be done, since the user needs to be able to download it
            try {
                future.get();
            } catch (Exception e) {
                logger.error("Exception occured during export", e);
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
                outputStreamWriter.write("ERROR : Content truncated - Contact your system administrator");
                outputStreamWriter.close();
            }
        }
    }

    private String renderColumnWithConfiguredEscaping(String columnContent, String separator) {
        if (escapePermissionBreakdown) {
            return escapeCsv(columnContent, separator);
        }
        return columnContent;
    }

    private String getFormattedPermissions(Set<AccessPermission> permissions) {
        if (permissions == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        Iterator<AccessPermission> permissionIterator = permissions.iterator();
        if (permissionIterator.hasNext()) {
            AccessPermission accessPermission = permissionIterator.next();
            sb.append(includeAccessStatus ? accessPermission.getAccessStatus() : "")
                    .append(aclStart)
                    .append(accessPermission.getAuthority())
                    .append(aclAuthorityPermissionSeparator)
                    .append(accessPermission.getPermission())
                    .append(aclEnd);
        }
        while (permissionIterator.hasNext()) {
            AccessPermission accessPermission = permissionIterator.next();
            sb.append(aclSeparator)
                    .append(includeAccessStatus ? accessPermission.getAccessStatus() : "")
                    .append(aclStart)
                    .append(accessPermission.getAuthority())
                    .append(aclAuthorityPermissionSeparator)
                    .append(accessPermission.getPermission())
                    .append(aclEnd);

        }
        return sb.toString();
    }

    private String getFormattedPermissionsBreakdown(Set<AccessPermission> permissions) {
        if (permissions == null) {
            return null;
        }
        Set<AccessPermission> accessPermissionsBreakdown = new HashSet<>(permissions.size());
        Iterator<AccessPermission> permissionIterator = permissions.iterator();
        while (permissionIterator.hasNext()) {
            AccessPermission accessPermission = permissionIterator.next();
            String authority = accessPermission.getAuthority();
            boolean isGroup = authority.startsWith("GROUP_");
            if (isGroup && !NON_RESOLVED_AUTHORITIES.contains(authority)) {
                Set<String> users = authorityService.getContainedAuthorities(AuthorityType.USER, authority, false);
                for (String user : users) {
                    accessPermissionsBreakdown.add(new UserAccessPermission(accessPermission, user));
                }
                if (users.isEmpty()) {
                    logger.debug("Ignoring empty Group authority : {}", authority);
                }
            } else {
                accessPermissionsBreakdown.add(accessPermission);
            }
        }
        return getFormattedPermissions(accessPermissionsBreakdown);
    }

    @NotNull
    private Set<AccessPermission> getDirectPermissions(Set<AccessPermission> permissions) {
        if (permissions == null) {
            return null;
        }
        Set<AccessPermission> directPermissions = new HashSet<>();
        for (AccessPermission accessPermission : permissions) {
            if (accessPermission.isSetDirectly()) {
                directPermissions.add(accessPermission);
            }
        }
        return directPermissions;
    }

    /**
     * Based on Apache Commons StringEscapeUtils.escapeCsv(Writer, String). That one, however, doesn't account for
     * user-configurable column separators: if the user selects semicolon as a separator (for e.g. MS Excel
     * compatibility) and a field value has a semicolon in it, that field isn't quoted and is split in half when opened
     * in a spreadsheet program.
     *
     * This version of the function rectifies that by being aware of the user-configured separator as well as
     * considering semicolon a separator by default.
     *
     * @param str data string
     * @param separator column separator
     * @return Returns data string in which separator strings are surrounded with quotes
     */
    public String escapeCsv(String str, String separator) {
        char[] searchChars;
        boolean containsCsvChar = false;

        // Ensure that searchChars contains the CSV_SEARCH_CHARS plus the separator (if it isn't in there already)
        if (!charArrayContains(CSV_SEARCH_CHARS, separator.charAt(0))) {
            searchChars = Arrays.copyOf(CSV_SEARCH_CHARS, CSV_SEARCH_CHARS.length + 1);
            searchChars[CSV_SEARCH_CHARS.length] = separator.charAt(0);
        } else {
            searchChars = CSV_SEARCH_CHARS;
        }

        // Iterate over the given string, see if it needs to be escaped at all (i.e. contains any csv chars)
        for (int i = 0; i < str.length(); i++) {
            if (charArrayContains(searchChars, str.charAt(i))) {
                containsCsvChar = true;
                break;
            }
        }

        // If not, just return the original string
        if (!containsCsvChar) {
            return str;
        }

        // Otherwise, surround it with quotes, double-quoting existing quotes
        StringBuilder sb = new StringBuilder();

        sb.append('"');
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '"') {
                sb.append('"'); // escape double quote
            }
            sb.append(c);
        }
        sb.append('"');
        return sb.toString();
    }

    private boolean charArrayContains(char[] haystack, char needle) {
        for (int i = 0; i < haystack.length; i++) {
            if (haystack[i] == needle) {
                return true;
            }
        }
        return false;
    }

    private class UserAccessPermission implements AccessPermission {

        private AccessPermission accessPermission;
        private String userAuthority;

        UserAccessPermission(AccessPermission accessPermission, String userAuthority) {
            this.accessPermission = accessPermission;
            this.userAuthority = userAuthority;
        }

        /**
         * The permission.
         */
        @Override
        public String getPermission() {
            return accessPermission.getPermission();
        }

        /**
         * Get the Access enumeration value
         */
        @Override
        public AccessStatus getAccessStatus() {
            return accessPermission.getAccessStatus();
        }

        /**
         * Get the user authority to which this permission applies.
         */
        @Override
        public String getAuthority() {
            return userAuthority;
        }

        /**
         * Get the type of authority to which this permission applies.
         */
        @Override
        public AuthorityType getAuthorityType() {
            return AuthorityType.USER;
        }

        /**
         * At what position in the inheritance chain for permissions is this permission set?
         * = 0 -> Set direct on the object.
         * > 0 -> Inherited
         * < 0 -> We don't know and are using this object for reporting (e.g. the actual permissions that apply to a node for the current user)
         */
        @Override
        public int getPosition() {
            return accessPermission.getPosition();
        }

        /**
         * Is this an inherited permission entry?
         */
        @Override
        public boolean isInherited() {
            return accessPermission.isInherited();
        }

        /**
         * Is this permission set on the object?
         */
        @Override
        public boolean isSetDirectly() {
            return accessPermission.isSetDirectly();
        }
    }
}

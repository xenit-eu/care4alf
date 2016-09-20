package eu.xenit.care4alf.module.bulk;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.domain.node.Node;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.Content;
import org.alfresco.util.GUID;
import org.apache.http.HttpStatus;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.servlet.FormData;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by willem on 3/10/15.
 */
@Component
@WebScript(families = {"care4alf"}, description = "execute actions in bulk")
@Transaction(TransactionType.REQUIRED)
@Authentication(AuthenticationType.ADMIN)
public class Bulk implements ApplicationContextAware {
    private final Logger logger = LoggerFactory.getLogger(Bulk.class);

    private ApplicationContext applicationContext;

    @Autowired
    private ServiceRegistry serviceRegistry;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private NamespacePrefixResolver namespacePrefixResolver;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private NamespaceService namespaceService;

    @Autowired
    private PermissionService permissionService;

    private List<BulkJob> processors = new ArrayList<>();

    @Uri(value = "/xenit/care4alf/bulk/action/{action}", method = HttpMethod.POST)
    public void bulk(@UriVariable final String action, JSONObject json, final WebScriptResponse response) throws IOException, JSONException {
        String query = json.getString("query");
        StoreRef storeRef = new StoreRef(json.getString("store"));
        String queryLanguage = "fts-alfresco";
        int batchSize = json.getInt("batchsize");
        int nbThreads = json.getInt("threads");
        JSONObject parameters = json.getJSONObject("parameters");

        logger.info(String.format("Starting bulk action '%s'", action));

        BatchProcessor<NodeRef> processor = createSearchBatchProcessor(batchSize, nbThreads, action, parameters, query, storeRef, queryLanguage);
        if (processor == null) {
            response.getWriter().write(String.format("No '%s' worker found", action));
            return;
        }

        JSONObject result = processorToJson(processor);
        result.put("action", action);

        response.getWriter().write(result.toString());
    }

    private JSONObject processorToJson(BatchProcessor processor) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("start", processor.getStartTime());
        result.put("end", processor.getEndTime());
        result.put("successes", processor.getSuccessfullyProcessedEntries());
        result.put("errors", processor.getTotalErrors());
        return result;
    }

    @Uri(value = "/xenit/care4alf/bulk/listActions")
    public void ListActions(final WebScriptResponse response) throws JSONException, IOException {
        HashSet<String> actionNames = new HashSet<String>();

        final JSONWriter json = new JSONWriter(response.getWriter());

        json.object();
        for (Object ann : applicationContext.getBeansWithAnnotation(Worker.class).values()) {
            Worker annotation = ann.getClass().getAnnotation(Worker.class);
            String newName = annotation.action();
            if (actionNames.contains(newName))
                throw new UnsupportedOperationException("Multiple workers with same action name found!");
            actionNames.add(newName);

            json.key(newName);
            json.array();
            for (String s : annotation.parameterNames())
                json.value(s);

            json.endArray();
        }
        json.endObject();

    }

    private AbstractWorker createWorkerForAction(@UriVariable String action, JSONObject parameters) {
        // Maybe somewhat hackish since the workers are not really used as beans
        for (Object ann : applicationContext.getBeansWithAnnotation(Worker.class).values()) {
            if (!ann.getClass().getAnnotation(Worker.class).action().equals(action))
                continue;

            // Found potential worker type
            Constructor<?> ctr = null;

            try {
                ctr = ann.getClass().getConstructor(JSONObject.class);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
            if (ctr == null) return null; // no valid constructor found

            try {
                AbstractWorker ret = (AbstractWorker) ctr.newInstance(parameters);
                ret.setNodeService(nodeService);
                ret.setNameSpacePrefixResolver(namespacePrefixResolver);
                ret.setNamespaceService(namespaceService);
                ret.setPermissionService(permissionService);
                return ret;

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    @Uri("/xenit/care4alf/bulk/stores")
    public void stores(final WebScriptResponse response) throws IOException, JSONException {
        List<StoreRef> stores = nodeService.getStores();
        response.getWriter().write(new JSONArray(stores).toString());
    }

    @Uri("/xenit/care4alf/bulk/processors")
    public void getProcessors(final WebScriptResponse response) throws IOException, JSONException {
        JSONArray array = new JSONArray();
        for (BulkJob bulkJob : this.processors) {
            array.put(processorToJson(bulkJob.getProcessor()));
        }
        response.getWriter().write(array.toString());
    }

    @Uri(value = "/xenit/care4alf/bulk/processors", method = HttpMethod.DELETE)
    public void cleanProcessors(final WebScriptResponse response) throws IOException, JSONException {
        this.processors.clear();
    }

    @Uri(value = "/xenit/care4alf/bulk/cancel/{index}", method = HttpMethod.DELETE)
    public void cancelJobs(@UriVariable final String index, final WebScriptResponse response) {
        logger.debug("Cancelling job nr {}", index);
        BulkJob bulkJob = this.processors.get(Integer.parseInt(index));
        BatchProcessWorkProvider provider = bulkJob.getWorkProvider();
        if (provider instanceof SearchWorkProvider) {
            ((SearchWorkProvider) provider).cancel();
        }

        response.setStatus(HttpStatus.SC_OK);
    }

    @Uri(value = "/xenit/care4alf/bluk/form/action/{action}", multipartProcessing = true, method = HttpMethod.POST)
    public void bulkForm(@UriVariable final String action, WebScriptRequest request, WebScriptResponse response) throws IOException, JSONException {
        FormData formData = (FormData) request.parseContent();

        if (!formData.hasField("type")) {
            response.setStatus(HttpStatus.SC_BAD_REQUEST);
            response.getWriter().append("'type' is missing from form, unable to execute action");
            return;
        }

        String type = null;
        String workspace = null;
        String query = null;
        Integer batchsize = null;
        Integer threads = null;
        JSONObject parameters = null;
        InputStream content = null;

        for (FormData.FormField formField : formData.getFields()) {
            if (formField.getName().equals("type")) {
                type = formField.getValue();
            } else if (formField.getName().equals("workspace")) {
                workspace = formField.getValue();
            } else if (formField.getName().equals("query")) {
                query = formField.getValue();
            } else if (formField.getName().equals("batchsize")) {
                batchsize = Integer.valueOf(formField.getValue());
            } else if (formField.getName().equals("threads")) {
                threads = Integer.valueOf(formField.getValue());
            } else if (formField.getName().equals("parameters")) {
                parameters = new JSONObject(formField.getValue());
            } else if (formField.getName().equals("file")) {
                content = formField.getInputStream();
            }
        }

        BatchProcessor<NodeRef> processor = null;
        if (type.equals("search")) {
            processor = createSearchBatchProcessor(batchsize, threads, action, parameters, query, new StoreRef(workspace), "fts-alfresco");
        } else if (type.equals("file")) {
            processor = createFileBatchProcessor(batchsize, threads, action, parameters, content);
        } else {
            // boohoo something went wrong
        }

        if (processor == null) {
            response.getWriter().write(String.format("No '%s' worker found", action));
            return;
        }


        JSONObject result = processorToJson(processor);
        result.put("action", action);

        response.getWriter().write(result.toString());


    }

    private BatchProcessor<NodeRef> createSearchBatchProcessor(int batchSize, int nbThreads, String action, JSONObject parameters, String query, StoreRef storeRef, String queryLanguage) throws IOException {
        BatchProcessor.BatchProcessWorkerAdaptor<NodeRef> worker = null;
        worker = createWorkerForAction(action, parameters);
        SearchWorkProvider workProvider = new SearchWorkProvider(searchService, storeRef, queryLanguage, query, batchSize);

        BatchProcessor<NodeRef> processor = new BatchProcessor<NodeRef>(
                "care4alf-bulk-" + GUID.generate(),
                transactionService.getRetryingTransactionHelper(),
                workProvider,
                nbThreads, batchSize, null, null, 100);


        processors.add(new BulkJob(processor, workProvider));
        // blocks until workers have finished
        processor.process(worker, true);
        return processor;
    }

    private BatchProcessor<NodeRef> createFileBatchProcessor(int batchSize, int nbThreads, String action, JSONObject parameters, InputStream content) {
        BatchProcessor.BatchProcessWorkerAdaptor<NodeRef> worker = null;
        worker = createWorkerForAction(action, parameters);
        FileWorkProvider workProvider = new FileWorkProvider(serviceRegistry, content, batchSize);

        BatchProcessor<NodeRef> processor = new BatchProcessor<NodeRef>(
                "care4alf-bulk-" + GUID.generate(),
                transactionService.getRetryingTransactionHelper(),
                workProvider,
                nbThreads, batchSize, null, null, 100);

        processors.add(new BulkJob(processor, workProvider));
        // blocks until workers have finished
        processor.process(worker, true);
        return processor;
    }


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}

package eu.xenit.care4alf.module.bulk;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
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
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

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
    private NodeService nodeService;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TransactionService transactionService;

    private List<BatchProcessor> processors = new ArrayList<BatchProcessor>();

    @Uri(value = "/xenit/care4alf/bulk/action/{action}", method = HttpMethod.POST)
    public void bulk(@UriVariable final String action, JSONObject json, final WebScriptResponse response) throws IOException, JSONException {
        String query = json.getString("query");
        StoreRef storeRef = new StoreRef(json.getString("store"));
        String queryLanguage = "fts-alfresco";
        int batchSize = json.getInt("batchsize");
        int nbThreads = json.getInt("threads");
        JSONObject parameters = json.getJSONObject("parameters");

        logger.info(String.format("Starting bulk action '%s'", action));

        BatchProcessor.BatchProcessWorkerAdaptor<NodeRef> worker = null;
        worker = createWorkerForAction(action, parameters);

        if (worker == null) {
            response.getWriter().write(String.format("No '%s' worker found", action));
            return;
        }

        BatchProcessor<NodeRef> processor = new BatchProcessor<NodeRef>(
                "care4alf-bulk-" + GUID.generate(),
                transactionService.getRetryingTransactionHelper(),
                new SearchWorkProvider(searchService, storeRef, queryLanguage, query, batchSize),
                nbThreads, batchSize, null, null, 100);


        processors.add(processor);
        // blocks until workers have finished
        processor.process(worker, true);

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
        for(BatchProcessor processor : this.processors)
        {
            array.put(processorToJson(processor));
        }
        response.getWriter().write(array.toString());
    }

    @Uri(value = "/xenit/care4alf/bulk/processors", method = HttpMethod.DELETE)
    public void cleanProcessors(final WebScriptResponse response) throws IOException, JSONException {
        this.processors.clear();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
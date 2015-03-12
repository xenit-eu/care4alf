package eu.xenit.care4alf.module.bulk;

import com.github.dynamicextensionsalfresco.actions.annotations.ActionMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by willem on 3/10/15.
 */
@Component
@WebScript(families = {"care4alf"}, description = "execute actions in bulk")
@Authentication(AuthenticationType.ADMIN)
public class Bulk {
//    private static Log logger = LogFactory.getLog(Bulk.class);

    @Autowired
    private NodeService nodeService;

    @Autowired
    private SearchService searchService;

    @Autowired
    private TransactionService transactionService;

    @Uri(value="/xenit/care4alf/bulk/{action}", method = HttpMethod.POST)
    public void bulk(@UriVariable final String action, JSONObject json, final WebScriptResponse response) throws IOException, JSONException {
        String query = json.getString("query");
        StoreRef storeRef = new StoreRef(json.getString("store"));
        String queryLanguage = "fts-alfresco";
        int batchSize = json.getInt("batchsize");
        int nbThreads = json.getInt("threads");

//        logger.info(String.format("Starting bulk action '%s'", action));

        BatchProcessor.BatchProcessWorkerAdaptor<NodeRef> worker = null;
        if(action.equals("delete"))
        {
            DeleteWorker w = new DeleteWorker();
            w.setNodeService(nodeService);
            worker = w;
        }
        else if(action.equals("archive"))
        {
            ArchiveWorker w = new ArchiveWorker();
            w.setNodeService(nodeService);
            worker = w;
        }


        if(worker == null)
        {
            response.getWriter().write(String.format("No '%s' worker found", action));
            return;
        }

        BatchProcessor<NodeRef> processor = new BatchProcessor<NodeRef>(
                "care4alf-bulk-" + GUID.generate(),
                transactionService.getRetryingTransactionHelper(),
                new SearchWorkProvider(searchService, storeRef, queryLanguage, query, batchSize),
                nbThreads, batchSize, null, null, 100);

        // blocks until workers have finished
        processor.process(worker, true);

        JSONObject result = new JSONObject();
        result.put("action", action);
        result.put("start", processor.getStartTime());
        result.put("end", processor.getEndTime());
        result.put("successes", processor.getSuccessfullyProcessedEntries());
        result.put("errors", processor.getTotalErrors());

        response.getWriter().write(result.toString());
    }

    @ActionMethod("Archive")
    public void archive(final NodeRef nodeRef) {
        if (nodeService.exists(nodeRef)) {
            nodeService.deleteNode(nodeRef);
        }
    }

}
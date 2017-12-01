package eu.xenit.care4alf.module.bulk;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

public class MetadataWorkProvider implements BatchProcessWorkProvider<NodeRef> {
    private final static Logger logger = LoggerFactory.getLogger(MetadataWorkProvider.class);

    private ServiceRegistry serviceRegistry;

    private int batchSize;
    private int skipCount;
    private int queryCount;
    private boolean cancel;

    private MetadataCSV metadataCSV;
    private final SearchParameters sp = new SearchParameters();
    private List<NodeRef> inProgressBatch;

    public MetadataWorkProvider(ServiceRegistry serviceRegistry, InputStream content, int batchSize) {
        this.serviceRegistry = serviceRegistry;
        this.batchSize = batchSize;
        this.metadataCSV = new MetadataCSV(content);

        sp.setLanguage(SearchService.LANGUAGE_FTS_ALFRESCO);
        sp.addStore(new StoreRef("workspace", "SpacesStore"));
    }

    @Override
    public int getTotalEstimatedWorkSize() {
        // TODO: Keep running tally of average query size, estimate by counting remaining queries * average size
        return -1;
    }

    @Override
    public Collection<NodeRef> getNextWork() {
        logger.info("Asked for work");
        if(cancel){
            return Collections.emptyList();
        }

        // if we don't have a batch yet or the batch has been fully returned already
        if(inProgressBatch == null || inProgressBatch.size() - skipCount <= 0){
            if (queryCount >= metadataCSV.getPropertyValues().size()) {
                logger.info("Batch is emptied, nothing left to query. Returning empty list.");
                return Collections.emptyList();
            }
            logger.info("Querying new batch");
            queryNewBatch();
            skipCount = 0;
        }
        // if the query came up empty, run the next query.
        while (inProgressBatch.size() == 0) {
           queryNewBatch();
        }

        int from = skipCount;
        int to = Math.min(skipCount + batchSize, inProgressBatch.size());
        skipCount = to;
        logger.debug("Sending [{}→{}] ({} items)", from, (to-1), (to-from));

        return inProgressBatch.subList(from, to);
    }


    private void queryNewBatch() {
        inProgressBatch = new ArrayList<NodeRef>();
        logger.debug("Calling getPropertyValues()");
        if (queryCount >= metadataCSV.getPropertyValues().size()) {
            return;
        }
        ResultSet rs = null;
        try {
            String property = metadataCSV.getPropertyName();
            String value = metadataCSV.getPropertyValues().get(queryCount);

            // It should be fine to query this all at once since we don't expect too many results per csv entry
            // If that doesn't fit your usecase, consider using the Search option instead of the Metadata option.
            String query = String.format("+@%s:%s", property.replace(":", "\\:"), value);
            logger.debug("Query: {}", query);
            sp.setQuery(query);
            rs = serviceRegistry.getSearchService().query(sp);
            inProgressBatch = rs.getNodeRefs();
            logger.info("Query complete, found {} items", rs.getNodeRefs().size());
            queryCount++;
        } finally {
            if (rs != null) rs.close();
        }
    }

    public MetadataCSV getMetadataCSV() {
        return metadataCSV;
    }

    public void cancel(){
        this.cancel = true;
    }
}


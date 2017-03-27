package eu.xenit.care4alf.module.bulk;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SearchWorkProvider implements BatchProcessWorkProvider<NodeRef> {
    private final static Logger logger = LoggerFactory.getLogger(SearchWorkProvider.class);

    private SearchService searchService;
    private SearchParameters sp;
    private int batchSize;
    private int skipCount;
    private boolean cancel;
    private List<NodeRef> nodeRefs;

    public SearchWorkProvider(
            SearchService searchService,
            StoreRef storeRef,
            String queryLanguage,
            String query,
            int batchSize) {
        this.searchService = searchService;
        this.batchSize = batchSize;
        this.skipCount = 0;

        sp = new SearchParameters();
        if (storeRef == null) {
            storeRef = new StoreRef("workspace", "SpacesStore");
        }

        sp.addStore(storeRef);
        sp.setLanguage(queryLanguage);
        sp.setMaxItems(-1);
        sp.addSort("ID", true);
        sp.setQuery(query);
    }

    @Override
    public int getTotalEstimatedWorkSize() {
        if (this.nodeRefs == null)
            return -1;
        return this.nodeRefs.size();
    }

    @Override
    public Collection<NodeRef> getNextWork() {
        logger.debug("getting next work.");

        if (this.nodeRefs == null) {
            fetchAllResults();
        }

        if (this.cancel) {
            return Collections.emptyList();
        }

        if (skipCount >= this.nodeRefs.size())
            return Collections.emptyList();

        int from = skipCount;
        int to = Math.min(skipCount + this.batchSize, this.nodeRefs.size());
        skipCount += batchSize;

        logger.debug("returning nodes from " + from + " to " + to + " from " + this.nodeRefs.size() + " total.");
        return this.nodeRefs.subList(from, to);
    }

    private void fetchAllResults() {
        this.nodeRefs = new ArrayList<NodeRef>();
        ResultSet rs = null;

        int resultSize = 0;
        int start = 0;
        try {
            do {
                sp.setSkipCount(start);
                logger.info("Start searching at " + start);
                logger.debug("-- searchparameters: " + sp.toString());
                rs = this.searchService.query(sp);

                List<NodeRef> results = rs.getNodeRefs();
                resultSize = results.size();
                logger.info("Results: " + resultSize);
                this.nodeRefs.addAll(results);

                start += 1000;
                rs.close();
            } while ((resultSize - 1000) >= 0 && !cancel);
        } finally {
            if (rs != null) rs.close();
        }

        logger.debug("done fetching results, found total of " + this.nodeRefs.size());
    }

    public void cancel() {
        this.cancel = true;
    }

}

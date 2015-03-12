package eu.xenit.care4alf.module.bulk;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class SearchWorkProvider implements BatchProcessWorkProvider<NodeRef>{
//	private static Log logger = LogFactory.getLog(SearchWorkProvider.class);

	private SearchService searchService;
	private SearchParameters sp;
	private int batchSize;
	private int skipCount;
    private int estimate;
    private boolean cancel;

	public SearchWorkProvider(
            SearchService searchService,
            StoreRef storeRef,
            String queryLanguage,
            String query,
			int batchSize) {
        this.estimate = -1;
		this.searchService = searchService;
		this.batchSize = batchSize;
		this.skipCount = 0;

		sp = new SearchParameters();
		if(storeRef==null){
			storeRef = new StoreRef("workspace", "SpacesStore");
		}
		
		sp.addStore(storeRef);
		sp.setLanguage(queryLanguage);
		sp.setMaxItems(batchSize);
		sp.addSort("ID", true);
		sp.setQuery(query);
	}

	@Override
	public int getTotalEstimatedWorkSize() {
        return this.estimate;
	}

	@Override
	public Collection<NodeRef> getNextWork() {
        if(this.cancel){
//            logger.debug("Returning empty list because workerprovider is cancelled");
            return Collections.emptyList();
        }

//		logger.debug("Query: " + sp.getQuery());
		sp.setSkipCount(skipCount);
		skipCount += batchSize;
		ResultSet rs = null;
		try {
			rs = this.searchService.query(sp);
//            estimate = ResultSetUtil.getCount(rs);
			if (rs.length() <= 0) {
//				logger.debug("Geen resultaten. Geef lege lijst terug");
				return new ArrayList<NodeRef>();
			} else {
				return rs.getNodeRefs();
			}
		} catch (Exception e) {
//			logger.error("Search query failed", e);
			return Collections.emptyList();
		} finally {
			if (rs != null) rs.close();
		}
	}

    public void cancel(){
//        logger.debug("Cancelling job");
        this.cancel = true;
    }

}
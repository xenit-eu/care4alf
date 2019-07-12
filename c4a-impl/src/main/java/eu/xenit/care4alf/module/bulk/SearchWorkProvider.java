package eu.xenit.care4alf.module.bulk;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class SearchWorkProvider implements BatchProcessWorkProvider<NodeRef> {
    private final static Logger logger = LoggerFactory.getLogger(SearchWorkProvider.class);
    public static final String SYS_NODE_DBID = "sys:node-dbid";

    private SearchService searchService;
    private NodeService nodeService;
    private SearchParameters sp;
    private String query;
    private int batchSize;
    private int skipCount;
    private long estimatedResults = -1;
    private boolean cancel;
    private List<NodeRef> nodeRefs = new ArrayList<NodeRef>();
    private long lastDBID = -1L;
    private Object lastDBIDLock = new Object();
    private int processedBatches = 0;

    public SearchWorkProvider(
            SearchService searchService,
            NodeService nodeService,
            StoreRef storeRef,
            String queryLanguage,
            String query,
            int batchSize) {
        this.searchService = searchService;
        this.batchSize = batchSize;
        this.query = query;
        this.nodeService = nodeService;

        sp = new SearchParameters();
        if (storeRef == null) {
            storeRef = new StoreRef("workspace", "SpacesStore");
        }

        sp.addStore(storeRef);
        sp.setLanguage(queryLanguage);
        sp.setMaxItems(-1);
        sp.addSort(SYS_NODE_DBID, true);
    }

    @Override
    public int getTotalEstimatedWorkSize() {
        if (this.nodeRefs == null)
            return -1;
        return (int) estimatedResults;
    }

    @Override
    public Collection<NodeRef> getNextWork() {
        boolean shouldWait;
        boolean checkSolrLag;
        synchronized (this.nodeRefs) {
            if (this.nodeRefs.isEmpty() && this.estimatedResults < 0) {
                logger.debug("Looking for Nodes ...");
                // Launch a new thread to look for nodes corresponding to provided query
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        AuthenticationUtil.runAsSystem(
                                new AuthenticationUtil.RunAsWork<Object>() {

                                    public Void doWork() throws Exception {
                                        fetchAllResults();
                                        return null;
                                    }
                                }
                        );
                    }
                }).start();
            }
        }
        try {
            synchronized (this.nodeRefs) {
                shouldWait = this.estimatedResults < 0
                        || (this.nodeRefs.size() < skipCount + batchSize
                        && this.nodeRefs.size() < this.estimatedResults);
                logger.debug("Should wait ? " + shouldWait);
            }
            while (shouldWait) {
                logger.debug("Waiting for Nodes ...");
                synchronized (this.nodeRefs) {
                    this.nodeRefs.wait();
                }
                synchronized (this.nodeRefs) {
                    shouldWait = this.nodeRefs.size() < skipCount + batchSize
                            && this.nodeRefs.size() < this.estimatedResults;
                }
            }

            if (this.cancel) {
                return Collections.emptyList();
            }

            if (skipCount >= this.nodeRefs.size())
                return Collections.emptyList();

            int from = skipCount;
            int to = Math.min(skipCount + this.batchSize, this.nodeRefs.size());
            skipCount += batchSize;
            processedBatches++;

            return this.nodeRefs.subList(from, to);
        }catch (InterruptedException e){
            throw new AlfrescoRuntimeException("Thread interrupted while waiting...");
        }
    }

    private void fetchAllResults() {
        ResultSet rs = null;
        try {
            do {
                logger.info("Start searching from DBID " + lastDBID);
                sp.setQuery(queryFromLastDBID());
                rs = this.searchService.query(sp);
                synchronized (this.nodeRefs) {
                    this.nodeRefs.addAll(rs.getNodeRefs());

                    Long estimatedResultsTemp = processedBatches * batchSize + rs.getNumberFound();

                    this.estimatedResults = estimatedResultsTemp>this.estimatedResults?estimatedResultsTemp:this.estimatedResults;
                    if(this.nodeRefs.size() > 0) {
                        synchronized (this.lastDBIDLock) {
                            this.lastDBID = (long) nodeService.getProperty(
                                    this.nodeRefs.get(this.nodeRefs.size() - 1),
                                    QName.createQName(NamespaceService.SYSTEM_MODEL_1_0_URI, "node-dbid")
                            );
                        }
                    }
                }
                rs.close();
                synchronized (this.nodeRefs) {
                    this.nodeRefs.notifyAll();
                }
            } while (!cancel && (rs.getNumberFound() >= 1000L));
            this.estimatedResults = this.nodeRefs.size();
            logger.info("Total number of Nodes to be processed " + this.nodeRefs.size());
        } finally {
            if (rs != null) rs.close();
        }
    }

    private String queryFromLastDBID(){
        synchronized (this.lastDBIDLock) {
            return new StringBuilder("(")
                    .append(query)
                    .append(") AND sys:node\\-dbid:[")
                    .append(lastDBID+1)
                    .append(" TO ")
                    .append(Long.MAX_VALUE)
                    .append("]")
                    .toString();
        }
    }

    public void cancel() {
        this.cancel = true;
    }

}

package eu.xenit.care4alf.module.bulk;

import eu.xenit.care4alf.BetterBatchProcessor;
import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Created by Thomas.Straetmans on 20/09/2016.
 */
public class BulkJob {

    private BetterBatchProcessor processor;

    private BatchProcessWorkProvider<NodeRef> workProvider;

    public BulkJob(BetterBatchProcessor batchProcessor, BatchProcessWorkProvider<NodeRef> workProvider) {
        setProcessor(batchProcessor);
        setWorkProvider(workProvider);
    }

    public BetterBatchProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(BetterBatchProcessor processor) {
        this.processor = processor;
    }

    public BatchProcessWorkProvider<NodeRef> getWorkProvider() {
        return workProvider;
    }

    public void setWorkProvider(BatchProcessWorkProvider<NodeRef> workProvider) {
        this.workProvider = workProvider;
    }
}

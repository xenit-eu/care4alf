package eu.xenit.care4alf.module.bulk;

import org.alfresco.repo.batch.BatchProcessWorkProvider;
import org.alfresco.repo.batch.BatchProcessor;
import org.alfresco.service.cmr.repository.NodeRef;

/**
 * Created by Thomas.Straetmans on 20/09/2016.
 */
public class BulkJob {

    private BatchProcessor processor;

    private BatchProcessWorkProvider<NodeRef> workProvider;

    public BulkJob(BatchProcessor batchProcessor, BatchProcessWorkProvider<NodeRef> workProvider) {
        setProcessor(batchProcessor);
        setWorkProvider(workProvider);
    }

    public BatchProcessor getProcessor() {
        return processor;
    }

    public void setProcessor(BatchProcessor processor) {
        this.processor = processor;
    }

    public BatchProcessWorkProvider<NodeRef> getWorkProvider() {
        return workProvider;
    }

    public void setWorkProvider(BatchProcessWorkProvider<NodeRef> workProvider) {
        this.workProvider = workProvider;
    }
}

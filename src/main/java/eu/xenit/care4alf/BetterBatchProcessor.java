package eu.xenit.care4alf;

import eu.xenit.care4alf.search.SolrAdmin;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.batch.*;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.node.integrity.IntegrityException;
import org.alfresco.repo.policy.BehaviourFilter;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.util.TraceableThreadFactory;
import org.alfresco.repo.transaction.TransactionListenerAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationEventPublisher;

/**
 * A <code>BetterBatchProcessor</code> manages the running and monitoring of a potentially long-running transactional batch
 * process. It iterates over a collection, and queues jobs that fire a worker on a batch of members. The queued jobs
 * handle progress / error reporting, transaction delineation and retrying. They are processed in parallel by a pool of
 * threads of a configurable size. The job processing is designed to be fault tolerant and will continue in the event of
 * errors. When the batch is complete a summary of the number of errors and the last error stack trace will be logged at
 * ERROR level. Each individual error is logged at WARN level and progress information is logged at INFO level. Through
 * the {@link BatchMonitor} interface, it also supports the real-time monitoring of batch metrics (e.g. over JMX in the
 * Enterprise Edition).
 *
 * @author dward
 */
public class BetterBatchProcessor<T> implements BatchMonitor
{
    public static final int MAX_AWAIT_TIMEOUT = 300;

    private final boolean disableAuditablePolicies;

    private final BehaviourFilter policyBehaviourFilter;

    /** The factory for all new threads */
    private TraceableThreadFactory threadFactory;

    /** The logger to use. */
    private final Log logger;

    /** The retrying transaction helper. */
    private final RetryingTransactionHelper retryingTransactionHelper;

    /** The source of the work being done. */
    private BatchProcessWorkProvider<T> workProvider;

    /** The process name. */
    private final String processName;

    /** The number of entries to process before reporting progress. */
    private final int loggingInterval;

    /** The number of worker threads. */
    private final int workerThreads;

    /** The number of entries we process at a time in a transaction. */
    private final int batchSize;

    /** The current entry id. */
    private String currentEntryId;

    /** The number of batches currently executing. */
    private int executingCount;

    /** What transactions need to be retried?. We do these single-threaded in order to avoid cross-dependency issues */
    private SortedSet<Integer> retryTxns = new TreeSet<Integer>();

    /** The last error. */
    private Throwable lastError;

    /** The last error entry id. */
    private String lastErrorEntryId;

    /** The total number of errors. */
    private int totalErrors;

    /** The number of successfully processed entries. */
    private int successfullyProcessedEntries;

    /** The start time. */
    private Date startTime;

    /** The end time. */
    private Date endTime;

    private SolrAdmin solrAdmin;
    private int nbBatches;
    private long maxLag;
    private boolean batchCancelled;
    private long awaitTimeOut;

    /**
     * Instantiates a new batch processor.
     *
     * @param processName
     *            the process name
     * @param retryingTransactionHelper
     *            the retrying transaction helper
     * @param workProvider
     *            the object providing the work packets
     * @param workerThreads
     *            the number of worker threads
     * @param batchSize
     *            the number of entries we process at a time in a transaction
     * @param applicationEventPublisher
     *            the application event publisher (may be <tt>null</tt>)
     * @param logger
     *            the logger to use (may be <tt>null</tt>)
     * @param loggingInterval
     *            the number of entries to process before reporting progress
     *
     * @since 3.4
     */
    public BetterBatchProcessor(
            String processName,
            RetryingTransactionHelper retryingTransactionHelper,
            BatchProcessWorkProvider<T> workProvider,
            int workerThreads, int batchSize,
            ApplicationEventPublisher applicationEventPublisher,
            Log logger,
            int loggingInterval,
            SolrAdmin solrAdmin,
            long maxLag,
            int nbBatches,
            boolean disableAuditablePolicies,
            BehaviourFilter policyBehaviourFilter)
    {
        this.threadFactory = new TraceableThreadFactory();
        this.threadFactory.setNamePrefix(processName);
        this.threadFactory.setThreadDaemon(true);

        this.processName = processName;
        this.retryingTransactionHelper = retryingTransactionHelper;
        this.workProvider = workProvider;
        this.workerThreads = workerThreads;
        this.batchSize = batchSize;
        this.nbBatches = nbBatches;
        this.maxLag = maxLag;
        this.disableAuditablePolicies = disableAuditablePolicies;
        this.policyBehaviourFilter = policyBehaviourFilter;
        this.awaitTimeOut = (maxLag>MAX_AWAIT_TIMEOUT)?MAX_AWAIT_TIMEOUT:maxLag;
        this.solrAdmin = solrAdmin;
        if (logger == null)
        {
            this.logger = LogFactory.getLog(this.getClass());
        }
        else
        {
            this.logger = logger;
        }
        this.loggingInterval = loggingInterval;

        // Let the (enterprise) monitoring side know of our presence
        if (applicationEventPublisher != null)
        {
            applicationEventPublisher.publishEvent(new BatchMonitorEvent(this));
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getCurrentEntryId()
    {
        return this.currentEntryId;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getLastError()
    {
        if (this.lastError == null)
        {
            return null;
        }
        Writer buff = new StringWriter(1024);
        PrintWriter out = new PrintWriter(buff);
        this.lastError.printStackTrace(out);
        out.close();
        return buff.toString();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getLastErrorEntryId()
    {
        return this.lastErrorEntryId;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getProcessName()
    {
        return this.processName;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int getSuccessfullyProcessedEntries()
    {
        return this.successfullyProcessedEntries;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized String getPercentComplete()
    {
        int totalResults = this.workProvider.getTotalEstimatedWorkSize();
        int processed = this.successfullyProcessedEntries + this.totalErrors;
        return processed <= totalResults ? NumberFormat.getPercentInstance().format(
                totalResults == 0 ? 1.0F : (float) processed / totalResults) : "Unknown";
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int getTotalErrors()
    {
        return this.totalErrors;
    }

    /**
     * {@inheritDoc}
     */
    public int getTotalResults()
    {
        return this.workProvider.getTotalEstimatedWorkSize();
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Date getEndTime()
    {
        return this.endTime;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized Date getStartTime()
    {
        return this.startTime;
    }

    /**
     * Invokes the worker for each entry in the collection, managing transactions and collating success / failure
     * information.
     *
     * @param worker
     *            the worker
     * @param splitTxns
     *            Can the modifications to Alfresco be split across multiple transactions for maximum performance? If
     *            <code>true</code>, worker invocations are isolated in separate transactions in batches for
     *            increased performance. If <code>false</code>, all invocations are performed in the current
     *            transaction. This is required if calling synchronously (e.g. in response to an authentication event in
     *            the same transaction).
     * @return the number of invocations
     */
    @SuppressWarnings("serial")
    public int process(final BatchProcessWorker<T> worker, final boolean splitTxns)
    {
        int count = workProvider.getTotalEstimatedWorkSize();
        synchronized (this)
        {
            this.startTime = new Date();
            if (this.logger.isInfoEnabled())
            {
                if (count >= 0)
                {
                    this.logger.info(getProcessName() + ": Commencing batch of " + count + " entries");
                }
                else
                {
                    this.logger.info(getProcessName() + ": Commencing batch");

                }
            }
        }

        // Create a thread pool executor with the specified number of threads and a finite blocking queue of jobs
        ExecutorService executorService = splitTxns && this.workerThreads > 1 ?
                new ThreadPoolExecutor(
                        this.workerThreads, this.workerThreads, 0L, TimeUnit.MILLISECONDS,
                        new SolrLagAwareArrayBlockingQueue<Runnable>(this.workerThreads * this.batchSize * 10),
                        threadFactory) : null;
        try
        {
            Iterator<T> iterator = new WorkProviderIterator<T>(this.workProvider);
            int id=0;
            List<T> batch = new ArrayList<T>(this.batchSize);
            while (iterator.hasNext())
            {
                batch.add(iterator.next());
                boolean hasNext = iterator.hasNext();
                if (batch.size() >= this.batchSize || !hasNext)
                {
                    final TxnCallback callback = new TxnCallback(id++, worker, batch, splitTxns);
                    if (hasNext)
                    {
                        batch = new ArrayList<T>(this.batchSize);
                    }

                    if (executorService == null)
                    {
                        callback.run();
                    }
                    else
                    {
                        executorService.execute(callback);
                    }
                }
            }
            return count;
        }
        finally
        {
            if (executorService != null)
            {
                executorService.shutdown();
                try
                {
                    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                }
                catch (InterruptedException e)
                {
                }
            }
            synchronized (this)
            {
                reportProgress(true);
                this.endTime = new Date();
                if (this.logger.isInfoEnabled())
                {
                    if (count >= 0)
                    {
                        this.logger.info(getProcessName() + ": Completed batch of " + count + " entries");
                    }
                    else
                    {
                        this.logger.info(getProcessName() + ": Completed batch");

                    }
                }
                if (this.totalErrors > 0 && this.logger.isErrorEnabled())
                {
                    this.logger.error(getProcessName() + ": " + this.totalErrors
                                    + " error(s) detected. Last error from entry \"" + this.lastErrorEntryId + "\"",
                            this.lastError);
                }
            }
        }
    }

    /**
     * Reports the current progress.
     *
     * @param last
     *            Have all jobs been processed? If <code>false</code> then progress is only reported after the number of
     *            entries indicated by {@link #loggingInterval}. If <code>true</code> then progress is reported if this
     *            is not one of the entries indicated by {@link #loggingInterval}.
     */
    private synchronized void reportProgress(boolean last)
    {
        int processed = this.successfullyProcessedEntries + this.totalErrors;
        if (processed % this.loggingInterval == 0 ^ last)
        {
            StringBuilder message = new StringBuilder(100).append(getProcessName()).append(": Processed ").append(
                    processed).append(" entries");
            int totalResults = this.workProvider.getTotalEstimatedWorkSize();
            if (totalResults >= processed)
            {
                message.append(" out of ").append(totalResults).append(". ").append(
                        NumberFormat.getPercentInstance().format(
                                totalResults == 0 ? 1.0F : (float) processed / totalResults)).append(" complete");
            }
            long duration = System.currentTimeMillis() - this.startTime.getTime();
            if (duration > 0)
            {
                message.append(". Rate: ").append(processed * 1000L / duration).append(" per second");
            }
            message.append(". " + this.totalErrors + " failures detected.");
            this.logger.info(message);
        }
    }

    public void cancel() {
        this.batchCancelled = true;
    }

    /**
     * An interface for workers to be invoked by the {@link BetterBatchProcessor}.
     */
    public interface BatchProcessWorker<T>
    {
        /**
         * Gets an identifier for the given entry (for monitoring / logging purposes).
         *
         * @param entry
         *            the entry
         * @return the identifier
         */
        public String getIdentifier(T entry);

        /**
         * Callback to allow thread initialization before the work entries are
         * {@link #process(Object) processed}.  Typically, this will include authenticating
         * as a valid user and disbling or enabling any system flags that might affect the
         * entry processing.
         */
        public void beforeProcess() throws Throwable;

        /**
         * Processes the given entry.
         *
         * @param entry
         *            the entry
         * @throws Throwable
         *             on any error
         */
        public void process(T entry) throws Throwable;

        /**
         * Callback to allow thread cleanup after the work entries have been
         * {@link #process(Object) processed}.
         * Typically, this will involve cleanup of authentication and resetting any
         * system flags previously set.
         * <p>
         * This call is made regardless of the outcome of the entry processing.
         */
        public void afterProcess() throws Throwable;
    }

    /**
     * Adaptor that allows implementations to only implement {@link #process(Object)}
     */
    public static abstract class BatchProcessWorkerAdaptor<TT> implements BatchProcessWorker<TT>
    {
        /**
         * @return  Returns the <code>toString()</code> of the entry
         */
        public String getIdentifier(TT entry)
        {
            return entry.toString();
        }
        /** No-op */
        public void beforeProcess() throws Throwable
        {
        }
        /** No-op */
        public void afterProcess() throws Throwable
        {
        }
    }

    /**
     * Small iterator that repeatedly gets the next batch of work from a {@link BatchProcessWorkProvider}

     * @author Derek Hulley
     */
    private static class WorkProviderIterator<T> implements Iterator<T>
    {
        private BatchProcessWorkProvider<T> workProvider;
        private Iterator<T> currentIterator;

        private WorkProviderIterator(BatchProcessWorkProvider<T> workProvider)
        {
            this.workProvider = workProvider;
        }

        public boolean hasNext()
        {
            boolean hasNext = false;
            if (workProvider == null)
            {
                // The workProvider was exhausted
                hasNext = false;
            }
            else
            {
                if (currentIterator != null)
                {
                    // See if there there is any more on this specific iterator
                    hasNext = currentIterator.hasNext();
                }

                // If we don't have a next (remember that the workProvider is still available)
                // go and get more results
                if (!hasNext)
                {
                    Collection<T> nextWork = workProvider.getNextWork();
                    if (nextWork == null)
                    {
                        throw new RuntimeException("BatchProcessWorkProvider returned 'null' work: " + workProvider);
                    }
                    // Check that there are some results at all
                    if (nextWork.size() == 0)
                    {
                        // An empty collection indicates that there are no more results
                        workProvider = null;
                        currentIterator = null;
                        hasNext = false;
                    }
                    else
                    {
                        // There were some results, so get a new iterator
                        currentIterator = nextWork.iterator();
                        hasNext = currentIterator.hasNext();
                    }
                }
            }
            return hasNext;
        }

        public T next()
        {
            if (!hasNext())
            {
                throw new NoSuchElementException();
            }
            return currentIterator.next();
        }

        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * A callback that invokes a worker on a batch, optionally in a new transaction.
     */
    class TxnCallback extends TransactionListenerAdapter implements RetryingTransactionCallback<Object>, Runnable
    {

        /**
         * Instantiates a new callback.
         *
         * @param worker
         *            the worker
         * @param batch
         *            the batch to process
         * @param splitTxns
         *            If <code>true</code>, the worker invocation is made in a new transaction.
         */
        public TxnCallback(int id, BatchProcessWorker<T> worker, List<T> batch, boolean splitTxns)
        {
            this.id = id;
            this.worker = worker;
            this.batch = batch;
            this.splitTxns = splitTxns;
        }

        private final int id;

        /** The worker. */
        private final BatchProcessWorker<T> worker;

        /** The batch. */
        private final List<T> batch;

        /** If <code>true</code>, the worker invocation is made in a new transaction. */
        private final boolean splitTxns;

        /** The total number of errors. */
        private int txnErrors;

        /** The number of successfully processed entries. */
        private int txnSuccesses;

        /** The current entry being processed in the transaction */
        private String txnEntryId;

        /** The last error. */
        private Throwable txnLastError;

        /** The last error entry id. */
        private String txnLastErrorEntryId;

        public Object execute() throws Throwable
        {
            reset();
            if (this.batch.isEmpty())
            {
                return null;
            }

            // Bind this instance to the transaction
            AlfrescoTransactionSupport.bindListener(this);

            synchronized (BetterBatchProcessor.this)
            {
                if (BetterBatchProcessor.this.logger.isDebugEnabled())
                {
                    BetterBatchProcessor.this.logger.debug("RETRY TXNS: " + BetterBatchProcessor.this.retryTxns);
                }
                // If we are retrying after failure, assume there are cross-dependencies and wait for other
                // executing batches to complete
                while (!BetterBatchProcessor.this.retryTxns.isEmpty()
                        && (BetterBatchProcessor.this.retryTxns.first() < this.id || BetterBatchProcessor.this.retryTxns.first() == this.id
                        && BetterBatchProcessor.this.executingCount > 0)
                        && BetterBatchProcessor.this.retryTxns.last() >= this.id)
                {
                    if (BetterBatchProcessor.this.logger.isDebugEnabled())
                    {
                        BetterBatchProcessor.this.logger.debug(Thread.currentThread().getName()
                                + " Recoverable failure: waiting for other batches to complete");
                    }
                    BetterBatchProcessor.this.wait();
                }
                if (BetterBatchProcessor.this.logger.isDebugEnabled())
                {
                    BetterBatchProcessor.this.logger.debug(Thread.currentThread().getName() + " ready to execute");
                }
                BetterBatchProcessor.this.currentEntryId = this.worker.getIdentifier(this.batch.get(0));
                BetterBatchProcessor.this.executingCount++;
            }

            for (T entry : this.batch)
            {
                this.txnEntryId = this.worker.getIdentifier(entry);
                try
                {
                    if (BetterBatchProcessor.this.disableAuditablePolicies){
                        BetterBatchProcessor.this.policyBehaviourFilter.disableBehaviour(ContentModel.ASPECT_AUDITABLE);
                    }
                    this.worker.process(entry);
                    this.txnSuccesses++;
                }
                catch (Throwable t)
                {
                    if (RetryingTransactionHelper.extractRetryCause(t) == null)
                    {
                        if (BetterBatchProcessor.this.logger.isWarnEnabled())
                        {
                            BetterBatchProcessor.this.logger.warn(getProcessName() + ": Failed to process entry \""
                                    + this.txnEntryId + "\".", t);
                        }
                        this.txnLastError = t;
                        this.txnLastErrorEntryId = this.txnEntryId;
                        this.txnErrors++;
                    }
                    else
                    {
                        // Next time we retry, we will wait for other executing batches to complete
                        throw t;
                    }
                }finally {
                    if (BetterBatchProcessor.this.disableAuditablePolicies){
                        BetterBatchProcessor.this.policyBehaviourFilter.enableBehaviour(ContentModel.ASPECT_AUDITABLE);
                    }
                }
            }
            return null;
        }

        public void run()
        {
            try
            {
            }
            catch (Throwable e)
            {
                BetterBatchProcessor.this.logger.error("Failed to cleanup Worker after processing.", e);
            }


            final BetterBatchProcessor<T>.TxnCallback callback = this;
            try
            {
                Throwable tt = null;
                worker.beforeProcess();
                try
                {
                    BetterBatchProcessor.this.retryingTransactionHelper.doInTransaction(callback, false, splitTxns);
                }
                catch (Throwable t)
                {
                    // Keep this and rethrow
                    tt = t;
                }
                worker.afterProcess();
                // Throw if there was a processing exception
                if (tt != null)
                {
                    throw tt;
                }
            }
            catch (Throwable t)
            {
                // If the callback was in its own transaction, it must have run out of retries
                if (this.splitTxns)
                {
                    this.txnLastError = t;
                    this.txnLastErrorEntryId = (t instanceof IntegrityException) ? "unknown" : this.txnEntryId;
                    this.txnErrors++;
                    if (BetterBatchProcessor.this.logger.isWarnEnabled())
                    {
                        String message = (t instanceof IntegrityException) ? ": Failed on batch commit." : ": Failed to process entry \""
                                + this.txnEntryId + "\".";
                        BetterBatchProcessor.this.logger.warn(getProcessName() + message, t);
                    }
                }
                // Otherwise, we have a retryable exception that we should propagate
                else
                {
                    if (t instanceof RuntimeException)
                    {
                        throw (RuntimeException) t;
                    }
                    if (t instanceof Error)
                    {
                        throw (Error) t;
                    }
                    throw new AlfrescoRuntimeException("Transactional error during " + getProcessName(), t);
                }
            }

            commitProgress();
        }

        /**
         * Resets the callback state for a retry.
         */
        private void reset()
        {
            this.txnLastError = null;
            this.txnLastErrorEntryId = null;
            this.txnSuccesses = this.txnErrors = 0;
        }

        /**
         * Commits progress from this transaction after a successful commit.
         */
        private void commitProgress()
        {
            synchronized (BetterBatchProcessor.this)
            {
                if (this.txnErrors > 0)
                {
                    int processed = BetterBatchProcessor.this.successfullyProcessedEntries + BetterBatchProcessor.this.totalErrors;
                    int currentIncrement = processed % BetterBatchProcessor.this.loggingInterval;
                    int newErrors = BetterBatchProcessor.this.totalErrors + this.txnErrors;
                    // Work out the number of logging intervals we will cross and report them
                    int intervals = (this.txnErrors + currentIncrement) / BetterBatchProcessor.this.loggingInterval;
                    if (intervals > 0)
                    {
                        BetterBatchProcessor.this.totalErrors += BetterBatchProcessor.this.loggingInterval - currentIncrement;
                        reportProgress(false);
                        while (--intervals > 0)
                        {
                            BetterBatchProcessor.this.totalErrors += BetterBatchProcessor.this.loggingInterval;
                            reportProgress(false);
                        }
                    }
                    BetterBatchProcessor.this.totalErrors = newErrors;
                }

                if (this.txnSuccesses > 0)
                {
                    int processed = BetterBatchProcessor.this.successfullyProcessedEntries + BetterBatchProcessor.this.totalErrors;
                    int currentIncrement = processed % BetterBatchProcessor.this.loggingInterval;
                    int newSuccess = BetterBatchProcessor.this.successfullyProcessedEntries + this.txnSuccesses;
                    // Work out the number of logging intervals we will cross and report them
                    int intervals = (this.txnSuccesses + currentIncrement) / BetterBatchProcessor.this.loggingInterval;
                    if (intervals > 0)
                    {
                        BetterBatchProcessor.this.successfullyProcessedEntries += BetterBatchProcessor.this.loggingInterval
                                - currentIncrement;
                        reportProgress(false);
                        while (--intervals > 0)
                        {
                            BetterBatchProcessor.this.successfullyProcessedEntries += BetterBatchProcessor.this.loggingInterval;
                            reportProgress(false);
                        }
                    }
                    BetterBatchProcessor.this.successfullyProcessedEntries = newSuccess;
                }

                if (this.txnLastError != null)
                {
                    BetterBatchProcessor.this.lastError = this.txnLastError;
                    BetterBatchProcessor.this.lastErrorEntryId = this.txnLastErrorEntryId;
                }

                reset();

                // Make sure we don't wait for a failing transaction
                BetterBatchProcessor.this.retryTxns.remove(this.id);
                BetterBatchProcessor.this.notifyAll();
            }
        }

        @Override
        public void afterCommit()
        {
            // Wake up any waiting batches
            synchronized (BetterBatchProcessor.this)
            {
                BetterBatchProcessor.this.executingCount--;
                // We do the final notifications in commitProgress so we can handle a transaction ending in a rollback
            }
        }

        @Override
        public void afterRollback()
        {
            // Wake up any waiting batches
            synchronized (BetterBatchProcessor.this)
            {
                BetterBatchProcessor.this.executingCount--;
                BetterBatchProcessor.this.retryTxns.add(this.id);
                BetterBatchProcessor.this.notifyAll();
            }
        }
    }
    private class SolrLagAwareArrayBlockingQueue<E> extends ArrayBlockingQueue<E>{

        ReentrantLock solrLock;

        /** Condition for waiting solr indexing */
        private final Condition solrLagExceeded;

        private int takeCounter;

        public SolrLagAwareArrayBlockingQueue(int capacity) {
            super(capacity);
            this.solrLock = new ReentrantLock(false);
            this.solrLagExceeded = solrLock.newCondition();
            this.takeCounter = 0;
        }

        // Add blocking behaviour to work queue
        @Override
        public boolean offer(E o)
        {
            try
            {
                put(o);
            }
            catch (InterruptedException e)
            {
                return false;
            }
            return true;
        }


        // Add blocking behaviour to work queue
        @Override
        public E take() throws InterruptedException
        {
            if(batchCancelled){
                this.clear();
            }
            if (++takeCounter > nbBatches) {
                logger.info(nbBatches + " batches reached !");
                if (takeCounter> nbBatches + workerThreads) {
                    takeCounter = 0;
                }
                long solrLag = solrAdmin.getSolrLag();
                logger.info("solr lag is "+solrLag+"s ..");
                if(maxLag!=-1 && solrLag>maxLag) {
                    while (solrLag > maxLag) {
                        logger.info("solr lag is " + solrLag + "s ..");
                        logger.info("Max solr lag " + maxLag + "s exceeded !");
                        logger.info("Suspending active thread for " + maxLag + "s !");
                        solrLock.lockInterruptibly();
                        try {
                            solrLagExceeded.await(awaitTimeOut, TimeUnit.SECONDS);
                        } finally {
                            solrLock.unlock();
                        }
                        solrLag = solrAdmin.getSolrLag();
                    }
                    logger.info("Batch resumed !");
                }
            }
            return super.take();
        }
    }
}

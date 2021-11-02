package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.BetterBatchProcessor;
import eu.xenit.care4alf.module.bulk.Bulk;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by willem on 12/17/16.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class BulkTest {
    private final Logger logger = LoggerFactory.getLogger(BulkTest.class);

    @BeforeClass
    public static void waitForSolr() throws InterruptedException {
        // Solr might not have indexed the necessary files by the time this test runs.
        // The order in which tests run is essentially random, if this is one of the earlier tests, it's likely to fail
        // if we didn't have this sleep here.
        Thread.sleep(20000);
    }
    @Autowired
    Bulk bulk;

    @Test
    public void testDummy() throws Exception {
        BetterBatchProcessor<NodeRef> processor = bulk.createSearchBatchProcessor(
                20,
                2,
                10,
                180,
                false,
                "dummy",
                null,
                "PATH:\"/app:company_home/app:dictionary//*\"",
                StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
                SearchService.LANGUAGE_FTS_ALFRESCO);
        int results = processor.getSuccessfullyProcessedEntries();
        System.out.printf("Bulk dummy: success: %d fail: %d\n", results, processor.getTotalErrors());
        Assert.assertTrue(results > 0);
    }

    @Test
    public void testReindex() throws Exception {
        BetterBatchProcessor<NodeRef> processor = bulk.createSearchBatchProcessor(
                20,
                2,
                10,
                180,
                false,
                "reindex",
                null,
                "PATH:\"/app:company_home/app:dictionary/app:scripts//*\"",
                StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
                SearchService.LANGUAGE_FTS_ALFRESCO);
        int results = processor.getSuccessfullyProcessedEntries();
        System.out.printf("Bulk reindex: success: %d fail: %d\n", results, processor.getTotalErrors());
        Assert.assertTrue("Expects more then 0 successful results",results > 0);
    }

}

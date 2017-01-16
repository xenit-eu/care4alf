package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.module.bulk.Bulk;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 12/17/16.
 */
@Component
@RunWith(ApixIntegration.class)
public class BulkTest {
    private final Logger logger = LoggerFactory.getLogger(BulkTest.class);

    @Autowired
    Bulk bulk;

    @Test
    public void testDummy() throws Exception {
        BetterBatchProcessor<NodeRef> processor = bulk.createSearchBatchProcessor(
                20,
                2,
                10,
                180,
                "dummy",
                null,
                "TYPE:\"cm:content\"",
                StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
                SearchService.LANGUAGE_FTS_ALFRESCO);
        int results = processor.getSuccessfullyProcessedEntries();
        Assert.assertTrue(results > 0);
    }
}

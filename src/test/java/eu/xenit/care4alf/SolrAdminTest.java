package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.search.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by willem on 5/23/16.
 */
@Component
@RunWith(ApixIntegration.class)
public class SolrAdminTest {
    @Autowired
    private SolrAdmin solrAdmin;

    @Test
    public void testSolrLag() throws Exception {
        long lag = this.solrAdmin.getSolrLag();
        Assert.assertEquals(0, lag);
    }

    @Test
    public void testTransactionsToIndex() throws Exception {
        List<SolrAdmin.Transaction> transactions = this.solrAdmin.getTransactionsToIndex(0);
        Assert.assertTrue(transactions.size() > 0);
    }

    @Test
    public void testNodesToIndex() throws Exception {
        Assert.assertEquals(0, this.solrAdmin.getNodesToIndex());
    }

}
package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.search.SolrAdmin;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.apache.commons.codec.EncoderException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.List;

/**
 * Created by willem on 5/23/16.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
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
        long nodesToIndex = this.solrAdmin.getNodesToIndex();
        Assert.assertTrue(nodesToIndex >= 0 && nodesToIndex <= 100);
    }

    @Test
    public void testSolrErrors() throws Exception {
        Assert.assertEquals(0, this.solrAdmin.getSolrErrors());
    }

    @Test
    public void testOptimize() throws IOException, EncoderException {
        String result = solrAdmin.optimize();
        Assert.assertFalse(result.contains("\"code\":400"));
        System.out.println(result);
        Assert.assertTrue(result.contains("responseHeader"));
    }

}
package eu.xenit.care4alfintegration;

import com.fasterxml.jackson.databind.JsonNode;
import eu.xenit.care4alf.search.AbstractSolrAdminClient;
import eu.xenit.care4alf.search.SolrAdmin;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Created by willem on 4/10/17.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class SolrAdminIntegrationTest {
    @Autowired
    private SolrAdmin solrAdmin;

    @Test
    public void testReindexNode() throws Exception {
        AbstractSolrAdminClient client = this.solrAdmin.getSolrAdminClient();
        JsonNode result = client.reindex(10);
        Assert.assertNotNull(result);
    }

}
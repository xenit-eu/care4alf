package eu.xenit.care4alfintegration;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.search.AbstractSolrAdminClient;
import eu.xenit.care4alf.search.SolrAdmin;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by willem on 4/10/17.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class SolrAdminIntegrationTest {
    @Autowired
    private SolrAdmin solrAdmin;

    @Test
    public void testReindexNode() throws EncoderException, JSONException, IOException {
        AbstractSolrAdminClient client = this.solrAdmin.getSolrAdminClient();
        JSONObject result = client.reindex(10);
    }

}
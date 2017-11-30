package eu.xenit.care4alfintegration.monitoring.metrics;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.metric.SolrSummaryMetrics;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by willem on 5/16/17.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class SolrSummaryMetricsIntegrationTest {
    @Autowired
    private SolrSummaryMetrics solrSummaryMetrics;

    @Test
    public void testMetrics(){
        Map<String, Long> metrics = this.solrSummaryMetrics.getMonitoringMetrics();
        Assert.assertTrue(metrics.size() > 0);
    }

}
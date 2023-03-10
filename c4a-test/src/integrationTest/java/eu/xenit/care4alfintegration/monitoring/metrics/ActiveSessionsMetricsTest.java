package eu.xenit.care4alfintegration.monitoring.metrics;

import eu.xenit.care4alf.monitoring.metric.ActiveSessionsMetric;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * Created by willem on 5/3/17.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class ActiveSessionsMetricsTest {
    @Autowired
    private ActiveSessionsMetric activeSessionsMetrics;

    @Test
    public void testMetrics(){
        Map<String, Long> metrics = activeSessionsMetrics.getMonitoringMetrics();
        Assert.assertTrue(metrics.entrySet().size() >= 1);
        Assert.assertTrue(metrics.get("users.tickets") > 0L);
    }

}
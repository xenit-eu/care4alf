package eu.xenit.care4alfintegration.monitoring.metrics;

import eu.xenit.care4alf.monitoring.metric.AuditMetric;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class GeneralMetricsTest {
    @Autowired
    AuditMetric auditMetric;

    @Test
    public void auditMetricTest(){
        Map<String, Long> metrics = this.auditMetric.getMonitoringMetrics();
        Assert.assertTrue("Should not be empty", metrics.entrySet().size() == 3);
        Assert.assertTrue("Should contain audit.entries", metrics.get("audit.entries") >= 0L);
    }
}
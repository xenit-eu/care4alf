package eu.xenit.care4alfintegration.monitoring.metrics;

import eu.xenit.care4alf.monitoring.metric.LicenseMetric;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Created by willem on 1/19/17.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class LicenseMetricsTest {

    @Autowired
    LicenseMetric licenseMetrics;

    @Test
    public void testLicenseValid() {
        Assert.assertTrue(licenseMetrics.getMonitoringMetrics().containsKey("license.valid"));
    }


}

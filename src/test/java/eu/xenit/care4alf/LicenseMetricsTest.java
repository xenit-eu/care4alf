package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.LicenseMetrics;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 1/19/17.
 */
@Component
@RunWith(ApixIntegration.class)
public class LicenseMetricsTest {

    @Autowired
    LicenseMetrics licenseMetrics;

    @Test
    public void testLicenseValid() {
        Assert.assertTrue(licenseMetrics.getMonitoringMetrics().containsKey("license.valid"));
    }

}

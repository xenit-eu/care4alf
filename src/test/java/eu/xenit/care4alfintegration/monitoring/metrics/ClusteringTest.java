package eu.xenit.care4alfintegration.monitoring.metrics;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.metric.ClusteringMetric;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 7/14/16.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class ClusteringTest {
    @Autowired
    private ClusteringMetric clustering;

    @Test
    public void testGetNumClusterMembers(){
        int nClusterMembers = this.clustering.getNumClusterMembers();
        Assert.assertTrue(nClusterMembers <= 1 && nClusterMembers >= 0);
    }
}

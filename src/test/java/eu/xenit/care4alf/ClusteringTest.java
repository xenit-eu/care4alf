package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 7/14/16.
 */
@Component
@RunWith(ApixIntegration.class)
public class ClusteringTest {
    @Autowired
    Clustering clustering;

    @Test
    public void testGetNumClusterMembers(){
        int nClusterMembers = this.clustering.getNumClusterMembers();
        Assert.assertTrue(nClusterMembers <= 1 && nClusterMembers >= 0);
    }
}

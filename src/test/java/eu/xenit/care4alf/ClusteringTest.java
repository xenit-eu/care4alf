package eu.xenit.care4alf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

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
        Assert.assertEquals(1,this.clustering.getNumClusterMembers());
    }
}

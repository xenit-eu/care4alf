package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.Amps;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.alfresco.service.cmr.repository.NodeRef;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class AmpsTest {
    @Autowired
    private Amps amps;

    @Test
    public void getAmps(){
        List<NodeRef> nodeRefs = amps.getAmps();
        Assert.assertTrue(nodeRefs.size() > 0);
    }

}
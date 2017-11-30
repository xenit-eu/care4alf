package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.scaling.Scaling;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by willem on 12/19/16.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class ScalingTest {
    @Autowired
    private Scaling scaling;

    @Test
    public void testVars() throws SQLException {
        Map<String, Integer> vars = scaling.getNTAX();
        Assert.assertTrue(vars.get("N1")>0);
        Assert.assertTrue(vars.get("N2")>0);
        Assert.assertTrue(vars.get("N3")>0);
        Assert.assertTrue(vars.get("T")>0);
        Assert.assertTrue(vars.get("A")>0);
        Assert.assertTrue(vars.get("X")>0);
    }
}

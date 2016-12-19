package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.scaling.Scaling;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Map;

/**
 * Created by willem on 12/19/16.
 */
@Component
@RunWith(ApixIntegration.class)
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

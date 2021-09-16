package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.Config;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by willem on 12/19/16.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class ConfigTest {

    @Autowired
    Config config;

    @Test
    public void crudOperationsProperty(){
        config.addProperty("ConfigTest","123");
        Assert.assertEquals("123",config.getProperty("ConfigTest"));
        config.removeProperty("ConfigTest");
        Assert.assertEquals(null, config.getProperty("ConfigTest"));
    }

}

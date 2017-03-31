package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 12/19/16.
 */
@Component
@RunWith(ApixIntegration.class)
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

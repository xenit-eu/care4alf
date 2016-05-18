package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 5/1/16.
 */
@Component
@RunWith(ApixIntegration.class)
public class AttributesTest {
    @Autowired
    Attributes attributes;

    @Test
    public void testListNotEmpty() throws Exception {
        Assert.assertTrue(this.attributes.list().size() >= 1);
    }
}

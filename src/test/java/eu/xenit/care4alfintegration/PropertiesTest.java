package eu.xenit.care4alfintegration;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.Properties;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by willem on 4/6/16.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class PropertiesTest {

    @Autowired
    Properties properties;

    @Test
    public void testResidualProperty() throws Exception {
        List<QName> residualProperties = this.properties.getResidualProperties();
        Assert.assertTrue(residualProperties.contains(QName.createQName("http://www.alfresco.org/model/content/1.0","source")));
        Assert.assertFalse(residualProperties.contains(ContentModel.PROP_NAME));
    }

}
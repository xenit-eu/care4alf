package eu.xenit.care4alfintegration;

import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import eu.xenit.care4alf.ContentStore;
import eu.xenit.care4alf.MissingContent;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by Thomas.Straetmans on 11/05/2017.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class ContentStoreTest {

    @Autowired
    ContentStore contentStore;

    private Logger logger = LoggerFactory.getLogger(ContentStoreTest.class);

    @Test
    public void getListReturnsNotNull(){
        Resolution res = contentStore.list(null);
        //Assert.assertNotEquals(null, res);
        Assert.assertNotNull(res);
    }

    @Test
    public void getIntegrityResults(){
        List<MissingContent> missingContent = this.contentStore.getIntegrityCheckResults();
        Assert.assertTrue(missingContent.size() == 0);
    }
}

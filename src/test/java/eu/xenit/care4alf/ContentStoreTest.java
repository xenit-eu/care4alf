package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by Thomas.Straetmans on 11/05/2017.
 */
@Component
@RunWith(ApixIntegration.class)
public class ContentStoreTest {

    @Autowired
    ContentStoreJava contentStoreJava;

    private Logger logger = LoggerFactory.getLogger(ContentStoreTest.class);

    @Test
    public void getListReturnsNotNull(){
        Resolution res = contentStoreJava.list(null);
        Assert.assertNotEquals(null, res);
    }
}

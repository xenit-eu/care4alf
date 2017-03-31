package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Created by willem on 6/6/16.
 */
@Component
@RunWith(ApixIntegration.class)
public class SQLQueryTest {
    @Autowired
    private Sql sqlQuery;

    @Test
    public void testValidateQuery() throws Exception {
        List<List<String>> results = sqlQuery.query("select 1");
        Assert.assertEquals("1",results.get(0).get(0));
    }

    public void testMultipleColumns() throws Exception {
        List<List<String>> results = sqlQuery.query("select 1,2");
        Assert.assertEquals("1",results.get(0).get(0));
        Assert.assertEquals("2",results.get(0).get(1));
    }

}
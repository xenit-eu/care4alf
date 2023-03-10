package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.Sql;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Created by willem on 6/6/16.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class SQLQueryTest {
    @Autowired
    private Sql sqlQuery;

    @Test
    public void testValidateQuery() throws Exception {
        List<List<String>> results = sqlQuery.query("select 1");
        Assert.assertEquals("1",results.get(1).get(0));
    }

    public void testMultipleColumns() throws Exception {
        List<List<String>> results = sqlQuery.query("select 1,2");
        Assert.assertEquals("1",results.get(1).get(0));
        Assert.assertEquals("2",results.get(1).get(1));
    }

}
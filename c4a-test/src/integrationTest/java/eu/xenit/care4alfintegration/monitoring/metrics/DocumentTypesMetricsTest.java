package eu.xenit.care4alfintegration.monitoring.metrics;

import eu.xenit.care4alf.monitoring.metric.DocumentTypesMetrics;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.alfresco.service.namespace.QName;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.Map;


/**
 * Created by willem on 4/19/17.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class DocumentTypesMetricsTest {

    @Autowired
    DocumentTypesMetrics documentTypesMetrics;

    @Test
    public void testInternals() throws SQLException {
        Map<QName, Long> types = documentTypesMetrics.getTypesCount();
        Assert.assertTrue("Types can not be empty", types.size() > 1);
        System.out.println(types);
    }

    @Test
    public void testMetrics() throws SQLException {
        Map<String, Long> metrics = documentTypesMetrics.getMonitoringMetrics();

        Assert.assertTrue(documentTypesMetrics.getMonitoringMetrics().keySet().size() > 1);

        System.out.println(metrics.size());

        Assert.assertTrue(metrics.containsKey("documenttypes.cm.content"));
        Assert.assertTrue(metrics.containsKey("documenttypes.cm.folder"));
    }

}
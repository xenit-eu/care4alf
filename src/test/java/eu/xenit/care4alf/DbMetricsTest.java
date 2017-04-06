package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.DbMetrics;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by willem on 1/17/17.
 */
@Component
@RunWith(ApixIntegration.class)
public class DbMetricsTest {

    @Autowired
    DbMetrics dbMetrics;

    @Test
    public void testDBConnectionpoolCount() throws IOException, SQLException {
        Map<String, Long> metrics = dbMetrics.getMonitoringMetrics();
        Assert.assertTrue(metrics.entrySet().size() >= 1);
        Assert.assertTrue(metrics.get("db.connectionpool.active") > 0L);
        Assert.assertTrue(metrics.get("db.ping") >= 0L);
    }

    @Test
    public void testPing() throws IOException {
        long time = dbMetrics.ping("www.google.com");
        System.out.println(time);
        Assert.assertTrue(time >= 0);
    }

}
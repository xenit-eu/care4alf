package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.ActiveSessionsMetrics;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by willem on 5/3/17.
 */
@Component
@RunWith(ApixIntegration.class)
public class ActiveSessionsMetricsTest {
    @Autowired
    private ActiveSessionsMetrics activeSessionsMetrics;

    @Test
    public void testMetrics(){
        Map<String, Long> metrics = activeSessionsMetrics.getMonitoringMetrics();
        Assert.assertTrue(metrics.entrySet().size() >= 1);
        Assert.assertTrue(metrics.get("users.tickets") > 0L);
    }

}
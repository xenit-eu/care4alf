package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.SystemMetrics;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Created by willem on 12/21/16.
 */
@Component
@RunWith(ApixIntegration.class)
public class RuntimeMetricsTest {

    @Autowired
    SystemMetrics runtimeMetrics;

    @Test
    public void testRuntimeMetrics(){
        Map<String, Long> metrics = runtimeMetrics.getMonitoringMetrics();
        Assert.assertEquals(6, metrics.size());
    }

}
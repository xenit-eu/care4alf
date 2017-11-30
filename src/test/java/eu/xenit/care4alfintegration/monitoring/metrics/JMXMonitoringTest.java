package eu.xenit.care4alfintegration.monitoring.metrics;

import eu.xenit.care4alf.monitoring.metric.MemoryMetric;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by willem on 1/16/17.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class JMXMonitoringTest {

    @Autowired
    private MemoryMetric memoryMetrics;

    @Test
    public void dataRealistic(){
        Map<String, Long> map = memoryMetrics.getMonitoringMetrics();
        assertTrue(map.get("jvm.memory.eden.used") > 0L);
        assertTrue(map.get("jvm.memory.eden.max") > 0L);
        assertTrue(map.get("jvm.memory.heap.used") > 0L);
    }

    @Test
    public void getDBConnections(){

    }
}

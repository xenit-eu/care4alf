package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.MemoryMetrics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by willem on 1/16/17.
 */
@Component
@RunWith(ApixIntegration.class)
public class JMXMonitoringTest {

    @Autowired
    private MemoryMetrics memoryMetrics;

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

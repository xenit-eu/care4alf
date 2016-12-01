package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.JMXMonitoring;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertTrue;

/**
 * Created by Thomas.Straetmans on 01/12/2016.
 */
@Component
@RunWith(ApixIntegration.class)
public class MonitoringTest {

    @Autowired
    private JMXMonitoring jmxMonitoring;

    @Test
    public void testBeansExist(){
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

    @Test
    public void dataRealistic(){
        Map<String, Long> map = jmxMonitoring.getMonitoringMetrics();
        assertTrue(map.get("memory.eden.used.MB") > 0L);
        assertTrue(map.get("memory.eden.max.MB") > 0L);
        assertTrue(map.get("system.processors") > 0);
        assertTrue(map.get("system.threads")> 0);
    }
}

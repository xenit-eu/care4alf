package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.GraphiteMetricsShipper;
import eu.xenit.care4alf.monitoring.JMXMonitoring;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.ArrayList;
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

    @Autowired
    private Monitoring monitoring;

    @Autowired
    GraphiteMetricsShipper shipper;

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
        assertTrue(map.get("jvm.memory.eden.used.MB") > 0L);
        assertTrue(map.get("jvm.memory.eden.max.MB") > 0L);
    }

    @Test
    public void metricCantContainSpaces() throws Exception {
        List<String> keys = new ArrayList<>(monitoring.getAllMetrics().keySet());
        for(String key:keys){
            if(key.contains(" ")){
                Assert.fail("Key contains space: " + key);
            }
        }
    }

    @Test
    public void getHostName() {
        String name = shipper.getServerName();
        System.out.println(name);
        Assert.assertTrue(name != null);
    }
}

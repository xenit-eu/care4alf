package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.GraphiteMetricsShipper;
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
        String serverName = shipper.getServerName();
        System.out.println(serverName);
        Assert.assertTrue(serverName != null);
    }

    @Test
    public void noConflictingNameSpaces() throws Exception {
        List<String> keys = new ArrayList<>(monitoring.getAllMetrics().keySet());
        //Assert.assertFalse(keys.contains("solr.lag") && keys.contains("solr.lag.nodes"));
        for(String key : keys){
            for(String conflictKey : keys){
                if(key.equals(conflictKey))
                    continue;

                if(conflictKey.contains(key+"."))
                    System.out.println(String.format("Conflicting key between '%s' and '%s'", key, conflictKey));
            }
        }
    }

}

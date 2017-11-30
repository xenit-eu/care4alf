package eu.xenit.care4alfintegration.monitoring.metrics;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.monitoring.metric.SystemMetrics;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.List;
import java.util.Map;

/**
 * Created by willem on 12/21/16.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class RuntimeMetricsTest {

    @Autowired
    SystemMetrics runtimeMetrics;

    @Test
    public void testRuntimeMetrics(){
        Map<String, Long> metrics = runtimeMetrics.getMonitoringMetrics();
        Assert.assertTrue(metrics.size() > 0);
        Assert.assertTrue(metrics.containsKey("jvm.memory.runtime.free"));
        Assert.assertTrue(metrics.containsKey("jvm.memory.runtime.max"));
        Assert.assertTrue(metrics.containsKey("jvm.memory.runtime.total"));
        Assert.assertTrue(metrics.containsKey("jvm.threads.count"));
    }

    @Test
    public void testBeansExist(){
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
    }

}
package eu.xenit.care4alf.monitoring;

import eu.xenit.care4alf.integration.MonitoredSource;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

//import java.lang.management.ManagementFactory;
//import java.lang.management.OperatingSystemMXBean;
//import com.sun.management.UnixOperatingSystemMXBean;

/**
 * Created by willem on 12/20/16.
 */
@Component
public class SystemMetrics implements MonitoredSource {
    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Runtime runtime = Runtime.getRuntime();

        Map<String, Long> map = new HashMap<>();
        map.put("jvm.memory.max",runtime.maxMemory()/1024/1024);
        map.put("jvm.memory.free",runtime.freeMemory()/1024/1024);
        map.put("jvm.memory.total",runtime.totalMemory()/1024/1024);

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

        map.put("system.loadavg", (long) os.getSystemLoadAverage()*100);
        map.put("system.processors", (long) os.getAvailableProcessors());

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        map.put("jvm.threads", (long) threadMXBean.getThreadCount());

//        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
//        if(os instanceof UnixOperatingSystemMXBean){
//            map.put("os.openfiles.count",((UnixOperatingSystemMXBean)os).getOpenFileDescriptorCount());
//            map.put("os.openfiles.max",((UnixOperatingSystemMXBean) os).getMaxFileDescriptorCount());
//        }

        return map;
    }
}

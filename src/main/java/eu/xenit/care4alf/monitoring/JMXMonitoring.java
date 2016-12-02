package eu.xenit.care4alf.monitoring;

import eu.xenit.care4alf.integration.MonitoredSource;
import org.springframework.stereotype.Component;

import java.lang.management.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Thomas.Straetmans on 30/11/2016.
 */
@Component
public class JMXMonitoring implements MonitoredSource {

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> metrics = new HashMap<>();

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            String name = null;
            switch (pool.getName()){
                case "Par Eden Space": name = "eden";
                    break;
                case "Par Survivor Space": name = "survivor";
                    break;
                case "CMS Old Gen": name = "old";
                    break;
            }
            if(name != null){
                MemoryUsage usage = pool.getUsage();
                metrics.put("memory."+name+".used.MB", usage.getUsed()/ 1024000);
                metrics.put("memory."+name+".max.MB", usage.getMax()/ 1024000);
            }
            MemoryUsage usage = pool.getUsage();

        }

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for(GarbageCollectorMXBean bean : gcBeans){
            metrics.put("gc."+bean.getName()+".count",bean.getCollectionCount());
            metrics.put("gc."+bean.getName()+".time.ms",bean.getCollectionTime());
        }

        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

        metrics.put("system.processors",(long) operatingSystemMXBean.getAvailableProcessors());
        metrics.put("system.loadaverage.times100",(long) (operatingSystemMXBean.getSystemLoadAverage()*100));

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

        metrics.put("system.threads",(long) threadMXBean.getThreadCount());
        return metrics;
    }
}

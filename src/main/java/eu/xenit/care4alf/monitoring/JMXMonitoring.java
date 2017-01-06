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
            String poolName = pool.getName().toLowerCase();
            if(poolName.contains("eden"))
                name = "eden";
            else if(poolName.contains("survivor"))
                name = "survivor";
            else if(poolName.contains("old"))
                name = "old";
            else
                name = poolName.toLowerCase().replace(" ","");
            if(name != null){
                MemoryUsage usage = pool.getUsage();
                metrics.put("jvm.memory."+name+".used.MB", usage.getUsed()/ 1024000);
                metrics.put("jvm.memory."+name+".max.MB", usage.getMax()/ 1024000);
            }
        }

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for(GarbageCollectorMXBean bean : gcBeans){
            metrics.put("jvm.gc."+bean.getName().replace(" ","")+".count",bean.getCollectionCount());
            metrics.put("jvm.gc."+bean.getName().replace(" ","")+".time.ms",bean.getCollectionTime());
        }

        return metrics;
    }
}

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
public class GCMonitoring implements MonitoredSource {

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> metrics = new HashMap<>();

        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for(GarbageCollectorMXBean bean : gcBeans){
            metrics.put("jvm.gc."+bean.getName().replace(" ","")+".count",bean.getCollectionCount());
            metrics.put("jvm.gc."+bean.getName().replace(" ","")+".time.ms",bean.getCollectionTime());
            metrics.put("jvm.gc."+bean.getName().replace(" ","")+".time.s",bean.getCollectionTime()/1000);
        }

        return metrics;
    }
}

package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.schedule.ScheduledTask;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.springframework.stereotype.Component;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Thomas.Straetmans on 30/11/2016.
 */
@Component
@ScheduledTask(name = "GCMonitoring", group = Monitoring.SCHEDULE_GROUP, cron = "0 0/5 * * * ?"
        , cronProp = "c4a.monitoring.gcmonitoring.cron") //We go light on this because it already exists in jmxTrans
public class GCMonitoring extends AbstractMonitoredSource {

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

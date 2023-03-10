package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.schedule.ScheduledTask;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 12/20/16.
 */
@Component
@ScheduledTask(name = "SystemMetrics", group = Monitoring.SCHEDULE_GROUP, cron = "0 0/5 * * * ?", cronProp = "c4a.monitoring.system.cron")
public class SystemMetrics extends AbstractMonitoredSource {
    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Runtime runtime = Runtime.getRuntime();//cfr system

        Map<String, Long> map = new HashMap<>();
        map.put("jvm.memory.runtime.max",runtime.maxMemory());//bytes as base units
        map.put("jvm.memory.runtime.free",runtime.freeMemory());
        map.put("jvm.memory.runtime.total",runtime.totalMemory());

        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();

        long loadAvg = (long) os.getSystemLoadAverage()*100;
        long availableProcessors = (long) os.getAvailableProcessors();
        long loadOverCores = loadAvg/availableProcessors;

        map.put("system.loadavg", loadAvg);
        map.put("system.processors", availableProcessors);
        map.put("system.loadPerNmbrOfCores", loadOverCores);

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        map.put("jvm.threads.count", (long) threadMXBean.getThreadCount());

        Map<Thread.State,Long> countStates = new HashMap<Thread.State, Long>();
        for(Thread.State state : Thread.State.values()){
            countStates.put(state,0L);
        }
        for(Long threadId : threadMXBean.getAllThreadIds()){
            Thread.State state = threadMXBean.getThreadInfo(threadId).getThreadState();
            countStates.put(threadMXBean.getThreadInfo(threadId).getThreadState(), countStates.get(state)+1);
        }
        for(Thread.State state : Thread.State.values()){
            map.put("jvm.threads."+state.name().toLowerCase(), countStates.get(state));
        }

        return map;
    }
}

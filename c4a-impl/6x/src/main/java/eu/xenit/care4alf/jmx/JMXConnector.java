package eu.xenit.care4alf.jmx;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.stereotype.Component;

/**
 * Created by Thomas.Straetmans on 24/11/2016.
 */
@Component
public class JMXConnector {

    public String getData() {
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();

        // generate heap state report
        String report = "";
        for (GarbageCollectorMXBean gc : gcBeans) {
            report += "\nGC Name         : " + gc.getName();
            report += "\nCollection count: " + gc.getCollectionCount();
            report += "\nCollection Time : " + gc.getCollectionTime() + " milli seconds";
            report += "\n";
        }

        List<MemoryPoolMXBean> memoryPoolMXBeans = ManagementFactory.getMemoryPoolMXBeans();
        for (MemoryPoolMXBean pool : memoryPoolMXBeans) {
            report += "\nMemory Pool: " + pool.getName();
            report += "\nType: " + pool.getType().name();
            MemoryUsage usage = pool.getUsage();
            report += "\n   Max : " + usage.getMax() / 1024000 + "MB";
            report += "\n   Used: " + usage.getUsed() / 1024000 + "MB";
            report += "\n";
        }

        ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        report += "\nDeadlocked threads: "+threadMXBean.findDeadlockedThreads();
        report += "\n";
        report += "Number of threads: "+threadMXBean.getThreadCount();
        report += "\n";
        report += "Number of deamon threads: "+threadMXBean.getDaemonThreadCount();
        report += "\n";
        report += "Id's of currently running threads: "+threadMXBean.getAllThreadIds();
        report += "\n";

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        Map<String, String> map = runtimeMXBean.getSystemProperties();
        report += "\nSystem Properties:";
        for(Map.Entry<String, String> entry: map.entrySet()){
            report += "\n  - "+entry.getKey()+" : "+ StringEscapeUtils.escapeJava(entry.getValue());
        }
        report += "\n";

        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        report += "\nSystem Load Average: " + operatingSystemMXBean.getSystemLoadAverage();
        report += "\n";
        report += "\nAvailable Processors: " + operatingSystemMXBean.getAvailableProcessors();
        report += "\n";

        ManagementFactory.getPlatformMBeanServer();


        return report;
    }
}

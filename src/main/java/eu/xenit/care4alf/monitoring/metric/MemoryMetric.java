package eu.xenit.care4alf.monitoring.metric;

import eu.xenit.care4alf.integration.MonitoredSource;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by willem on 1/16/17.
 */
@Component
public class MemoryMetric extends AbstractMonitoredSource {

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> map = new HashMap<String, Long>();

        // Retrieve memory managed bean from management factory.
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean() ;
        MemoryUsage heap = memBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memBean.getNonHeapMemoryUsage();

        // Retrieve the four values stored within MemoryUsage:
        // init: Amount of memory in bytes that the JVM initially requests from the OS.
        // used: Amount of memory used.
        // committed: Amount of memory that is committed for the JVM to use.
        // max: Maximum amount of memory that can be used for memory management.

        map.put("jvm.memory.heap.init", heap.getInit());
        map.put("jvm.memory.heap.used", heap.getUsed());
        map.put("jvm.memory.heap.committed",heap.getCommitted());
        map.put("jvm.memory.heap.max", heap.getMax());

        map.put("jvm.memory.nonheap.init", nonHeap.getInit());
        map.put("jvm.memory.nonheap.used", nonHeap.getUsed());
        map.put("jvm.memory.nonheap.committed",nonHeap.getCommitted());
        map.put("jvm.memory.nonheap.max", nonHeap.getMax());

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
                map.put("jvm.memory."+name+".used", usage.getUsed());//base unit = bytes
                map.put("jvm.memory."+name+".max", usage.getMax());
            }
        }

        return map;
    }

}

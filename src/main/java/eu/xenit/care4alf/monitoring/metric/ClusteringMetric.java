package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.jobs.ScheduledQuartzJob;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.management.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 7/14/16.
 */
@Component
@ScheduledQuartzJob(name = "ClusteringMetric", group = Monitoring.SCHEDULE_GROUP, cron = "0/15 * * * * ?", cronProp = "c4a.monitoring.clustering.cron")
public class ClusteringMetric extends AbstractMonitoredSource {
    @Autowired
    private MBeanServerConnection alfrescoMBeanServer;

    public int getNumClusterMembers(){
        try {
            AttributeList attributes = alfrescoMBeanServer.getAttributes(new ObjectName("Alfresco:Name=Cluster,Tool=Admin"), new String[]{"NumClusterMembers"});
            int numClusterMembers = (Integer) ((Attribute) attributes.get(0)).getValue();
            return numClusterMembers;
        } catch(Exception e){
        }
        return -1;
    }

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> r = new HashMap<>();
        r.put("cluster.nodes", (long) this.getNumClusterMembers());
        return r;
    }
}
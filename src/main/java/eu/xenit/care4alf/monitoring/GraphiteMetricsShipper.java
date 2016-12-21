package eu.xenit.care4alf.monitoring;

import com.github.dynamicextensionsalfresco.jobs.ScheduledQuartzJob;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

/**
 * Created by willem on 12/13/16.
 */
@Component
@ScheduledQuartzJob(name = "Graphite Metrics shipper", cron = "* 0/5 * * * ?")
public class GraphiteMetricsShipper implements Job {
    private final Logger logger = LoggerFactory.getLogger(GraphiteMetricsShipper.class);

    private GraphiteClient client;

    @Autowired
    Monitoring monitoring;

    @Autowired()
    @Qualifier("global-properties")
    private java.util.Properties properties;

    private boolean enabled = false;
    private String name = "alfresco";

    public String getServerName(){
        return this.name;
    }

    @PostConstruct
    public void initGraphiteClient(){
        this.enabled = Boolean.parseBoolean(properties.getProperty("c4a.monitoring.graphite.enabled", "false"));

        if(!enabled)
            return;

        try {
            this.name = properties.getProperty("c4a.monitoring.graphite.prefix", InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            logger.warn("Couldn't fetch hostname");
        }

        this.client = new GraphiteClient(
            properties.getProperty("c4a.monitoring.graphite.host", "carbon"),
            Integer.parseInt(properties.getProperty("c4a.monitoring.graphite.port", "2003"))
        );
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if(!enabled)
            return;

        logger.debug("Fetching and sending metrics to Graphite");
        try {
            Map<String, Long> prefixed = monitoring.getAllMetrics();
            for(Map.Entry<String, Long> metric: monitoring.getAllMetrics().entrySet()){
                prefixed.put(this.name + "." + metric.getKey(), metric.getValue());
            }
            client.send(prefixed);
        } catch (Exception e) {
            logger.warn("Can't send metrics to Graphite");
            if(logger.isDebugEnabled())
                e.printStackTrace();
        }
    }
}
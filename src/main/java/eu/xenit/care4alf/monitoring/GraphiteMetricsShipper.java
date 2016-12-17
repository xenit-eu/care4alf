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
    private String prefix = "alfresco";//TODO: integrate

    @PostConstruct
    public void initGraphiteClient(){
        this.enabled = Boolean.parseBoolean(properties.getProperty("c4a.monitoring.graphite.enabled", "true"));

        if(!enabled)
            return;

        this.prefix = properties.getProperty("c4a.monitoring.graphite.prefix");

        this.client = new GraphiteClient(
                properties.getProperty("c4a.monitoring.graphite.host", "172.21.0.2"),
                Integer.parseInt(properties.getProperty("c4a.monitoring.graphite.port", "2003"))
        );
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        if(!enabled)
            return;

        logger.debug("Fetching and sending metrics to Graphite");
        try {
            client.send(monitoring.getAllMetrics());
        } catch (Exception e) {
            logger.warn("Can't send metrics to Graphite");
            if(logger.isDebugEnabled())
                e.printStackTrace();
        }
    }
}
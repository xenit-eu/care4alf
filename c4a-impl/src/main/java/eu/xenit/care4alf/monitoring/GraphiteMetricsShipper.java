package eu.xenit.care4alf.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 12/13/16.
 */
@Component
public class GraphiteMetricsShipper extends MetricsShipper {
    public static final String NAME = "graphite";
    public static final String SANITIZE_KEY_REGEX = "[^A-Za-z0-9_.-]";
    private final Logger logger = LoggerFactory.getLogger(GraphiteMetricsShipper.class);

    private GraphiteClient client;


    @Override
    public String getName(){
        return NAME;
    }

    @PostConstruct
    public void initGraphiteClient(){
        this.client = new GraphiteClient(
            properties.getProperty("c4a.monitoring.graphite.host", "carbon"),
            Integer.parseInt(properties.getProperty("c4a.monitoring.graphite.port", "2003"))
        );
    }

    @Override
    public void send(Map<String, Long> metrics, String serverName){
        Map<String, Long> prefixed = new HashMap<>();
        for(Map.Entry<String, Long> metric: metrics.entrySet()){
            prefixed.put(serverName + "." + metric.getKey().replaceAll(SANITIZE_KEY_REGEX,""), metric.getValue());
        }

        if(logger.isDebugEnabled())
            logger.debug("Sending {} metrics with prefix : {}",prefixed.size(), serverName);

        try {
            client.send(prefixed);
        } catch (Exception e) {
            logger.warn("Can't send metrics to Graphite");
            if(logger.isDebugEnabled())
                e.printStackTrace();
        }
    }

    public String toString(){
        return this.getName() + " : " + this.client.toString();
    }
}
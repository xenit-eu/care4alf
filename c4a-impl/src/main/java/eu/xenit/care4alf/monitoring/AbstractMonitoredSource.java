package eu.xenit.care4alf.monitoring;

import com.github.dynamicextensionsalfresco.schedule.Task;
import eu.xenit.care4alf.integration.MonitoredSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public abstract class AbstractMonitoredSource implements MonitoredSource, Task {
    private final Logger logger = LoggerFactory.getLogger(AbstractMonitoredSource.class);

    @Autowired
    @Qualifier("global-properties")
    private java.util.Properties properties;

    private String shipperName;
    private boolean enabled = false;
    private MetricsShipper shipper;

    @Autowired(required = false)
    private List<MetricsShipper> allShippers;

    @PostConstruct
    public void initMonitoredSourceConfig() {
        this.shipperName = properties.getProperty("c4a.monitoring.shipper", "graphite");

        // Config to disable/enable a particular shipper
        this.enabled = Boolean.parseBoolean(getMonitoringConfigProperty("enabled", shipperName, "false"))
                && Boolean.parseBoolean(getMonitoringConfigProperty("enabled", "metric." + this.getName(), "true"));

        if (!enabled) {
            logger.warn("Metric {} is disabled!", this.getName());
            return;
        }

        for (MetricsShipper ms : allShippers) {
            if (ms.getName().equals(shipperName)) {
                shipper = ms;
                break;
            }
        }
        if (shipper == null) {
            logger.error("A metrics shipper was configured, but no implementation was found !");
            logger.error("Disabling monitoring...");
            this.enabled = false;
        }
    }

    private String getMonitoringConfigProperty(String subKey, String key, String defaultValue) {
        return properties.getProperty("c4a.monitoring." + key + "." + subKey,
                properties.getProperty("c4a.monitoring." + subKey, defaultValue));
    }

    @Override
    public void execute() {
        if (!enabled) {
            return;
        }

        try {
            logger.debug("Fetching and sending metrics...");
            shipper.send(this.getMonitoringMetrics());
        } catch (Exception e) {
            logger.warn("Can't send metrics: " + this.shipper.toString());
            if (logger.isDebugEnabled()) {
                e.printStackTrace();
            }
        }
    }

    public String getName() {
        return this.getClass().getSimpleName().toLowerCase().replace("metrics", "").replace("metric", "");
    }
}

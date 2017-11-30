package eu.xenit.care4alf.monitoring;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import eu.xenit.care4alf.integration.MonitoredSource;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by willem on 3/7/16.
 */
@Component
@WebScript(families = "care4alf", description = "Monitoring")
@Authentication(AuthenticationType.NONE)
public class Monitoring implements ApplicationContextAware {
    private final Logger logger = LoggerFactory.getLogger(Monitoring.class);
    public static final String SCHEDULE_GROUP = "Monitoring";

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Autowired(required = false)
    List<MonitoredSource> allMonitoredSources;

    public List<MonitoredSource> getAllMonitoredSources(){
        return this.allMonitoredSources;
    }

    public Map<String, Long> getAllMetrics() throws Exception {
        final Map<String, Long> vars = new HashMap<String, Long>();

        // find all beans implementing the MonitoredSource interface and get the metrics to add.
        logger.debug("Scanning parent spring context for beans implementing MonitoredSource interface...");
        if (allMonitoredSources != null) {
            for (MonitoredSource source : allMonitoredSources) {
                if(source == null) {
                    continue;
                }

                if(!isEnabled((source))){
                    logger.debug("Source %s is not enabled", source.getClass().getSimpleName());
                    if(source instanceof AbstractMonitoredSource)
                        logger.debug("Enable by setting property c4a.monitoring.metric.{}.enabled=true",((AbstractMonitoredSource) source).getName());
                    continue;
                }

                try {
                    Long start = System.currentTimeMillis();
                    AbstractMonitoredSource aSource = (AbstractMonitoredSource) source;
                    Map<String, Long> metrics = source.getMonitoringMetrics();
                    if (metrics != null) {
	                    for (String key : metrics.keySet()) {
	                        vars.put(key, metrics.get(key));
	                    }
                    }
                    Long diff = System.currentTimeMillis() - start;
                    logger.debug("Metric '{}' took {} ms", aSource.getName(), diff);
                    vars.put("metrics." + aSource.getName() + ".timing", diff);
                }
                catch(Exception e){
                    logger.warn("Can't fetch some metric");
                    e.printStackTrace();
                }
            }
        }

        return vars;
    }

    @Autowired
    @Qualifier("global-properties")
    private java.util.Properties properties;

    public boolean isEnabled(MonitoredSource monitoredSource){
        AbstractMonitoredSource abstractMonitoredSource = (AbstractMonitoredSource) monitoredSource;
        return Boolean.parseBoolean(properties.getProperty("c4a.monitoring.metric." + abstractMonitoredSource.getName() + ".enabled", "true"));
    }

    @Uri(value = "/xenit/care4alf/monitoring/vars")
    @Transaction
    public Resolution getVars(WebScriptResponse response) throws Exception {
        final Map<String, Long> vars = this.getAllMetrics();

        return prepareResult(vars);
    }

    @Uri(value = "/xenit/care4alf/monitoring/{metric}")
    public Resolution getClusterMetrics(@UriVariable final String metric, WebScriptResponse response) throws Exception {
        final Map<String, Long> vars = new HashMap<String, Long>();

        if (allMonitoredSources != null) {
            for (MonitoredSource source : allMonitoredSources) {
                if (source == null)
                    continue;
                if (source instanceof AbstractMonitoredSource && ((AbstractMonitoredSource) source).getName().equals(metric)) {
                    try {
                        Map<String, Long> metrics = source.getMonitoringMetrics();
                        for (String key : metrics.keySet()) {
                            vars.put(key, metrics.get(key));
                        }
                    } catch (Exception e) {
                        logger.warn("Can't fetch some metric");
                    }
                }
            }
        }

        return prepareResult(vars);
    }

    @NotNull
    private JsonWriterResolution prepareResult(final Map<String, Long> vars) throws JSONException {
        return new JsonWriterResolution() {
            protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                jsonWriter.object();
                for (Map.Entry<String, Long> entry : vars.entrySet())
                    jsonWriter.key(entry.getKey()).value(entry.getValue());
                jsonWriter.endObject();
            }
        };
    }
}

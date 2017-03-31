package eu.xenit.care4alf.monitoring;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import eu.xenit.care4alf.integration.MonitoredSource;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
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

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Autowired(required = false)
    List<MonitoredSource> allMonitoredSources;

    public Map<String, Long> getAllMetrics() throws Exception {
        final Map<String, Long> vars = new HashMap<String, Long>();

        // find all beans implementing the MonitoredSource interface and get the metrics to add.
        logger.debug("Scanning parent spring context for beans implementing MonitoredSource interface...");
        if (allMonitoredSources != null) {
            for (MonitoredSource source : allMonitoredSources) {
                if(source == null)
                    continue;
                try {
                    Map<String, Long> metrics = source.getMonitoringMetrics();
                    for (String key : metrics.keySet()) {
                        vars.put(key, metrics.get(key));
                        logger.debug(key + " done");
                    }
                }
                catch(Exception e){
                    logger.warn("Can't fetch some metric");
                }
            }
        }

        return vars;
    }

    @Uri(value = "/xenit/care4alf/monitoring/vars")
    public Resolution getVars(WebScriptResponse response) throws Exception {
        final Map<String, Long> vars = this.getAllMetrics();

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

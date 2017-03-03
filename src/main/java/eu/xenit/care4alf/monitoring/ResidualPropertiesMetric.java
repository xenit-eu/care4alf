package eu.xenit.care4alf.monitoring;

import eu.xenit.care4alf.Properties;
import eu.xenit.care4alf.integration.MonitoredSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 3/3/17.
 */
public class ResidualPropertiesMetric implements MonitoredSource {

    @Autowired
    private Properties properties;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> r = new HashMap<String, Long>();
        r.put("properties.residual", this.getResidualProperties("alfresco"));
        return r;
    }

    private long getResidualProperties(String filter) {
        try {
            return this.properties.getResidualProperties(filter).size();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

}

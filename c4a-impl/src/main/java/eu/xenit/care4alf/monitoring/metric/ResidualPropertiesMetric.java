package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.schedule.ScheduledTask;
import eu.xenit.care4alf.Properties;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 3/3/17.
 */
@Component
@ScheduledTask(name = "ResidualPropertiesMetric", group = Monitoring.SCHEDULE_GROUP, cron = "0 0 1 * * ?"
        , cronProp = "c4a.monitoring.risidualproperties.cron")
public class ResidualPropertiesMetric extends AbstractMonitoredSource {

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

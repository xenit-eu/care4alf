package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.jobs.ScheduledQuartzJob;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

@Component
@ScheduledQuartzJob(name = "AuditMetrics", group = Monitoring.SCHEDULE_GROUP, cron = "0 0 1 * * ?", cronProp = "c4a.monitoring.audit.cron")
public class AuditMetric extends AbstractMonitoredSource {
    @Autowired
    private DataSource dataSource;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String,Long> map = new HashMap<>();
        try {
            map.put("audit.entries", query("select count(*) from alf_audit_entry"));
            map.put("audit.properties.value", query("select count(*) from alf_prop_value"));
            map.put("audit.properties.string", query("select count(*) from alf_prop_string_value"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }

    private long query(String query) throws SQLException {
        long r = 0;
        final Connection connection = dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery(query);
            if (rs.next()) {
                r = rs.getLong(1);
            }
            rs.close();
        } finally {
            connection.close();
        }
        return r;
    }
}
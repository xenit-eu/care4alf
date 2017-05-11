package eu.xenit.care4alf.monitoring.metric;

import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.scaling.Scaling;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 12/19/16.
 */
@Component
public class RepositoryMetric extends AbstractMonitoredSource {
    @Autowired
    private Scaling scaling;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String,Long> map = new HashMap<>();
        try {
            map.put("repository.workspace.nodes", (long) scaling.getVar("N1"));
            map.put("repository.archive.nodes", (long) scaling.getVar("N2"));
            map.put("repository.version.nodes", (long) scaling.getVar("N3"));
            map.put("repository.transactions", (long) scaling.getVar("T"));
            map.put("repository.acl", (long) scaling.getVar("A"));
            map.put("repository.acltransactions", (long) scaling.getVar("X"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return map;
    }
}

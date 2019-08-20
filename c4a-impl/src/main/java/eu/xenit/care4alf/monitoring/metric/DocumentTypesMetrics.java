package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.schedule.ScheduledTask;
import eu.xenit.care4alf.Properties;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 4/19/17.
 */
@Component
@ScheduledTask(name = "DocumentTypesMetrics", group = Monitoring.SCHEDULE_GROUP, cron = "0 0 0/1 * * ?",
        cronProp = "c4a.monitoring.documenttypes.cron")//every hour
public class DocumentTypesMetrics extends AbstractMonitoredSource {
    private final Logger logger = LoggerFactory.getLogger(DocumentTypesMetrics.class);

    @Autowired
    private DataSource dataSource;

    @Autowired
    Properties properties;

    @Autowired
    private NamespacePrefixResolver namespacePrefixResolver;

    private Map<Long, Long> getTypesCountRaw() throws SQLException {
        Map<Long, Long> r = new HashMap<>();
        String query = "select type_qname_id, count(*) from alf_node group by type_qname_id";
        final Connection connection = this.dataSource.getConnection();
        try {
            final Statement stmt = connection.createStatement();
            final ResultSet rs = stmt.executeQuery(query);
            while (rs.next()) {
                long qnameId = rs.getLong(1);
                long count = rs.getLong(2);
                r.put(qnameId, count);
            }
            rs.close();
        } finally {
            connection.close();
        }
        return r;
    }

    public Map<QName, Long> getTypesCount() throws SQLException {
        Map<QName, Long> r = new HashMap<>();
        for(Map.Entry<Long, Long> e : this.getTypesCountRaw().entrySet()){
            r.put(
                    this.getDocumentTypes().get(e.getKey()),
                    e.getValue());
        }
        return r;
    }

    public Map<Long, QName> getDocumentTypes() throws SQLException {
        Map<Long, QName> qnames = new HashMap<>();
        for(Properties.QNameInfo q : this.properties.getQNames()){
            qnames.put(q.getId(), q.getQname());
        }
        return qnames;
    }

    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> metrics = new HashMap<>();

        try {
            for(Map.Entry<QName, Long> e: this.getTypesCount().entrySet()){
                String prefix = this.namespacePrefixResolver.getPrefixes(e.getKey().getNamespaceURI()).iterator().next();
                metrics.put("documenttypes."+prefix+"."+e.getKey().getLocalName(), Long.valueOf(e.getValue()));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return metrics;
    }
}
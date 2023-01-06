package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.schedule.ScheduledTask;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;
import org.alfresco.repo.descriptor.DescriptorDAO;
import org.alfresco.service.descriptor.Descriptor;
import org.apache.commons.dbcp2.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 1/17/17.
 */
@Component
@ScheduledTask(name = "DbMetrics", group = Monitoring.SCHEDULE_GROUP, cron = "0 0/5 * * * ?", cronProp = "c4a.monitoring.db.cron")
public class DbMetrics extends AbstractMonitoredSource {

    public static final String PROPS_PREFIX = "eu.xenit.c4a.metrics.";

    @Autowired
    @Qualifier("defaultDataSource")
    private DataSource dataSource;

    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private DescriptorDAO currentRepoDescriptorDAO;

    @Autowired
    @Qualifier("global-properties")
    private Properties globalProps;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> map = new HashMap<>();

        map.put("db.connectionpool.max", (long) ((BasicDataSource)dataSource).getMaxTotal());
        map.put("db.connectionpool.active", (long) ((BasicDataSource)dataSource).getNumActive());
        map.put("db.connectionpool.initial", (long) ((BasicDataSource)dataSource).getInitialSize());

        map.put("db.connectionpool.idle.min", (long) ((BasicDataSource)dataSource).getMinIdle());
        map.put("db.connectionpool.idle.max", (long) ((BasicDataSource)dataSource).getMaxIdle());

        map.put("db.connectionpool.wait.max", ((BasicDataSource)dataSource).getMaxWaitMillis());
        map.put("db.connectionpool.wait.active", (long) ((BasicDataSource)dataSource).getMaxTotal());

        map.put("db.healthy",dbCheck());

        ((BasicDataSource) dataSource).getUrl();

        map.put("db.ping", this.getDBPing());

        return map;
    }

    public long ping(String host) {
        long start = System.currentTimeMillis();
        try {
            if(InetAddress.getByName(host).isReachable(2000))
                return System.currentTimeMillis() - start;
        } catch (IOException e) {
            return -1;
        }
        return -1;
    }

    public long dbCheck() {
        try {
            this.getDescriptor();
        } catch (Exception e) {
            return -1;
        }
        return 1;
    }

    private Descriptor getDescriptor() {
        return currentRepoDescriptorDAO.getDescriptor();
    }

    private long getDBPing(){
        String dbip = globalProps.getProperty(PROPS_PREFIX + "dbip");
        if(dbip == null) {
            URI uri = URI.create(((BasicDataSource) dataSource).getUrl());
            return this.ping(uri.getHost());
        } else {
            return this.ping(dbip);
        }
    }
}
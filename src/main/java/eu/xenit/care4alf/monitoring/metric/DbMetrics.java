package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import org.alfresco.repo.descriptor.DescriptorDAO;
import org.alfresco.service.descriptor.Descriptor;
import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 1/17/17.
 */
@Component
public class DbMetrics extends AbstractMonitoredSource {

    @Autowired
    @Qualifier("defaultDataSource")
    private DataSource dataSource;

    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private DescriptorDAO currentRepoDescriptorDAO;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> map = new HashMap<>();

        map.put("db.connectionpool.max", (long) ((BasicDataSource)dataSource).getMaxActive());
        map.put("db.connectionpool.active", (long) ((BasicDataSource)dataSource).getNumActive());
        map.put("db.connectionpool.initial", (long) ((BasicDataSource)dataSource).getInitialSize());

        map.put("db.connectionpool.idle.min", (long) ((BasicDataSource)dataSource).getMinIdle());
        map.put("db.connectionpool.idle.max", (long) ((BasicDataSource)dataSource).getMaxIdle());

        map.put("db.connectionpool.wait.max", ((BasicDataSource)dataSource).getMaxWait());
        map.put("db.connectionpool.wait.active", (long) ((BasicDataSource)dataSource).getMaxActive());

        map.put("db.healthy",dbCheck());

        ((BasicDataSource) dataSource).getUrl();
        URI uri = URI.create(((BasicDataSource) dataSource).getUrl());
        map.put("db.ping", this.ping(uri.getHost()));

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

}
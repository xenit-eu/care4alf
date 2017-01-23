package eu.xenit.care4alf.monitoring;

import eu.xenit.care4alf.integration.MonitoredSource;
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
public class DbMetrics implements MonitoredSource{

    @Autowired
    @Qualifier("defaultDataSource")
    private DataSource dataSource;

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

}
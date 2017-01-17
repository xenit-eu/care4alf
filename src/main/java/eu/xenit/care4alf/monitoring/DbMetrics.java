package eu.xenit.care4alf.monitoring;

import eu.xenit.care4alf.integration.MonitoredSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.management.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 1/17/17.
 */
@Component
public class DbMetrics implements MonitoredSource{

    @Autowired
    private MBeanServerConnection alfrescoMBeanServer;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> map = new HashMap<>();
        try {
            AttributeList attributes = alfrescoMBeanServer.getAttributes(new ObjectName("Alfresco:Name=ConnectionPool"), new String[]{"NumActive"});
            map.put("db.connectionpool.active", Long.valueOf((Integer) ((Attribute) attributes.get(0)).getValue()));
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        }
        return map;
    }

}
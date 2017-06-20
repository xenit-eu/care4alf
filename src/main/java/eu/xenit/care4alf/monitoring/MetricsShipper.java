package eu.xenit.care4alf.monitoring;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * Created by yregaieg on 07.06.17.
 */
public abstract class MetricsShipper {
    private String serverName;

    @Autowired()
    @Qualifier("global-properties")
    protected java.util.Properties properties;

    public abstract String getName();
    public abstract void send(Map<String, Long> metrics, String serverName);

    @PostConstruct
    public void initServerName(){
        this.serverName = getMonitoringConfigProperty("prefix", this.getName(), "alfresco");
    }

    protected String getServerName(){
        return serverName;
    }

    public void send(Map<String, Long> metrics) {
        this.send(metrics, this.getServerName());
    }

    private String getMonitoringConfigProperty(String key, String shipper, String defaultValue){
        return properties.getProperty("c4a.monitoring."+key, properties.getProperty("c4a.monitoring."+shipper+"."+key, defaultValue));
    }
}

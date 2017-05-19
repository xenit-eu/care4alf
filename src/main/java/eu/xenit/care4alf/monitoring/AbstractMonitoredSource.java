package eu.xenit.care4alf.monitoring;

import eu.xenit.care4alf.integration.MonitoredSource;
import org.springframework.stereotype.Component;

/**
 * Created by willem on 5/10/17.
 */
@Component
public  abstract class AbstractMonitoredSource implements MonitoredSource{
    public String getName(){
        return this.getClass().getSimpleName().toLowerCase().replace("metrics","").replace("metric","");
    }
}

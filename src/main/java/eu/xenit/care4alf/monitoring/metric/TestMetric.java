package eu.xenit.care4alf.monitoring.metric;

import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 5/11/17.
 */
@Component
public class TestMetric extends AbstractMonitoredSource{

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> map = new HashMap<>();
        map.put("test", 123L);
        return map;
    }

}

package eu.xenit.care4alfintegration.monitoring.metrics;

import eu.xenit.care4alf.monitoring.GraphiteClient;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 12/13/16.
 */
@Ignore
public class GraphiteClientIntegrationTest {
    @Test
    public void testSendSingleMetric() {
        GraphiteClient client = new GraphiteClient();
        client.send("junit.test.single.metric", 1234);
    }

    @Test
    public void testSendMultipleMetric() {
        GraphiteClient client = new GraphiteClient();
        Map<String, Long> metrics = new HashMap<>();
        metrics.put("junit.test.multiple.metric1", 1234L);
        metrics.put("junit.test.multiple.metric2", 345L);
        metrics.put("junit.test.multiple.metric3", 1435L);
        metrics.put("junit.test.multiple.metric4", 4564L);
        client.send(metrics);
    }

}
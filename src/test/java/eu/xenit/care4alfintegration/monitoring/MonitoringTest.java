package eu.xenit.care4alfintegration.monitoring;

import eu.xenit.care4alf.Config;
import eu.xenit.care4alf.integration.MonitoredSource;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.GraphiteMetricsShipper;
import eu.xenit.care4alf.monitoring.Monitoring;
import eu.xenit.care4alf.monitoring.metric.SolrSummaryMetrics;
import eu.xenit.care4alf.monitoring.metric.TestMetric;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Thomas.Straetmans on 01/12/2016.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class MonitoringTest {

    @Autowired
    private Monitoring monitoring;

    @Autowired
    GraphiteMetricsShipper shipper;

    @Test
    public void metricCantContainSpaces() throws Exception {
        List<String> keys = new ArrayList<>(monitoring.getAllMetrics().keySet());
        for(String key:keys){
            if(key.contains(" ")){
                Assert.fail("Key contains space: " + key);
            }
        }
    }

    @Test
    public void getServerName() {
        String serverName = shipper.getServerName();
        System.out.println(serverName);
        Assert.assertTrue(serverName != null);
    }

    @Test
    @Ignore
    public void noConflictingNameSpaces() throws Exception {
        List<String> keys = new ArrayList<>(monitoring.getAllMetrics().keySet());
        boolean conflict = false;
        for(String key : keys){
            for(String conflictKey : keys){
                if(key.equals(conflictKey))
                    continue;

                if(conflictKey.contains(key+".")) {
                    conflict=true;
                    System.out.println(String.format("Conflicting key between '%s' and '%s'", key, conflictKey));
                }
            }
        }
        Assert.assertFalse(conflict);
    }

    @Autowired
    private SolrSummaryMetrics solrSummaryMetrics;

    @Test
    public void testName(){
        Assert.assertEquals("solrsummary",this.solrSummaryMetrics.getName());
    }

    @Test
    public void testConflictingMetricNames(){
        List<String> names = new ArrayList<>();
        for(MonitoredSource monitor : monitoring.getAllMonitoredSources()){
            Assert.assertTrue(monitor instanceof AbstractMonitoredSource);
            AbstractMonitoredSource aMonitor = (AbstractMonitoredSource) monitor;
            names.add(aMonitor.getName());
        }
        Assert.assertEquals(names.size(),new HashSet(names).size());
    }

    @Autowired
    private TestMetric testMetric;

    @Autowired
    private Config config;

    @Test
    public void testMetricEnabled(){
        config.removeProperty("c4a.monitoring.metric.test.enabled");
        Assert.assertTrue(monitoring.isEnabled(testMetric));
    }

    @Test
    public void testMetricDisabled(){
        config.addProperty("c4a.monitoring.metric.test.enabled","false");
        Assert.assertFalse(monitoring.isEnabled(testMetric));
    }

}

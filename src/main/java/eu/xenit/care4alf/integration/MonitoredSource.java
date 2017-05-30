package eu.xenit.care4alf.integration;

import java.util.Map;


/**
 * Created by Giovanni on 10/11/16.
 *
 *  Interface for a class which can export monitoring data (key -&gt; metric).
 *
 */
public interface MonitoredSource {

    Map<String, Long> getMonitoringMetrics();

}

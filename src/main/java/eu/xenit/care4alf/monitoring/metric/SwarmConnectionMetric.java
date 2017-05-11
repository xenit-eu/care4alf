package eu.xenit.care4alf.monitoring.metric;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SwarmConnectionMetric extends AbstractMonitoredSource {

	private final Logger logger = LoggerFactory.getLogger(SwarmConnectionMetric.class);
	
	@Autowired
    private ApplicationContext applicationContext;
	
	/*
	 * Even if SwarmStoreAdapter bean implements MonitoredSource, it is not considered implementing the same 
	 * interface as the interface has been loaded by another classloader (outside OSGI container). 
	 * So we have to implement this trick to make it work like other sources.
	 * 
	 * (non-Javadoc)
	 * @see eu.xenit.care4alf.integration.MonitoredSource#getMonitoringMetrics()
	 */
	@Override
	public Map<String, Long> getMonitoringMetrics() {
		try {
			Object obj = applicationContext.getBean("swarmStoreAdapter");
			if (obj == null) return null;
			Method m = obj.getClass().getMethod("getMonitoringMetrics");
			if (m == null) return null;
			@SuppressWarnings("unchecked")
			Map<String, Long> data = new HashMap<>();
			for(Map.Entry<String, Long> entry : ((Map<String, Long>) m.invoke(obj)).entrySet()){
				data.put(entry.getKey().replace(".","-"),entry.getValue());
			}
			return data;
		} catch (Exception e) {
			logger.debug("Error in capturing data.", e);
		}
		return null;
	}

}

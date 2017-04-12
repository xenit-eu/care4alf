package eu.xenit.care4alf.monitoring;

import java.lang.reflect.Method;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import eu.xenit.care4alf.integration.MonitoredSource;

@Component
public class SwarmConnectionMetrics implements MonitoredSource {

	private final Logger logger = LoggerFactory.getLogger(SwarmConnectionMetrics.class);
	
	@Autowired
    private ApplicationContext applicationContext;
	
	@Override
	public Map<String, Long> getMonitoringMetrics() {
		try {
			Object obj = applicationContext.getBean("swarmStoreAdapter");
			if (obj == null) return null;
			Method m = obj.getClass().getMethod("getMonitoringMetrics");
			if (m == null) return null;
			@SuppressWarnings("unchecked")
			Map<String, Long> data = (Map<String, Long>) m.invoke(obj);
			return data;
		} catch (Exception e) {
			logger.error("Error in capturing data.", e);
			System.out.println("error");
		}
		return null;
	}

}

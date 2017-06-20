package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.jobs.ScheduledQuartzJob;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.alfresco.repo.security.authentication.TicketComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 5/3/17.
 */
@Component
@ScheduledQuartzJob(name = "ActiveSessionsMetric", group = Monitoring.SCHEDULE_GROUP, cron = "0 0/1 * * * ?", cronProp = "c4a.monitoring.activesessions.cron")
public class ActiveSessionsMetric extends AbstractMonitoredSource {

    @Autowired
    private TicketComponent ticketComponent;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> map = new HashMap<>();

        map.put("users.tickets", (long) this.ticketComponent.getUsersWithTickets(true).size());

        return map;
    }

}
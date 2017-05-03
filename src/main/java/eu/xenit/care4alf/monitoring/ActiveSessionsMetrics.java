package eu.xenit.care4alf.monitoring;

import eu.xenit.care4alf.integration.MonitoredSource;
import org.alfresco.repo.security.authentication.TicketComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 5/3/17.
 */
@Component
public class ActiveSessionsMetrics implements MonitoredSource {

    @Autowired
    private TicketComponent ticketComponent;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> map = new HashMap<>();

        map.put("users.tickets", (long) this.ticketComponent.getUsersWithTickets(true).size());

        return map;
    }

}
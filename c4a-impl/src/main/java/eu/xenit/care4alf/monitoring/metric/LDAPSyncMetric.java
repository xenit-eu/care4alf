package eu.xenit.care4alf.monitoring.metric;

import com.github.dynamicextensionsalfresco.schedule.ScheduledTask;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.alfresco.repo.security.sync.SyncStatus;
import org.alfresco.service.cmr.attributes.AttributeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by yregaieg on 23.05.17.
 */
@Component
@ScheduledTask(name = "LDAPSyncMetric", group = Monitoring.SCHEDULE_GROUP, cron = "0 0 0/2 * * ?"
        , cronProp = "c4a.monitoring.ldapsync.cron")
public class LDAPSyncMetric extends AbstractMonitoredSource {
    private final Logger logger = LoggerFactory.getLogger(LDAPSyncMetric.class);

    private static final String STATUS_ATTRIBUTE = "STATUS";
    public static final String ROOT_ATTRIBUTE_PATH = ".ChainingUserRegistrySynchronizer";

    @Autowired
    private AttributeService attributeService;

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> metrics = new HashMap<>();
        String statusStr = (String) attributeService.getAttribute(ROOT_ATTRIBUTE_PATH, STATUS_ATTRIBUTE);
        Long status;
        if (SyncStatus.WAITING.toString().equals(statusStr)){
            status = 1L;
        }else if (SyncStatus.IN_PROGRESS.toString().equals(statusStr)){
            status = 2L;
        }else if (SyncStatus.COMPLETE.toString().equals(statusStr)){
            status = 3L;
        }else if (SyncStatus.COMPLETE_ERROR.toString().equals(statusStr)){
            status = -1L;
        }else{
            status = 0L;
        }

        metrics.put("sync.ldap.status", status);
        if (logger.isDebugEnabled()) {
            logger.debug("LDAP sync status : " + status + " - " + statusStr);
        }
        return metrics;
    }
}

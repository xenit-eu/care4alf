package eu.xenit.care4alf.monitoring.metric;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dynamicextensionsalfresco.schedule.ScheduledTask;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.admin.RepoAdminService;
import org.alfresco.service.license.LicenseDescriptor;
import org.alfresco.service.license.LicenseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by willem on 1/19/17.
 */
@Component
@WebScript
@ScheduledTask(name = "LicenseMetric", group = Monitoring.SCHEDULE_GROUP, cron = "0 0/10 * * * ?", cronProp = "c4a.monitoring.license.cron")
public class LicenseMetric extends AbstractMonitoredSource {

    private LicenseService licenseService;
    private RepoAdminService repoAdminService;

    @Autowired
    public LicenseMetric(LicenseService licenseService, RepoAdminService repoAdminService) {
        this.licenseService = licenseService;
        this.repoAdminService = repoAdminService;
    }

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        LicenseInfo licenseInfo = getLicenseInfo();
        Map<String, Long> map = new HashMap<>();
        if (licenseInfo != null) {
            map.put("license.valid", licenseInfo.getRemainingDays());
            map.put("license.users.max", licenseInfo.getMaxUsers());
        } else {
            // No Active License found
            map.put("license.valid", -255L);
            map.put("license.users.max", -255L);
        }
        Long userCount = AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Long>() {
            @Override
            public Long doWork() {
                return repoAdminService.getUsage().getUsers();
            }
        });
        map.put("license.users.authorized", userCount == null ? -1L : userCount);
        return map;
    }

    @Uri(value="/xenit/care4alf/monitoring/license", defaultFormat = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public LicenseInfo getLicenseInfo() {
        if (licenseService == null || licenseService.getLicense() == null) {
            return null;
        } else {
            return new LicenseInfo(licenseService.getLicense());
        }
    }

    public static class LicenseInfo {
        private Long remainingDays;
        private Long maxUsers;
        private String validUntil;
        private String holderOrganisation;
        private boolean clusterEnabled;

        public LicenseInfo(LicenseDescriptor licenseDescriptor) {
            this.holderOrganisation = licenseDescriptor.getHolderOrganisation();
            this.clusterEnabled = licenseDescriptor.isClusterEnabled();
            if (licenseDescriptor.getValidUntil() != null) {
                final SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
                this.validUntil = sdf.format(licenseDescriptor.getValidUntil());
                this.remainingDays = Long.valueOf(licenseDescriptor.getRemainingDays());
            } else {
                // This clause catches a perpetual license.
                // Normal Alfresco behaviour would be to return null for both fields.
                // Now hardcoded to 999999999 to avoid unnecessary alerts and because cabot cannot check on multiple metrics.
                this.validUntil = "perpetual";
                this.remainingDays = 999999999L;
            }
            if (licenseDescriptor.getMaxUsers() != null) {
                this.maxUsers = licenseDescriptor.getMaxUsers();
            } else {
                // This clause catches a license with no maximum users.
                // Normal Alfresco behaviour would be to return null.
                // Now hardcoded to 999999999 to avoid unnecessary alerts and because cabot cannot check on multiple metrics.
                this.maxUsers = 999999999L;
            }
        }

        @JsonProperty("license.holder")
        public String getHolderOrganisation () {
            return holderOrganisation;
        }
        @JsonProperty("cluster")
        public boolean isClusterEnabled () {
            return clusterEnabled;
        }
        @JsonProperty("days")
        public Long getRemainingDays() {
            return remainingDays;
        }
        @JsonProperty("valid.until")
        public String getValidUntil() {
            return validUntil;
        }
        @JsonProperty("users.max")
        public Long getMaxUsers() {
            return maxUsers;
        }
    }
}


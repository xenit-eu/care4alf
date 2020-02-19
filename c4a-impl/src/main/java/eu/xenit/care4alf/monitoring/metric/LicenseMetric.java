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
import java.util.Date;
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
    public LicenseMetric(LicenseService licenseService, RepoAdminService repoAdminService){
        this.licenseService = licenseService;
        this.repoAdminService = repoAdminService;
    }

    @Override
    public Map<String, Long> getMonitoringMetrics() {
        Map<String, Long> map = new HashMap<>();
        if(licenseService == null || licenseService.getLicense() == null)
            map.put("license.valid", -1L);
        else {
            map.put("license.valid", Long.valueOf(licenseService.getLicense().getRemainingDays()));
            if(licenseService.getLicense().getMaxUsers() == null)
                map.put("license.users.max", -1L);
            else
                map.put("license.users.max", licenseService.getLicense().getMaxUsers());
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
    public LicenseInfo getLicenseInfo(){
        if (licenseService == null || licenseService.getLicense() == null){
            return null;
        } else {
            final LicenseDescriptor license = licenseService.getLicense();
            return new LicenseInfo(license.getRemainingDays(), license.getValidUntil(), license.getHolderOrganisation(),
                    license.isClusterEnabled());
        }
    }

    public static class LicenseInfo {
        private Integer remainingDays;
        private String validUntil;
        private String holderOrganisation;
        private boolean clusterEnabled;

        public LicenseInfo(Integer remainingDays, Date validUntil, String holderOrganisation, boolean clusterEnabled){
            if (validUntil != null) {
                final SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
                this.validUntil = sdf.format(validUntil);
                this.remainingDays = remainingDays;
            } else {
                // This clause catches a perpetual license.
                // Normal Alfresco behaviour would be to return null for both fields.
                // Now hardcoded to 9999 to avoid unnecessary alerts.
                this.validUntil = "perpetual";
                this.remainingDays = 9999;
            }
            this.holderOrganisation = holderOrganisation;
            this.clusterEnabled = clusterEnabled;
        }

        @JsonProperty("days")
        public Integer getRemainingDays() {
            return remainingDays;
        }
        @JsonProperty("valid.until")
        public String getValidUntil() {
            return validUntil;
        }
        @JsonProperty("license.holder")
        public String getHolderOrganisation () {
            return holderOrganisation;
        }
        @JsonProperty("cluster")
        public boolean isClusterEnabled () {
            return clusterEnabled;
        }
    }
}


package eu.xenit.care4alf.monitoring.metric;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.github.dynamicextensionsalfresco.schedule.ScheduledTask;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import eu.xenit.care4alf.monitoring.AbstractMonitoredSource;
import eu.xenit.care4alf.monitoring.Monitoring;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.admin.RepoAdminService;
import org.alfresco.service.license.LicenseDescriptor;
import org.alfresco.service.license.LicenseService;
import org.json.JSONException;
import org.json.JSONWriter;
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

//    @Uri(value="/xenit/care4alf/monitoring/license")
//    public Resolution getLicenseInfo(){
//        if(licenseService == null ||licenseService.getLicense() == null)
//            return new JsonWriterResolution() {
//                @Override
//                protected void writeJson(JSONWriter jsonWriter) throws JSONException {
//                    jsonWriter.object();
//                    jsonWriter.endObject();
//                }
//            };
//
//        final LicenseDescriptor license = licenseService.getLicense();
//        final SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
//        return new JsonWriterResolution() {
//            @Override
//            protected void writeJson(JSONWriter jsonWriter) throws JSONException {
//                jsonWriter.object();
//                jsonWriter.key("days").value(license.getRemainingDays());
//                if (license.getValidUntil() != null ) {
//                    jsonWriter.key("valid.until").value(sdf.format(license.getValidUntil()));
//                } else {
//                    jsonWriter.key("valid.until").value(null);
//                }
//                jsonWriter.key("license.holder").value(license.getHolderOrganisation());
//                jsonWriter.key("cluster").value(license.isClusterEnabled());
//                jsonWriter.endObject();
//            }
//        };
//    }

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

    public final class LicenseInfo {
        @JsonProperty("days")
        private Integer remainingDays;
        @JsonProperty("valid.until")
        private String validUntil;
        @JsonProperty("license.holder")
        private String holderOrganisation;
        @JsonProperty("cluster")
        private boolean clusterEnabled;

        private LicenseInfo(Integer remainingDays, Date validUntil, String holderOrganisation, boolean clusterEnabled){
            this.remainingDays = remainingDays;
            if (validUntil != null) {
                final SimpleDateFormat sdf = new SimpleDateFormat("dd-M-yyyy");
                this.validUntil = sdf.format(validUntil);
            } else {
                this.validUntil = null;
            }
            this.holderOrganisation = holderOrganisation;
            this.clusterEnabled = clusterEnabled;
        }

        public Integer getRemainingDays() {
            return remainingDays;
        }

        public String getValidUntil() {
            return validUntil;
        }

        public String getHolderOrganisation () {
            return holderOrganisation;
        }

        public boolean isClusterEnabled () {
            return clusterEnabled;
        }
    }

}


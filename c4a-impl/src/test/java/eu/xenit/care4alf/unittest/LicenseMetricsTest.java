package eu.xenit.care4alf.unittest;


import eu.xenit.care4alf.monitoring.metric.LicenseMetric;
import org.alfresco.service.cmr.admin.RepoAdminService;
import org.alfresco.service.cmr.admin.RepoUsage;
import org.alfresco.service.license.LicenseDescriptor;
import org.alfresco.service.license.LicenseService;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.Test;

import java.util.Map;

public class LicenseMetricsTest {

    @Test
    public void testPerpetualLicense(){
        LicenseService licenseServiceMock = mock(LicenseService.class);
        LicenseDescriptor licenseDescriptorMock = mock(LicenseDescriptor.class);

        when(licenseServiceMock.getLicense()).thenReturn(licenseDescriptorMock);
        when(licenseDescriptorMock.getRemainingDays()).thenReturn(null);
        when(licenseDescriptorMock.getValidUntil()).thenReturn(null);
        when(licenseDescriptorMock.getHolderOrganisation()).thenReturn("TestOrganisation");
        when(licenseDescriptorMock.isClusterEnabled()).thenReturn(true);

        RepoAdminService repoAdminServiceMock = mock(RepoAdminService.class);
        LicenseMetric licenseMetric = new LicenseMetric(licenseServiceMock, repoAdminServiceMock);

        LicenseMetric.LicenseInfo licenseInfo = licenseMetric.getLicenseInfo();

        assert(licenseInfo.getRemainingDays() == 999999999L);
        assert(licenseInfo.getValidUntil() == "perpetual");
        assert(licenseInfo.getHolderOrganisation() == "TestOrganisation");
        assert(licenseInfo.isClusterEnabled() == true);
    }

    @Test
    public void testUnlimitedUserLicense(){
        LicenseService licenseServiceMock = mock(LicenseService.class);
        LicenseDescriptor licenseDescriptorMock = mock(LicenseDescriptor.class);

        when(licenseServiceMock.getLicense()).thenReturn(licenseDescriptorMock);
        when(licenseDescriptorMock.getMaxUsers()).thenReturn(null);

        RepoAdminService repoAdminServiceMock = mock(RepoAdminService.class);
        LicenseMetric licenseMetric = new LicenseMetric(licenseServiceMock, repoAdminServiceMock);

        LicenseMetric.LicenseInfo licenseInfo = licenseMetric.getLicenseInfo();
        assert(licenseInfo.getMaxUsers() == 999999999L);
    }

    @Test
    public void testMissingLicense(){
        LicenseService licenseServiceMock = mock(LicenseService.class);
        when(licenseServiceMock.getLicense()).thenReturn(null);

        RepoAdminService repoAdminServiceMock = mock(RepoAdminService.class);
        RepoUsage repoUsageMock = mock(RepoUsage.class);

        when(repoAdminServiceMock.getUsage()).thenReturn(repoUsageMock);
        when(repoUsageMock.getUsers()).thenReturn(1000L);
        LicenseMetric licenseMetric = new LicenseMetric(licenseServiceMock, repoAdminServiceMock);

        Map<String, Long> metrics = licenseMetric.getMonitoringMetrics();

        assert(metrics.get("license.valid") == -255L);
        assert(metrics.get("license.users.max") == -255L);
        assert(metrics.get("license.users.authorized") == 1000L);
    }

}

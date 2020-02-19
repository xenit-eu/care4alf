package eu.xenit.care4alf.unittest;


import eu.xenit.care4alf.monitoring.metric.LicenseMetric;
import org.alfresco.service.cmr.admin.RepoAdminService;
import org.alfresco.service.license.LicenseDescriptor;
import org.alfresco.service.license.LicenseService;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.Test;


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

        assert(licenseInfo.getRemainingDays() == 9999);
        assert(licenseInfo.getValidUntil() == "perpetual");
        assert(licenseInfo.getHolderOrganisation() == "TestOrganisation");
        assert(licenseInfo.isClusterEnabled() == true);
    }

}

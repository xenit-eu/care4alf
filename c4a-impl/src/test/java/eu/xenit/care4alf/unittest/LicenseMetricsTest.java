package eu.xenit.care4alf.unittest;

import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import eu.xenit.care4alf.dumbster.smtp.Response;
import eu.xenit.care4alf.monitoring.metric.LicenseMetric;
import org.alfresco.service.cmr.admin.RepoAdminService;
import org.alfresco.service.license.LicenseDescriptor;
import org.alfresco.service.license.LicenseService;
import org.json.JSONWriter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.Writer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


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

        System.out.println(licenseInfo.getRemainingDays());
        assert(licenseInfo.getRemainingDays() == null);
        assert(licenseInfo.getValidUntil() == null);
        assert(licenseInfo.getHolderOrganisation() == "TestOrganisation");
        assert(licenseInfo.isClusterEnabled() == true);


    }

}

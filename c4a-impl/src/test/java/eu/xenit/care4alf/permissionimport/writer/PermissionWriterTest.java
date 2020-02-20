package eu.xenit.care4alf.permissionimport.writer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.security.PermissionService;
import org.junit.Test;

public class PermissionWriterTest {

    @Test
    public void testRemovePermissionsOnCompanyHome() {
        // Reproduce ALFREDOPS-433
        Repository repository = mock(Repository.class);
        PermissionService permissionService = mock(PermissionService.class);
        PermissionWriter permissionWriter = new PermissionWriter(repository, null, null, permissionService, null, null);
        NodeRef companyHomeNodeRef = new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, "companyHome");
        when(repository.getCompanyHome()).thenReturn(companyHomeNodeRef);
        permissionWriter.removePermissions(new String[]{});
        verify(permissionService).deletePermissions(companyHomeNodeRef);
    }


}

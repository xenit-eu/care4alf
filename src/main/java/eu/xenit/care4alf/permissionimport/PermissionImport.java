package eu.xenit.care4alf.permissionimport;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import eu.xenit.care4alf.permissionimport.reader.PermissionReader;
import eu.xenit.care4alf.permissionimport.reader.PermissionSetting;
import eu.xenit.care4alf.permissionimport.reader.XlsxPermissionReader;
import eu.xenit.care4alf.permissionimport.writer.PermissionWriter;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.servlet.FormData;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Component
@Authentication(AuthenticationType.ADMIN)
@WebScript(baseUri = "/xenit/care4alf/permissionimport", families = {"care4alf"}, description = "Import Permissions")
public class PermissionImport {

    @Autowired
    private Repository repository;

    @Autowired
    private FileFolderService fileFolderService;

    @Autowired
    private NodeService nodeService;

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private AuthorityService authorityService;

    @Autowired
    private SearchService searchService;


    @Uri(value="importpermissions", method = HttpMethod.POST)
    public void importPermissions(WebScriptRequest request, WebScriptResponse response) throws IOException {
        FormData formData = (FormData) request.parseContent();

        InputStream content = null;
        for (FormData.FormField formField : formData.getFields()) {
            if (formField.getName().equals("file")) {
                content = formField.getInputStream();
            }
        }

        PermissionReader reader = new XlsxPermissionReader(content);
        PermissionWriter writer = new PermissionWriter(repository, fileFolderService, nodeService, permissionService, authorityService, searchService);

        for(PermissionSetting permissionSetting: reader){
            response.getWriter().write("Setting permission:\n"+permissionSetting.toString()+"\n");
            writer.write(permissionSetting);
        }
        response.getWriter().write("done");
    }
}

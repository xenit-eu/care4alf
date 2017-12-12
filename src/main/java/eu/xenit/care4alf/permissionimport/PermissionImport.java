package eu.xenit.care4alf.permissionimport;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AccessPermission;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.PermissionService;
import org.json.JSONException;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.extensions.webscripts.servlet.FormData;
import org.springframework.stereotype.Component;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;

import eu.xenit.care4alf.permissionimport.reader.PermissionReader;
import eu.xenit.care4alf.permissionimport.reader.PermissionSetting;
import eu.xenit.care4alf.permissionimport.reader.XlsxPermissionReader;
import eu.xenit.care4alf.permissionimport.writer.PermissionWriter;

@Component
@Authentication(AuthenticationType.ADMIN)
@WebScript(baseUri = "/xenit/care4alf/permissionimport", families = { "care4alf" }, description = "Import Permissions")
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

    @Uri(value = "importpermissions", method = HttpMethod.POST)
    public void importPermissions(WebScriptRequest request, WebScriptResponse response,
            @RequestParam(required = false, defaultValue = "false") boolean removeFirst) throws IOException {

        FormData formData = (FormData) request.parseContent();

        InputStream content = null;
        for (FormData.FormField formField : formData.getFields()) {
            if (formField.getName().equals("file")) {
                content = formField.getInputStream();
            }
        }

        PermissionReader reader = new XlsxPermissionReader(content);
        PermissionWriter writer = new PermissionWriter(repository, fileFolderService, nodeService, permissionService,
                authorityService, searchService);

        for (PermissionSetting permissionSetting : reader) {
            response.getWriter().write("Setting permission:\n" + permissionSetting.toString() + "\n");
            writer.write(permissionSetting, removeFirst);
        }
        response.getWriter().write("done");
    }

    /**
     * used for integration tests.
     * 
     * @param path
     * @param request
     * @param response
     * @throws IOException
     * @throws JSONException
     */
    @Uri(value = "permissions", method = HttpMethod.GET)
    public void permissions(@RequestParam(required = true) String path, WebScriptRequest request,
            WebScriptResponse response) throws IOException, JSONException {

        // search or create the folder
        NodeRef folderNodeRef = repository.getCompanyHome();
        for (String folder : path.split("\\/")) {
            folderNodeRef = nodeService.getChildByName(folderNodeRef, ContentModel.ASSOC_CONTAINS, folder);
            if (folderNodeRef == null)
                throw new IOException("Path does not exists!");
        }
        Set<AccessPermission> perms = permissionService.getAllSetPermissions(folderNodeRef);

        response.addHeader("Content-Type", "application/json");
        // ObjectMapper mapper = new ObjectMapper();
        // mapper.writeValue(response.getWriter(), perms);

        final JSONWriter jsonRes = new JSONWriter(response.getWriter());
        jsonRes.array();
        for (AccessPermission perm : perms) {
            jsonRes.object();
            jsonRes.key("permission").value(perm.getPermission());
            jsonRes.key("authority").value(perm.getAuthority());
            jsonRes.key("inherited").value(perm.isInherited());
            jsonRes.endObject();
        }
        jsonRes.endArray();

    }

}

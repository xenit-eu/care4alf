package eu.xenit.care4alf.permissionimport.writer;

import eu.xenit.care4alf.AuthorityHelper;
import eu.xenit.care4alf.permissionimport.reader.PermissionSetting;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class PermissionWriter {

    private final Repository repository;
    private final FileFolderService fileFolderService;
    private final NodeService nodeService;
    private final PermissionService permissionService;
    private final AuthorityService authorityService;
    private final AuthorityHelper authorityHelper;

    private static final Logger LOG = LoggerFactory.getLogger(PermissionWriter.class);


    public PermissionWriter(Repository repository, FileFolderService fileFolderService, NodeService nodeService,
                            PermissionService permissionService, AuthorityService authorityService, SearchService searchService){
        this.repository = repository;
        this.fileFolderService = fileFolderService;
        this.nodeService = nodeService;
        this.permissionService = permissionService;
        this.authorityService = authorityService;
        this.authorityHelper = new AuthorityHelper(searchService);
    }

    public void write(PermissionSetting permissionSetting){
        if(permissionSetting.getPath() == null){
            LOG.error("The path is null");
            return;
        }

        LOG.debug("Path for permission: "+ Arrays.toString(permissionSetting.getPath()));

        NodeRef parent = repository.getCompanyHome();

        //search or create the folder
        for (String folder: permissionSetting.getPath()) {
            NodeRef child = nodeService.getChildByName(parent, ContentModel.ASSOC_CONTAINS, folder);
            if(child == null){
                LOG.debug("Creating folder: "+folder);
                child = fileFolderService.create(parent, folder, ContentModel.TYPE_FOLDER).getNodeRef();
            }
            parent = child;
        }

        LOG.debug("Inherit: "+permissionSetting.isInherit());
        permissionService.setInheritParentPermissions(parent, permissionSetting.isInherit());


        if(permissionSetting.getGroup() != null && permissionSetting.getPermission() != null){
            String group = "GROUP_"+permissionSetting.getGroup();

            List<NodeRef> authorityNodeRefs = authorityHelper.getNodeGroupNodeRefs(permissionSetting.getGroup());
//            Set<String> authorities = authorityService.findAuthorities(AuthorityType.GROUP, null, false, permissionSetting.getGroup(), null);
            if(authorityNodeRefs.isEmpty()){
                LOG.error("No group found: "+permissionSetting.getGroup());
            } else {
                LOG.debug("Setting permission: "+group+" "+permissionSetting.getPermission());
                permissionService.setPermission(parent, group, permissionSetting.getPermission(), true);
            }
        }

    }

}

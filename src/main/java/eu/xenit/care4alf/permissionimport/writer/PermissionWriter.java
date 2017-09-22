package eu.xenit.care4alf.permissionimport.writer;

import eu.xenit.care4alf.permissionimport.reader.PermissionSetting;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthorityService;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.PermissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Set;

public class PermissionWriter {

    private Repository repository;
    private FileFolderService fileFolderService;
    private NodeService nodeService;
    private PermissionService permissionService;
    private AuthorityService authorityService;

    private static final Logger LOG = LoggerFactory.getLogger(PermissionWriter.class);


    public PermissionWriter(Repository repository, FileFolderService fileFolderService, NodeService nodeService,
                            PermissionService permissionService, AuthorityService authorityService){
        this.repository = repository;
        this.fileFolderService = fileFolderService;
        this.nodeService = nodeService;
        this.permissionService = permissionService;
        this.authorityService = authorityService;
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

            Set<String> authorities = authorityService.findAuthorities(AuthorityType.GROUP, null, false, permissionSetting.getGroup(), null);
            if(authorities.isEmpty()){
                LOG.error("No group found: "+permissionSetting.getGroup());
            } else {
                LOG.debug("Setting permission: "+group+" "+permissionSetting.getPermission());
                permissionService.setPermission(parent, group, permissionSetting.getPermission(), true);
            }
        }

    }

}

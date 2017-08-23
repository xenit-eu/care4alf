package eu.xenit.care4alf.helpers;

/**
 * Created by paul on 23/11/2016.
**/

import org.alfresco.model.ContentModel;
import org.alfresco.repo.nodelocator.CompanyHomeNodeLocator;
import org.alfresco.repo.nodelocator.XPathNodeLocator;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.workflow.WorkflowException;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class NodeHelper {

    private final static Logger logger = LoggerFactory.getLogger(NodeHelper.class);

    private static final Pattern SPECIAL_CHARS_PATTERN = Pattern.compile("[*\"<>\\\\/.?:|]+");

    @Autowired
    private ServiceRegistry services;

    private NodeRef companyHomeRef;

    private NodeRef dataDictionaryRef;

    public NodeRef getCompanyHome() {
        if (companyHomeRef == null) {
            companyHomeRef = services.getNodeLocatorService().getNode(CompanyHomeNodeLocator.NAME, null, null);
        }
        return companyHomeRef;
    }

    public NodeRef getDataDictionary() {
        if (dataDictionaryRef == null) {
            dataDictionaryRef = getCompanyHomeChildFolder("Data Dictionary");
        }
        return dataDictionaryRef;
    }

    public NodeRef getCompanyHomeChildFolder(String name) {
        return getChild(getCompanyHome(), name);
    }

    public NodeRef getNodeRef(String xpath) {
        return getNodeRef(null, xpath);
    }

    public NodeRef getNodeRef(NodeRef source, String xpath) {
        final Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(XPathNodeLocator.QUERY_KEY, xpath);
        return services.getNodeLocatorService().getNode(XPathNodeLocator.NAME, source, parameters);
    }

    public NodeRef createDocument(NodeRef parentRef, String name) {
        return createDocument(parentRef, name, new HashMap<QName, Serializable>());
    }

    public NodeRef createDocument(NodeRef parentRef, String name, Map<QName, Serializable> properties) {
        final QName qname = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name);
        properties.put(ContentModel.PROP_NAME, name);
        return services.getNodeService().createNode(parentRef, ContentModel.ASSOC_CONTAINS, qname, ContentModel.TYPE_CONTENT, properties).getChildRef();
    }

    public NodeRef createFolder(NodeRef parentRef, String name) {
        return createFolder(parentRef, name, ContentModel.TYPE_FOLDER);
    }

    public NodeRef createFolder(final NodeRef parentRef, final String name, final QName folderType) {
        return (NodeRef) AuthenticationUtil.runAsSystem(new AuthenticationUtil.RunAsWork<Object>() {
            @Override
            public Object doWork() throws Exception {
                try {
                    QName qname = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, name);
                    final Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
                    properties.put(ContentModel.PROP_NAME, name);
                    logger.debug("Creating folder with name: {}", name);
                    return services.getNodeService().createNode(parentRef, ContentModel.ASSOC_CONTAINS, qname, folderType, properties).getChildRef();
                } catch (Exception e) {
                    logger.error("Exception found!");
                    logger.debug(e.getMessage());
                    throw new WorkflowException("Error found.");
                }
            }
        });
    }

    public NodeRef createFolderIfNotExists(NodeRef parentRef, String name) {
        return createFolderIfNotExists(parentRef, name, ContentModel.TYPE_FOLDER);
    }

    public NodeRef createFolderIfNotExists(NodeRef parentRef, String name, QName folderType) {
        final NodeRef folder = getChild(parentRef, name);
        return folder == null ? createFolder(parentRef, name, folderType) : folder;
    }

    public List<NodeRef> getAllSubFoldersByType(QName type, NodeRef contextRef) {
        List<NodeRef> noderefs = new ArrayList<NodeRef>();
        for (FileInfo info : services.getFileFolderService().listFolders(contextRef)) {
            if (info.getType().equals(type)) {
                noderefs.add(info.getNodeRef());
            }
            List<NodeRef> lookdownRefs = getAllSubFoldersByType(type, info.getNodeRef());
            if (!lookdownRefs.isEmpty()) {
                noderefs.addAll(lookdownRefs);
            }
        }
        return noderefs;
    }

    public NodeRef getSubFolderByName(NodeRef parentRef, String name) {
        for (FileInfo info : services.getFileFolderService().listFolders(parentRef)) {
            if (info.getName().equals(name)) {
                return info.getNodeRef();
            }
            NodeRef lookdownRef = getSubFolderByName(info.getNodeRef(), name);
            if (lookdownRef != null) {
                return lookdownRef;
            }
        }
        return null;
    }

    public NodeRef getChild(NodeRef parentRef, String name) {
        return services.getNodeService().getChildByName(parentRef, ContentModel.ASSOC_CONTAINS, name);
    }

    public List<NodeRef> getAllChildNodes(NodeRef parentNode) {
        List<NodeRef> childNodes = new ArrayList<NodeRef>();

        List<ChildAssociationRef> childAssociations = services.getNodeService().getChildAssocs(parentNode);
        for(ChildAssociationRef childAssoc : childAssociations) {
            childNodes.add(childAssoc.getChildRef());
        }

        return childNodes;
    }

    public NodeRef getPrimaryParent(NodeRef childRef) {
        ChildAssociationRef childAssoc =  services.getNodeService().getPrimaryParent(childRef);
        return childAssoc.getParentRef();
    }

    public boolean containsChild(NodeRef parentRef, String name) {
        return getChild(parentRef, name) != null;
    }

    public String getStringProperty(NodeRef nodeRef, QName property) {
        return (String) services.getNodeService().getProperty(nodeRef, property);
    }

    public String getName(NodeRef nodeRef) {
        return getStringProperty(nodeRef, ContentModel.PROP_NAME);
    }

    public List<String> getListProperty(NodeRef nodeRef, QName property) {
        return (List<String>) services.getNodeService().getProperty(nodeRef, property);
    }

    public NodeRef getNodeRefProperty(NodeRef nodeRef, QName property) {
        return (NodeRef) services.getNodeService().getProperty(nodeRef, property);
    }

    public List<NodeRef> getNodeRefListProperty(NodeRef nodeRef, QName property) {
        return (List<NodeRef>) services.getNodeService().getProperty(nodeRef, property);
    }

    public String getCategoryProperty(NodeRef nodeRef, QName property) {
        return categoryValueToString(services.getNodeService().getProperty(nodeRef, property));
    }

    public String categoryValueToString(Serializable categoryValue) {
        if (categoryValue instanceof List) {
            logger.info("Category value of type list");
            List<NodeRef> nodes = (List<NodeRef>) categoryValue;
            String values = "";
            for (NodeRef nodeRef : nodes) {
                values += "," + services.getFileFolderService().getFileInfo(nodeRef).getName();
            }
            return values.isEmpty() ? "" : values.substring(1);
        } else if (isNodeRef(categoryValue)) {
            logger.info("Category value of type NodeRef");
            return services.getFileFolderService().getFileInfo((NodeRef) categoryValue).getName();
        } else {
            logger.debug("category value of no type");
            return "";
        }
    }

    public boolean isNodeRef(Serializable nodeRef) {
        return NodeRef.isNodeRef(nodeRef.toString());
    }

    public int getListSize(Serializable nodeRef) {
        String string = "" + nodeRef;
        return string.split(",").length;
    }

    /**
     * This method checks if String contains disallowed characters: * " &lt; &gt; \ / . ? : and |.
     *
     * Can be used for checking the name property when creating content.
     *
     * @param text Phrase to check
     *
     * @return true if contains * " &lt; &gt; \ / . ? : or |
     */
    public boolean containsIntegrityViolationCharacters(String text){
        if (text!=null && SPECIAL_CHARS_PATTERN.matcher(text).find()){
            return true;
        }
        return false;
    }

}

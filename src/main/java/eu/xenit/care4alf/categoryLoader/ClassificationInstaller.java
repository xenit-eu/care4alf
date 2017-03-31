package eu.xenit.care4alf.categoryLoader;

/**
 * Created by Thomas.Straetmans on 25/11/2016.
 */

import org.alfresco.model.ContentModel;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component
public class ClassificationInstaller {
    private final static Logger logger = LoggerFactory.getLogger(ClassificationInstaller.class);

    public static final String CLASSIFICATION_PATH = "/META-INF/alfresco/config/";
    public static final String CLASSIFICATION_FORMAT = ".json";

    private NodeRef categoryRootRef = null;

    @Autowired
    private ServiceRegistry services;



    public NodeRef getCategoryRootRef() {
        if (categoryRootRef == null) {
            categoryRootRef = services.getSearchService().query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, "/cm:categoryRoot").getNodeRef(0);
        }
        return categoryRootRef;
    }

    public void create(String classificationName, String targetNamespace, String jsonString, boolean forceReplace) {
        logger.debug("Starting classification creation for {}", classificationName);
        try {
            final JSONObject json = new JSONObject(jsonString);
            logger.debug("JSON: {}", json);
            addClassification(json, forceReplace, targetNamespace);
            logger.info("Classification creation done for {}", classificationName);
        } catch (Exception e) {
            logger.error("Exception creating classification: {}", e.getMessage());
            logger.info("Classification creation failed for {}", classificationName);
        }

    }

    public void addClassification(@Nonnull JSONObject jsonObject, boolean forceReplace, String targetNamespace) throws JSONException {
        logger.debug("Target namespace: {}", targetNamespace);
        String targetLocalName = jsonObject.getJSONObject("target").getString("localname");
        logger.debug("Target name: {}", targetLocalName);
        QName categoryAspectQName = QName.createQName(targetNamespace, targetLocalName);
        NodeRef categoryAspectRef = createClassificationRoot(categoryAspectQName, forceReplace);
        addAspectCategories(categoryAspectRef, jsonObject.getJSONObject("classification"));
    }

    private NodeRef createClassificationRoot(QName categoryAspectQName, boolean forceReplace){
        NodeRef classificationRoot = classificationExists(categoryAspectQName);
        if (classificationRoot != null) {
            if (forceReplace) {
                removeClassificationIfExists(categoryAspectQName);
            } else {
                logger.debug("Classification root already exists, skipping creation.");
                return classificationRoot;
            }
        }
        return createClassification(categoryAspectQName);
    }

    private NodeRef createClassification(QName categoryAspectQName) {
        logger.debug("Creating classification for aspect: {}", categoryAspectQName);
        Map<QName, Serializable> props = new HashMap<QName, Serializable>();
        props.put(ContentModel.PROP_NAME, categoryAspectQName.getLocalName());
        return services.getNodeService().createNode(getCategoryRootRef(), ContentModel.ASSOC_CATEGORIES, categoryAspectQName, ContentModel.TYPE_CATEGORY, props).getChildRef();
    }

    private void addAspectCategories(@Nonnull NodeRef parentRef, @Nonnull JSONObject json) throws JSONException {
        final Iterator<String> it = json.keys();
        while (it.hasNext()) {
            String key = it.next();
            String validname = createValidName(key);
            NodeRef childRef = categoryExists(validname, parentRef);
            if(childRef == null) {
                childRef = services.getCategoryService().createCategory(parentRef, validname);
            }
            if (!json.isNull(key)) {
                addAspectCategories(childRef, json.getJSONObject(key));
            }
        }
    }

    private NodeRef categoryExists(String validname, NodeRef parentRef) {
        services.getNodeService().getChildByName(parentRef, ContentModel.ASSOC_SUBCATEGORIES, validname);
        return services.getSearchService().query(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_XPATH, "/cm:categoryRoot").getNodeRef(0);
    }

    private String createValidName(String name) {
        String validName = QName.createValidLocalName(name);
        validName = validName.replaceAll("/", "-").replaceAll("\"", "-").replaceAll(" :", " --").replaceAll(":", " --");
        return validName.endsWith(".") ? validName.substring(0, validName.length() - 1) : validName;
    }

    public NodeRef classificationExists(QName categoryAspectQName) {
        for (ChildAssociationRef childAssociationRef : services.getNodeService().getChildAssocs(getCategoryRootRef())) {
            if (childAssociationRef.getQName().equals(categoryAspectQName)) {
                return childAssociationRef.getChildRef();
            }
        }
        return null;
    }

    public boolean removeClassificationIfExists(QName categoryAspectQName) {
        for (ChildAssociationRef childAssociationRef : services.getNodeService().getChildAssocs(getCategoryRootRef())) {
            if (childAssociationRef.getQName().equals(categoryAspectQName)) {
                logger.debug("Removing classification for aspect: {}", categoryAspectQName.getLocalName());
                services.getNodeService().removeChildAssociation(childAssociationRef);
                return true;
            }
        }
        return false;
    }

}

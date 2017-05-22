package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import eu.xenit.care4alf.helpers.UtilHelper;
import org.alfresco.model.ContentModel;
import org.alfresco.query.PagingRequest;
import org.alfresco.repo.domain.node.ContentDataWithId;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.json.JSONException;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.*;

/**
 * Created by Thomas.Straetmans on 10/05/2017.
 */

@Component
@WebScript(baseUri = "/xenit/care4alf/contentstore", families = "care4alf", description = "Content store verification")
@Authentication(AuthenticationType.ADMIN)
public class ContentStore {

    @Autowired
    ServiceRegistry services;

    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private ContentService contentService;

    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private NodeService nodeService;

    final private Logger logger = LoggerFactory.getLogger(ContentStore.class);

    private JdbcTemplate jdbcTemplate;// = new JdbcTemplate(dataSource);

    private QName WorkspaceDiskUsage = QName.createQName("WorkspaceDiskUsage");
    private QName ArchiveDiskUsage = QName.createQName("ArchiveDiskUsage");

    @Autowired
    public ContentStore(DataSource dataSource) {
        jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Uri(value = "/diskusagebyowner")
    @Transaction(readOnly = true)
    public Resolution list(final WebScriptResponse response) {
        List<PersonService.PersonInfo> people = services.getPersonService().getPeople(null, null, null, new PagingRequest(Integer.MAX_VALUE, null)).getPage();
        List<NodeRef> peopleref = new ArrayList<>(people.size());
        logger.error("# of pll found: {}", people.size());
        for (PersonService.PersonInfo info : people) {
            peopleref.add(info.getNodeRef());
        }
        final ArrayList<Map<String, Object>> responselist = new ArrayList();
        for (NodeRef ref : peopleref) {

            Long workspace = (Long) services.getNodeService().getProperty(ref, WorkspaceDiskUsage);
            Long archive = (Long) services.getNodeService().getProperty(ref, ArchiveDiskUsage);
            logger.error("For {} we have found archive: {}, workspace: {}", ref, archive, workspace);
            if (workspace != null || archive != null) {
                Map<String, Object> responsemap = new HashMap<>();
                responsemap.put("username", services.getNodeService().getProperty(ref, ContentModel.PROP_USERNAME));
                responsemap.put("workspace", workspace);
                responsemap.put("archive", archive);
                responselist.add(responsemap);
            }
        }

        return new JsonWriterResolution() {
            @Override
            protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                jsonWriter.array();
                for (Map<String, Object> map : responselist) {
                    jsonWriter.object();
                    for (Map.Entry<String, Object> entry : map.entrySet()) {
                        jsonWriter.key(entry.getKey()).value(entry.getValue());
                    }
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            }
        };
    }

    @Uri(value = "/updatediskusage", method = HttpMethod.PUT)
    public void update() {
        AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<Object>() {

            @Override
            public Void doWork() throws Exception {
                logger.error("Update diskusage started");
                Calendar timestart = Calendar.getInstance();
                HashMap<String, Long> workspace = new HashMap<>();
                HashMap<String, Long> archive = new HashMap<>();
                List<Long> ids = jdbcTemplate.queryForList("select id from alf_node", Long.class);
                for (Long id : ids) {
                    NodeRef ref = services.getNodeService().getNodeRef(id);
                    if (ref != null) {
                        ContentDataWithId data = (ContentDataWithId) services.getNodeService().getProperty(ref, ContentModel.PROP_CONTENT);
                        Object ownerobj = services.getNodeService().getProperty(ref, ContentModel.PROP_CREATOR);
                        String owner = (ownerobj == null ? null : ownerobj.toString());
                        if (data != null && owner != null) {
                            if (ref.getStoreRef().getProtocol().equals("archive")) {
                                archive.put(owner, (Long) UtilHelper.getOrElse(archive, owner, 0L) + data.getSize());
                            } else {
                                workspace.put(owner, (Long) UtilHelper.getOrElse(workspace, owner, 0L) + data.getSize());
                            }
                        }
                    }
                }

                for (Map.Entry<String, Long> entry : workspace.entrySet()) {
                    NodeRef person = services.getPersonService().getPerson(entry.getKey());
                    services.getNodeService().setProperty(person, WorkspaceDiskUsage, entry.getValue());
                }

                for (Map.Entry<String, Long> entry : archive.entrySet()) {
                    NodeRef person = services.getPersonService().getPerson(entry.getKey());
                    services.getNodeService().setProperty(person, ArchiveDiskUsage, entry.getValue());
                }
                Calendar timeend = Calendar.getInstance();
                logger.error("Update diskusage has finished. Time taken: {} s", (timeend.getTimeInMillis() - timestart.getTimeInMillis()) / 1000);
                return null;
            }
        }, AuthenticationUtil.getSystemUserName());
    }

    @Uri(value = "/checkintegrity", method = HttpMethod.GET)
    public Resolution checkintegrity() {
        return new JsonWriterResolution() {
            @Override
            protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                jsonWriter.array();
                for (MissingContent content : getIntegrityCheckResults()) {
                    jsonWriter.object();
                    jsonWriter.key("noderef").value(content.getNodeRef());
                    jsonWriter.key("contentUrl").value(content.getContentUrl());
                    jsonWriter.key("cause").value(content.getCause());
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            }
        };
    }

    public List<MissingContent> getIntegrityCheckResults(){
        final ArrayList<MissingContent> missingContent = new ArrayList<>();
        List<Long> ids = jdbcTemplate.queryForList("select id from alf_node", Long.class);
        for (Long id : ids) {
            NodeRef nodeRef = nodeService.getNodeRef(id);
            if (nodeRef != null) {
                ContentDataWithId content = (ContentDataWithId) services.getNodeService().getProperty(nodeRef, ContentModel.PROP_CONTENT);
                if (content != null) {
                    try {
                        contentService.getReader(nodeRef, ContentModel.PROP_CONTENT).getContentInputStream().close();
                    } catch (Exception ex) {
                        String contentUrl = "<none>";
                        if (content != null && content.getContentUrl() != null) {
                            contentUrl = content.getContentUrl();
                        }
                        missingContent.add(new MissingContent(nodeRef, contentUrl, ex.getMessage()));
                    }
                }
            }
        }
        return missingContent;
    }
}

class MissingContent {
    private NodeRef nodeRef;
    private String contentUrl;
    private String cause;

    public MissingContent(NodeRef nodeRef, String contentUrl, String cause) {
        this.nodeRef = nodeRef;
        this.contentUrl = contentUrl;
        this.cause = cause;
    }

    public NodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(NodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public String getContentUrl() {
        return contentUrl;
    }

    public void setContentUrl(String contentUrl) {
        this.contentUrl = contentUrl;
    }

    public String getCause() {
        return cause;
    }

    public void setCause(String cause) {
        this.cause = cause;
    }
}

package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Transaction;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.VersionNumber;
import org.apache.commons.lang.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@WebScript(baseUri = "/xenit/care4alf/amps", families = {"care4alf"}, description = "Update AMP module versions")
@Authentication(AuthenticationType.ADMIN)
public class Amps {
    @Autowired
    private DataSource dataSource;

    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private NodeService nodeService;

    @Uri(value = "/")
    @Transaction(readOnly = true)
    public Resolution list() {
        return new JsonWriterResolution() {
            @Override
            protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                List<NodeRef> ampRefs = getAmps();
                jsonWriter.array();
                for (NodeRef nodeRef : ampRefs) {
                    jsonWriter.object();
                    Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
                    for (Map.Entry<QName, Serializable> property : properties.entrySet()) {
                        jsonWriter.key(property.getKey().toString()).value(property.getValue());
                    }
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            }
        };
    }

    @Uri(value = "/", method = HttpMethod.POST)
    public void save(JSONObject json) throws JSONException {
        long dbid = json.getLong("{http://www.alfresco.org/model/system/1.0}node-dbid");
        String currentVersion = json.getString("{http://www.alfresco.org/system/modules/1.0}currentVersion");
        String installedVersion = json.getString("{http://www.alfresco.org/system/modules/1.0}installedVersion");
        NodeRef nodeRef = this.nodeService.getNodeRef(dbid);
        this.nodeService.setProperty(nodeRef, QName.createQName("{http://www.alfresco.org/system/modules/1.0}currentVersion"), (new VersionNumber(currentVersion)));
        this.nodeService.setProperty(nodeRef, QName.createQName("{http://www.alfresco.org/system/modules/1.0}installedVersion"), (new VersionNumber(installedVersion)));
    }

    @Uri(value = "/", method = HttpMethod.DELETE)
    public void clear() {
        List<NodeRef> nodeRefs = this.getAmps();
        for (NodeRef nodeRef : nodeRefs) {
            this.nodeService.deleteNode(nodeRef);
        }
    }

    public List<NodeRef> getAmps() {
        String versionIds = StringUtils.join(this.getVersionQnameIds(), ", ");
        List<Long> ids = new JdbcTemplate(this.dataSource).queryForList(
                "select distinct(node_id) from alf_node_properties where qname_id in (" + versionIds + ')',
                Long.class);
        List<NodeRef> nodeRefs = new ArrayList<>();
        for (Long id : ids) {
            nodeRefs.add(nodeService.getNodeRef(id));
        }
        return nodeRefs;
    }

    private final List getVersionQnameIds() {
        String query = "SELECT id FROM alf_qname WHERE local_name IN ('currentVersion','installedVersion')";
        return new JdbcTemplate(this.dataSource).queryForList(query,String.class);
    }

}
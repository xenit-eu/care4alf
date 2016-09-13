package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import eu.xenit.care4alf.search.SolrAdmin;
import org.alfresco.repo.descriptor.DescriptorDAO;
import org.alfresco.repo.dictionary.DictionaryDAO;
import org.alfresco.service.cmr.dictionary.ModelDefinition;
import org.alfresco.service.cmr.dictionary.NamespaceDefinition;
import org.alfresco.service.descriptor.Descriptor;
import org.alfresco.service.namespace.QName;
import org.apache.commons.codec.EncoderException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.CRC32;

/**
 * Created by willem on 3/7/16.
 */
@Component
@WebScript(families = "care4alf", description = "Monitoring")
@Authentication(AuthenticationType.NONE)
public class Monitoring {

    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private DescriptorDAO currentRepoDescriptorDAO;

    @Autowired
    private SolrAdmin solrAdmin;

    @Autowired
    private DictionaryDAO dictionaryDAO;

    @Autowired
    private Properties properties;

    @Autowired
    private Clustering clustering;

    private final Logger logger = LoggerFactory.getLogger(Monitoring.class);


    @Uri("/xenit/care4alf/monitoring")
    public void monitoring(final WebScriptResponse res) throws IOException, JSONException {
        Descriptor descriptor = getDescriptor();
        JSONObject obj = new JSONObject();
        final JSONWriter jsonRes = new JSONWriter(res.getWriter());
        jsonRes.object();
        jsonRes.key("data");
        jsonRes.object();
        jsonRes.key("edition").value(descriptor.getLicenseMode());
        jsonRes.key("version").value(descriptor.getVersion());
        jsonRes.key("schema").value(descriptor.getSchema());
        jsonRes.endObject();
        jsonRes.endObject();
    }

    private long dbCheck() {
        try {
            this.getDescriptor();
        } catch (Exception e) {
            return -1;
        }
        return 1;
    }

    private Descriptor getDescriptor() {
        return currentRepoDescriptorDAO.getDescriptor();
    }

    @Uri("/xenit/care4alf/monitoring/db")
    public void getDbStatus(WebScriptResponse res) throws IOException {
        dbCheck();
        res.getWriter().write("OK");
    }

    @Uri(value = "/xenit/care4alf/monitoring/solr/errors", defaultFormat = "text")
    public void getSolrErrors(WebScriptResponse res) throws IOException, JSONException, EncoderException {
        res.getWriter().write(Long.toString(this.solrAdmin.getSolrErrors()));
    }

    @Uri(value = "/xenit/care4alf/monitoring/solr/lag", defaultFormat = "text")
    public void getSolrLags(WebScriptResponse res) throws IOException, JSONException, EncoderException {
        res.getWriter().write(String.valueOf(this.solrAdmin.getSolrLag()));
    }

    private long getResidualProperties(String filter) {
        try {
            return this.properties.getResidualProperties(filter).size();
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    @Uri(value = "/xenit/care4alf/monitoring/vars")
    public Resolution getVars(WebScriptResponse response) throws IOException, JSONException, EncoderException {
        final Map<String, Long> vars = new HashMap<String, Long>();
        logger.debug("Starting vars");
        vars.put("db", this.dbCheck());
        logger.debug("db done");
        vars.put("solr.errors", this.solrAdmin.getSolrErrors());
        logger.debug("solr.errors done");
        vars.put("solr.lag", this.solrAdmin.getSolrLag());
        logger.debug("solr.lag done");
        vars.put("solr.lag.nodes", this.solrAdmin.getNodesToIndex());
        logger.debug("solr.lag.nodes done");
        vars.put("solr.model.errors", this.solrAdmin.getModelErrors());
        logger.debug("solr.model.errors done");
        vars.put("properties.residual", this.getResidualProperties("alfresco"));
        logger.debug("solr.properties.residual done");
        vars.put("cluster.nodes", (long) this.clustering.getNumClusterMembers());
        logger.debug("cluster.nodes done");

        return new JsonWriterResolution() {
            protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                jsonWriter.object();
                for (Map.Entry<String, Long> entry : vars.entrySet())
                    jsonWriter.key(entry.getKey()).value(entry.getValue());
                jsonWriter.endObject();
            }
        };
    }

    private long getChecksum(QName modelQName, ModelDefinition modelDefinition) {
        final CRC32 crc = new CRC32();
        final OutputStream stream = new OutputStream() {
            public void write(int b) throws IOException {
                crc.update(b);
            }
        };

        modelDefinition.toXML(ModelDefinition.XMLBindingType.DEFAULT, stream);

        return crc.getValue();
    }

}

package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.zip.CRC32;

/**
 * Created by willem on 3/7/16.
 */
@Component
@WebScript(description = "Monitoring")
@Authentication(AuthenticationType.NONE)
public class Monitoring {

    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    private DescriptorDAO currentRepoDescriptorDAO;

    @Autowired
    private SolrAdmin solrAdmin;

    @Autowired
    private DictionaryDAO dictionaryDAO;

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

    private void dbCheck(){
        this.getDescriptor();//uses nodeservice and searchservice
    }

    private Descriptor getDescriptor(){
        return currentRepoDescriptorDAO.getDescriptor();
    }

    @Uri("/xenit/care4alf/monitoring/db")
    public void getDbStatus(WebScriptResponse res) throws IOException {
        dbCheck();
        res.getWriter().write("OK");
    }

    @Uri(value="/xenit/care4alf/monitoring/solr/errors",defaultFormat = "text")
    public void getSolrErrors(WebScriptResponse res) throws IOException, JSONException, EncoderException {
        res.getWriter().write(Integer.toString(this.solrAdmin.getSolrErrors()));
    }

    @Uri(value="/xenit/care4alf/monitoring/cluster/dictionary",defaultFormat = "text")
    public void getDictionaryChecksums(WebScriptResponse response) throws IOException, JSONException, EncoderException {
        //TODO: compare 2 cluster nodes
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.object();
        final Collection<QName> models = dictionaryDAO.getModels();
        for (QName modelName : models) {
            final ModelDefinition modelDefinition = dictionaryDAO.getModel(modelName);
            final Collection<NamespaceDefinition> namespaces = modelDefinition.getNamespaces();
            for (NamespaceDefinition namespace : namespaces) {
                json
                        .key(namespace.getUri())
                        .value(getChecksum(modelName, modelDefinition));
            }
        }
        json.endObject();
    }

    private long getChecksum(QName modelQName, ModelDefinition modelDefinition)
    {
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
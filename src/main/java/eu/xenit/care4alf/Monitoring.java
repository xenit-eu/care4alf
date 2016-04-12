package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.annotations.AlfrescoService;
import com.github.dynamicextensionsalfresco.annotations.ServiceType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import org.alfresco.repo.descriptor.DescriptorDAO;
import org.alfresco.service.descriptor.Descriptor;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by willem on 3/7/16.
 */
@Component
@WebScript(description = "Monitoring")
@Authentication(AuthenticationType.NONE)
public class Monitoring {

    @Autowired
    @AlfrescoService(ServiceType.LOW_LEVEL)
    DescriptorDAO currentRepoDescriptorDAO;

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

}
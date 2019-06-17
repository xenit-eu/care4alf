package eu.xenit.care4alf.integrity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.alfresco.service.cmr.repository.NodeRef;
import org.json.JSONException;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@WebScript(baseUri = "/xenit/care4alf/integrity", families = "care4alf", description = "IntegrityScanner Verification")
@Authentication(AuthenticationType.ADMIN)
public class Integrity {
    @Autowired
    IntegrityScanner integrityScanner;

    @Uri(value = "/report", method = HttpMethod.GET)
    public void report(final WebScriptResponse response) throws IOException {
        writeReportAsResponse(integrityScanner.getLastReport(), response);
    }

    private static class SubsetBody { public List<String> nodes; public List<String> files; public SubsetBody() {} }

    @Uri(value="/subset", method = HttpMethod.POST)
    public void scanSubset(@RequestBody SubsetBody body, WebScriptResponse response) throws IOException {
        List<NodeRef> convertedNodes = new ArrayList<>(body.nodes.size());
        for (String node : body.nodes) {
            convertedNodes.add(new NodeRef(node));
        }
        writeReportAsResponse(integrityScanner.scanSubset(convertedNodes.iterator(), body.files), response);
    }

    @Uri(value = "/progress", method = HttpMethod.GET)
    public void progress(final WebScriptResponse response) throws IOException, JSONException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.object();
        json.key("nodeProgress").value(integrityScanner.getNodeProgress());
        json.key("fileProgress").value(integrityScanner.getFileProgress());
        json.endObject();
    }

    @Uri(value = "/cancel", method = HttpMethod.POST)
    public void cancel() {
        integrityScanner.cancelScan();
    }

    private void writeReportAsResponse(IntegrityReport report, WebScriptResponse response) throws IOException {
        response.setContentEncoding("utf-8");
        response.setHeader("Cache-Control", "no-cache");
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(new ISO8601DateFormat());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        if (report != null) {
            response.setContentType("application/json");
            mapper.writeValue(response.getWriter(), report);
        } else {
            response.setContentType("text/plain");
            response.setStatus(404);
            response.getWriter().write("No scan report exists yet. Run the scan-all Action.");
        }
    }
}

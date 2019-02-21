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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

@Component
@WebScript(baseUri = "/xenit/care4alf/integrity", families = "care4alf", description = "IntegrityScanner Verification")
@Authentication(AuthenticationType.ADMIN)
public class Integrity {
    @Autowired
    IntegrityScanner integrityScanner;

    @Uri(value = "/report", method = HttpMethod.GET)
    public void report(WebScriptRequest req, WebScriptResponse response) throws IOException {
        response.setContentType("application/json");
        response.setContentEncoding("utf-8");
        response.setHeader("Cache-Control", "no-cache");
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(new ISO8601DateFormat());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writeValue(response.getWriter(), integrityScanner.getLastReport().get());
    }
}

package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by willem on 3/7/16.
 */
@Component
@WebScript(description = "Monitoring")
public class Monitoring {
    @Authentication(AuthenticationType.NONE)
    @Uri("/xenit/care4alf/monitoring")
    public void monitoring(final WebScriptResponse response) throws IOException {
        response.getWriter().write("OK");
    }
}
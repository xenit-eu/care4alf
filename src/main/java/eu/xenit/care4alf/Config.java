package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import org.json.JSONException;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Enumeration;

/**
 * Created by willem on 9/1/16.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/config")
@Authentication(AuthenticationType.ADMIN)
public class Config {
    @Autowired()
    @Qualifier("global-properties")
    private java.util.Properties properties;

    @Uri("/")
    public void listGlobalProperties(WebScriptResponse res) throws IOException, JSONException {
        final JSONWriter json = new JSONWriter(res.getWriter());
        json.object();
        Enumeration<String> it = (Enumeration<String>) this.properties.propertyNames();
        while (it.hasMoreElements()) {
            String name = it.nextElement();
            json.key(name).value(this.properties.getProperty(name));
        }
        json.endObject();
    }

    public String getProperty(String name) {
        return this.properties.getProperty(name);
    }

}

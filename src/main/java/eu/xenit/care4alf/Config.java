package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Created by willem on 9/1/16.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/config", families = {"care4alf"}, description = "Config")
@Authentication(AuthenticationType.ADMIN)
public class Config {
    @Autowired()
    @Qualifier("global-properties")
    private java.util.Properties properties;

    @Uri("/")
    public void listGlobalProperties(WebScriptResponse res) throws IOException, JSONException {
        final JSONWriter json = new JSONWriter(res.getWriter());
        json.object();
        List<String> propertyNames = Collections.list((Enumeration<String>) this.properties.propertyNames());
        Collections.sort(propertyNames);
        for(String name : propertyNames)
            json.key(name).value(this.properties.getProperty(name));
        json.endObject();
    }

    public String getProperty(String name) {
        return this.properties.getProperty(name);
    }

    public void addProperty(String key, String value){
        this.properties.setProperty(key, value);
    }

    public void removeProperty(String key){
        this.properties.remove(key);
    }

    @Uri(value = "/", method = HttpMethod.POST)
    public void restAddProperty(JSONObject json) throws JSONException {
        this.addProperty(json.getString("key"), json.getString("value"));
    }

    @Uri(value = "/{key}", method = HttpMethod.DELETE)
    public void restDeleteProperty(@UriVariable String key) throws JSONException {
        this.removeProperty(key);
    }

}
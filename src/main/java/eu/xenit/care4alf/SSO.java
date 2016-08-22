package eu.xenit.care4alf;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.json.JSONException;
import org.json.JSONWriter;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.SQLException;

/**
 * Created by willem on 8/22/16.
 */
@Component
@WebScript(families = "care4alf", baseUri = "/xenit/care4alf/sso", description = "SSO")
@Authentication(AuthenticationType.NONE)
public class SSO {
    @Uri("/header")
    public void header(WebScriptRequest req, WebScriptResponse res) throws SQLException, IOException, JSONException {
        res.setContentType("application/json");
        final JSONWriter json = new JSONWriter(res.getWriter());
        json.object();
        json.key("length").value(this.getHeaderLength(req));//#bytes
        json.key("content");
            json.object();
            for(String name : req.getHeaderNames()){
                json.key(name).value(req.getHeader(name));
            }
            json.endObject();
        json.endObject();
    }

    public int getHeaderLength(WebScriptRequest req){
        int length = 0;
        for(String name : req.getHeaderNames()){
            length += name.getBytes().length;
            for(String value : req.getHeaderValues(name))
            {
                length += value.getBytes().length;
            }
        }
        return length;
    }
}

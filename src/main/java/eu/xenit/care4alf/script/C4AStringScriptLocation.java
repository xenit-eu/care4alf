package eu.xenit.care4alf.script;

import org.alfresco.service.cmr.repository.ScriptLocation;
import org.alfresco.util.MD5;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Younes on 04/11/2016.
 */
public class C4AStringScriptLocation implements ScriptLocation {
    private String script;
    private String path;

    private static Map<String, C4AStringScriptLocation> instances = new HashMap<String, C4AStringScriptLocation>();

    public static C4AStringScriptLocation getC4AStringScriptLocationForString(String script){
        String key = MD5.Digest(script.getBytes(Charset.forName("UTF-8"))) + ".js";
        if (!instances.containsKey(key)){
            C4AStringScriptLocation sl = new C4AStringScriptLocation(key, script);
            instances.put(key, sl);
        }
        return instances.get(key);
    }

    public C4AStringScriptLocation(String path, String script){
        this.script = script;
        this.path = path;
    }
    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(script.getBytes(Charset.forName("UTF-8")));
    }

    @Override
    public Reader getReader() {
        return new StringReader(this.script);
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public boolean isCachable() {
        return true;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String toString(){
        return "["+path+"]";
    }
}
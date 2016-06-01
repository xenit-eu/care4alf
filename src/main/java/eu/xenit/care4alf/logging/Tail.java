package eu.xenit.care4alf.logging;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.JsonWriterResolution;
import com.github.dynamicextensionsalfresco.webscripts.resolutions.Resolution;
import org.json.JSONException;
import org.json.JSONWriter;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by willem on 5/10/16.
 */
@Component
@Authentication(AuthenticationType.ADMIN)
@WebScript(baseUri = "/xenit/care4alf/tail", families = {"care4alf"}, description = "Tail of logs")
public class Tail {
    @Uri(value = "/tails")
    public Resolution tail(@RequestParam(defaultValue = "200") int n, @RequestParam(defaultValue = "/opt/alfresco/tomcat/logs/catalina.out") String path, WebScriptResponse resp) throws IOException {
        final ArrayList<String> output = new ArrayList<String>();

        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(path))) {
            for (int i = 0; i < n; i++)
                output.add(reader.readLine());
        } catch(final Exception e){
            return new JsonWriterResolution() {
                @Override
                protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                    StackTraceElement[] elements = e.getStackTrace();
                    e.printStackTrace();
                    jsonWriter.array();
                    jsonWriter.object();
                    jsonWriter.key("timestamp").value(e.getClass());
                    jsonWriter.key("text").value(e.getLocalizedMessage());
                    jsonWriter.endObject();
                    for( StackTraceElement element :elements){
                        jsonWriter.object();
                        jsonWriter.key("text").value("at " + element.toString());
                        jsonWriter.endObject();
                    }
                    jsonWriter.endArray();
                }
            };
        };

        return new JsonWriterResolution() {
            @Override
            protected void writeJson(JSONWriter jsonWriter) throws JSONException {
                jsonWriter.array();
                for (int i = output.size() - 1; i >= 0; i--) {
                    String string = output.get(i);
                    Map<String, String> formated = formatLines(string);
                    jsonWriter.object();
                    for (Map.Entry<String, String> entry : formated.entrySet()) {
                        String key = entry.getKey();
                        String value = entry.getValue();
                        jsonWriter.key(key).value(value);
                    }
                    jsonWriter.endObject();
                }
                jsonWriter.endArray();
            }
        };


    }

    @Uri(value="/printtail",defaultFormat = "text")
    public void printtail(@RequestParam(defaultValue = "200") int n, @RequestParam(defaultValue = "/opt/alfresco/tomcat/logs/catalina.out") String path, WebScriptResponse resp) throws IOException {
        ArrayList<String> output = new ArrayList<String>();
        Writer writer = resp.getWriter();
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(new File(path))) {
            for (int i = 0; i < n; i++)
                output.add(reader.readLine() + "\n");
        }
        catch(Exception e){
            String msg = e.toString()+"\n";
            writer.append(msg);
            for(StackTraceElement element : e.getStackTrace()){
                msg = "\tat " + element.toString() + "\n";
                writer.append(msg);
            }
            return;
        }



        for (int i = n - 1; i >= 0; i--) {
            writer.append(output.get(i));
        }
    }

    public static Map<String, String> formatLines(String line) {
        Map<String, String> response = new HashMap<>();
        line = line.trim();
        Matcher m1 = Pattern.compile("\\s\\s").matcher(line);
        if (!m1.find()) {
            response.put("text", line);
        } else {
            String timestamp = line.split("\\s\\s")[0];
            response.put("timestamp", timestamp);
            Matcher matcher = Pattern.compile("[A-Z]{4,5}").matcher(line);
            if (matcher.find()) {
                response.put("status", matcher.group(0));
            }
            matcher = Pattern.compile("\\s(\\[\\S*])").matcher(line);
            int count = 0;
            while (matcher.find()) {
                if (count == 0) {
                    response.put("service", matcher.group(0));
                }
                if (count == 1) {
                    response.put("thread", matcher.group(1));
                }
                count++;
            }
            matcher = Pattern.compile("\\]\\s+([^\\[]+)").matcher(line);
            while (matcher.find()) {
                response.put("text", matcher.group(1));
            }
        }
        return response;
    }
}

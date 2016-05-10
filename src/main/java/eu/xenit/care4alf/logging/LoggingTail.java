package eu.xenit.care4alf.logging;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Created by willem on 5/10/16.
 */
@Component
@Authentication(AuthenticationType.ADMIN)
public class LoggingTail {
    @Uri(value="/xenit/care4alf/logging/tail",defaultFormat = "text")
    public void tail(@RequestParam(defaultValue = "200") int n, WebScriptResponse resp) throws IOException {
        ArrayList<String> output = new ArrayList<String>();

        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(new File("/opt/alfresco/tomcat/logs/catalina.out"))) {
            for (int i = 0; i < n; i++)
                output.add(reader.readLine() + "\n");
        }

        Writer writer = resp.getWriter();

        for (int i = n - 1; i >= 0; i--) {
            writer.append(output.get(i));
        }
    }
}
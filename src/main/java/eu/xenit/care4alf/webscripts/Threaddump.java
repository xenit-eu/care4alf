package eu.xenit.care4alf.webscripts;

import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Objects;

/**
 * Created by Thomas.Straetmans on 19/09/2016.
 */

@Component
@WebScript(baseUri = "/xenit/care4alf/thread", families = {"care4alf-noui"})
public class Threaddump {

    @Uri("dump")
    public void Dump(final WebScriptResponse response, @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "ALL") String name) throws IOException {
        String threaddump = generateThreadDump(status, name);
        response.getWriter().write(threaddump);
    }

    private String generateThreadDump(String status, String name) {
        final StringBuilder dump = new StringBuilder();
        final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        final ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
        for (ThreadInfo threadInfo : threadInfos) {
            if ((Objects.equals(threadInfo.getThreadName(), name) || name.equals("ALL")) && (Objects.equals(threadInfo.getThreadState().toString(), status) || status.equals("ALL"))) {
                dump.append('"');
                dump.append(threadInfo.getThreadName());
                dump.append("\" ");
                final Thread.State state = threadInfo.getThreadState();
                dump.append("\n  java.Thread.State: ");
                dump.append(state);
                final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
                for (final StackTraceElement stackTraceElement : stackTraceElements) {
                    dump.append("\n          at ");
                    dump.append(stackTraceElement);
                }
                dump.append("\n\n");
            }
        }
        return dump.toString();
    }
}

package eu.xenit.care4alf.scheduledjobs;

import com.github.dynamicextensionsalfresco.webscripts.annotations.Authentication;
import com.github.dynamicextensionsalfresco.webscripts.annotations.AuthenticationType;
import com.github.dynamicextensionsalfresco.webscripts.annotations.HttpMethod;
import com.github.dynamicextensionsalfresco.webscripts.annotations.RequestParam;
import com.github.dynamicextensionsalfresco.webscripts.annotations.Uri;
import com.github.dynamicextensionsalfresco.webscripts.annotations.UriVariable;
import com.github.dynamicextensionsalfresco.webscripts.annotations.WebScript;
import java.io.IOException;
import java.io.PrintWriter;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.WebScriptRequest;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

@Component
@WebScript(baseUri = "/xenit/care4alf/scheduled", families = {"care4alf"}, description = "Show and execute scheduled jobs")
@Authentication(AuthenticationType.ADMIN)
public class ScheduledJobs {

    @Autowired
    private ScheduledJobsImpl scheduledJobsImpl;

    @Uri(value = "groups")
    public void getJobGroups(final WebScriptResponse response) throws SchedulerException, IOException, JSONException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.array();
        for (String groupName : scheduledJobsImpl.getJobGroups()) {
            json.object();
            json.key("name");
            json.value(groupName);
            json.endObject();
        }
        json.endArray();
    }


    @Uri(value = "job")
    public void getJobsREST(@RequestParam(defaultValue = "DEFAULT") String groupname, final WebScriptResponse response)
            throws IOException, JSONException, SchedulerException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.array();

        for (ScheduledJob job : scheduledJobsImpl.getScheduledJobs(groupname)) {
            json.object();
            json.key("name");
            json.value(job.getTriggerName());
            json.key("jobClass");
            json.value(job.getJobClass().getSimpleName());
            json.key("previousFireTime");
            json.value(job.getPreviousFireTime());
            json.key("nextFireTime");
            json.value(job.getNextFireTime());
            json.key("cronExpression");
            json.value(job.getCronExpression());
            json.endObject();
        }
        json.endArray();
    }

    @Uri(value = "executing")
    public void getCurrentlyExecutingJobs(final WebScriptResponse response)
            throws IOException, JSONException, SchedulerException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.array();
        for (ExecutingJob executingJob : scheduledJobsImpl.getCurrentlyExecutingJobs()) {
            json.object();
            json.key("group");
            json.value(executingJob.getGroupName());
            json.key("name");
            json.value(executingJob.getJobName());
            json.key("firetime");
            json.value(executingJob.getFireTime());
            json.endObject();
        }
        json.endArray();
    }

    @Uri(value = "execute", method = HttpMethod.POST)
    public void execute(@RequestBody final JSONObject body) throws SchedulerException {
        scheduledJobsImpl.execute(body.getString("name"), body.getString("group"));
    }

    @Uri("validateschema/txt")
    public void showSchemaValidation(WebScriptResponse res) throws IOException {
        scheduledJobsImpl.validateSchema((PrintWriter) res.getWriter());
    }
}

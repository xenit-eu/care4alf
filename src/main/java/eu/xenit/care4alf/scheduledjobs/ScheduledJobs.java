package eu.xenit.care4alf.scheduledjobs;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.repo.domain.schema.SchemaBootstrap;
import org.json.JSONException;
import org.json.JSONWriter;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.extensions.webscripts.WebScriptResponse;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by willem on 6/1/15.
 */
@Component
@WebScript(baseUri = "/xenit/care4alf/scheduled", families = {"care4alf"}, description = "Show and execute scheduled jobs")
@Authentication(AuthenticationType.ADMIN)
public class ScheduledJobs {
    private final Logger logger = LoggerFactory.getLogger(ScheduledJobs.class);

    @Autowired
    @Qualifier("schedulerFactory")
    SchedulerFactoryBean schedulerFactory;

    public static final String[] ATTRIBUTES = new String[]{
            "CalendarName", "CronExpression", "Description", "EndTimeEndTime", "FinalFireTime", "Group", "JobGroup",
            "JobName", "MayFireAgain", "Name", "NextFireTime", "PreviousFireTime", "Priority", "StartTime", "State",
            "TimeZone", "Volatile"};

    @Uri(value="job")
    public void job(final WebScriptResponse response) throws IOException, JSONException, SchedulerException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.array();
        Scheduler scheduler = schedulerFactory.getScheduler();
        //loop all group
        for (String groupName : scheduler.getJobGroupNames()) {

            //loop all jobs by groupname
            for (String jobName : scheduler.getJobNames(groupName)) {
                json.object();
                json.key("JobName");
                json.value(jobName);
                json.key("Name");
                json.value(jobName);
                //get job's trigger
                Trigger[] triggers = scheduler.getTriggersOfJob(jobName,groupName);

                json.key("PreviousFireTime");
                json.value(triggers[0].getPreviousFireTime());
                json.key("NextFireTime");
                json.value(triggers[0].getNextFireTime());

                json.endObject();
            }
        }
        json.endArray();
    }

    public List<String> getScheduledJobsNames() throws SchedulerException {
        List<String> names = new ArrayList<>();
        Scheduler scheduler = schedulerFactory.getScheduler();
        System.out.println(scheduler.getTriggerGroupNames().toString());
        //loop all group
        for (String groupName : scheduler.getJobGroupNames()) {

            //loop all jobs by groupname
            for (String jobName : scheduler.getJobNames(groupName)) {
                names.add(jobName);
                //get job's trigger
                Trigger[] triggers = scheduler.getTriggersOfJob(jobName,groupName);
                Date nextFireTime = triggers[0].getNextFireTime();

                System.out.println("[jobName] : " + jobName + " [groupName] : "
                        + groupName + " - " + nextFireTime);

            }
        }
        return names;
    }

    @Uri(value="job/{name}/execute", method = HttpMethod.POST)
    public void executeGet(@UriVariable final String name) throws SchedulerException {
        this.execute(name);
    }

    public void execute(String fullName) throws SchedulerException {
        this.execute(fullName, "DEFAULT");
    }

    public void execute(String fullName, String groupName) throws SchedulerException {
        logger.info("Executing '{}' '{}'", fullName, groupName);
        this.schedulerFactory.getScheduler().triggerJob(fullName, groupName);
    }


    @Autowired
    SchemaBootstrap schemaBootstrap;
    @Uri("validateschema/txt")
    public void showSchemaValidation(WebScriptResponse res) throws IOException {
        this.validateSchema((PrintWriter) res.getWriter());
    }

    public void validateSchema(PrintWriter writer){
        this.schemaBootstrap.validateSchema("Alfresco-{0}-Validation-{1}-", writer);
        writer.write("END.");
    }

}
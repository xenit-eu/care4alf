package eu.xenit.care4alf.scheduledjobs;

import com.github.dynamicextensionsalfresco.webscripts.annotations.*;
import org.alfresco.repo.domain.schema.SchemaBootstrap;
import org.json.JSONException;
import org.json.JSONWriter;
import org.quartz.CronTrigger;
import org.quartz.JobExecutionContext;
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
    private SchedulerFactoryBean schedulerFactory;

    public static final String[] ATTRIBUTES = new String[]{
            "CalendarName", "CronExpression", "Description", "EndTimeEndTime", "FinalFireTime", "Group", "JobGroup",
            "JobName", "MayFireAgain", "Name", "NextFireTime", "PreviousFireTime", "Priority", "StartTime", "State",
            "TimeZone", "Volatile"};

    @Uri(value = "groups")
    public void getJobGroups(final WebScriptResponse response) throws SchedulerException, IOException, JSONException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.array();
        Scheduler scheduler = schedulerFactory.getScheduler();
        logger.debug("# of scheduler groups: {}", scheduler.getJobGroupNames().length);
        for (String groupName : scheduler.getJobGroupNames()) {
            json.object();
            json.key("name");
            json.value(groupName);
            json.endObject();
        }
        json.endArray();
    }


    @Uri(value = "job")
    public void getJobsREST(@RequestParam(defaultValue = "DEFAULT") String groupname, final WebScriptResponse response) throws IOException, JSONException, SchedulerException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        json.array();

        for (ScheduledJob job : this.getScheduledJobs(groupname)) {
            json.object();
            json.key("JobName");
            json.value(job.getName());
            json.key("Name");
            json.value(job.getName());
            json.key("PreviousFireTime");
            json.value(job.getPreviousFireTime());
            json.key("NextFireTime");
            json.value(job.getNextFireTime());
            json.key("CronExpression");
            json.value(job.getCronExpression());
            json.endObject();
        }
        json.endArray();
    }

    @Uri(value = "executing")
    public void getCurrentlyExecutingJobs(final WebScriptResponse response)
            throws IOException, JSONException, SchedulerException {
        final JSONWriter json = new JSONWriter(response.getWriter());
        Scheduler scheduler = schedulerFactory.getScheduler();
        json.array();
        List<JobExecutionContext> jobContexts = scheduler.getCurrentlyExecutingJobs();
        for (JobExecutionContext jobContext : jobContexts) {
            json.object();
            json.key("group");
            json.value(jobContext.getJobDetail().getGroup());
            json.key("name");
            json.value(jobContext.getJobDetail().getName());
            json.key("firetime");
            json.value(jobContext.getFireTime());
            json.endObject();
        }
        json.endArray();
    }

    public List<ScheduledJob> getScheduledJobs(String groupName) throws SchedulerException {
        List<ScheduledJob> jobs = new ArrayList<>();

        Scheduler scheduler = schedulerFactory.getScheduler();

        //loop all jobs by groupname
        for (String jobName : scheduler.getJobNames(groupName)) {
            Trigger[] triggers = scheduler.getTriggersOfJob(jobName, groupName);

            String cronExpression = "";
            if (triggers[0] instanceof CronTrigger)
                cronExpression = ((CronTrigger) triggers[0]).getCronExpression();

            jobs.add(new ScheduledJob(jobName, cronExpression, triggers[0].getPreviousFireTime(), triggers[0].getNextFireTime()));
        }


        return jobs;
    }

    @Uri(value = "job/{name}/{group}/execute", method = HttpMethod.POST)
    public void executeGet(@UriVariable final String name, @UriVariable final String group) throws SchedulerException {
        this.execute(name, group);
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

    public void validateSchema(PrintWriter writer) {
        this.schemaBootstrap.validateSchema("Alfresco-{0}-Validation-{1}-", writer);
        writer.write("END.");
    }

    class ScheduledJob {
        private String name, cronExpression;
        private Date previousFireTime, nextFireTime;

        public ScheduledJob(String name, String cronExpression, Date previousFireTime, Date nextFireTime) {
            this.name = name;
            this.cronExpression = cronExpression;
            this.previousFireTime = previousFireTime;
            this.nextFireTime = nextFireTime;
        }

        public String getName() {
            return name;
        }

        public String getCronExpression() {
            return cronExpression;
        }

        public Date getPreviousFireTime() {
            return previousFireTime;
        }

        public Date getNextFireTime() {
            return nextFireTime;
        }
    }

}
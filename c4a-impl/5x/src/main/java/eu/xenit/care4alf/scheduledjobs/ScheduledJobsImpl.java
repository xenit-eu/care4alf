package eu.xenit.care4alf.scheduledjobs;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.alfresco.repo.domain.schema.SchemaBootstrap;
import org.quartz.CronTrigger;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobsImpl {
    private final Logger logger = LoggerFactory.getLogger(ScheduledJobsImpl.class);

    @Autowired
    @Qualifier("schedulerFactory")
    private SchedulerFactoryBean schedulerFactory;

    @Autowired
    SchemaBootstrap schemaBootstrap;


    public List<String> getJobGroups() throws SchedulerException {
        Scheduler scheduler = schedulerFactory.getScheduler();
        logger.debug("# of scheduler groups: {}", scheduler.getJobGroupNames().length);
        return Arrays.asList(scheduler.getJobGroupNames());
    }


    public List<ExecutingJob> getCurrentlyExecutingJobs() throws SchedulerException {
        return ((List<JobExecutionContext>) schedulerFactory.getScheduler().getCurrentlyExecutingJobs())
                .stream()
                .map(ScheduledJobsImpl::toExecutingJob)
                .collect(Collectors.toList());
    }

    public List<ScheduledJob> getScheduledJobs(String groupName) throws SchedulerException {
        List<ScheduledJob> jobs = new ArrayList<>();

        Scheduler scheduler = schedulerFactory.getScheduler();

        //loop all jobs by groupname
        for (String jobName : scheduler.getJobNames(groupName)) {
            Class jobClass = scheduler.getJobDetail(jobName, groupName).getJobClass();

            String cronExpression = "";
            Trigger firstTrigger = scheduler.getTriggersOfJob(jobName, groupName)[0];
            if (firstTrigger instanceof CronTrigger) {
                cronExpression = ((CronTrigger) firstTrigger).getCronExpression();
            }

            jobs.add(new ScheduledJob(firstTrigger.getKey().getName(), cronExpression, jobClass,
                    firstTrigger.getPreviousFireTime(), firstTrigger.getNextFireTime()));
        }

        return jobs;
    }

    public void execute(String triggerName, String groupName) throws SchedulerException {
        logger.info("Executing '{}' '{}'", triggerName, groupName);
        Trigger trigger = schedulerFactory.getScheduler().getTrigger(triggerName, groupName);
        schedulerFactory.getScheduler().triggerJob(trigger.getJobName(), trigger.getJobGroup());
    }


    public void validateSchema(PrintWriter writer) {
        this.schemaBootstrap.validateSchema("Alfresco-{0}-Validation-{1}-", writer);
        writer.write("END.");
    }

    private static ExecutingJob toExecutingJob(JobExecutionContext context) {
        return new ExecutingJob(context.getJobDetail().getGroup(), context.getJobDetail().getName(),
                context.getFireTime());
    }
}
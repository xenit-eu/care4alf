package eu.xenit.care4alf.scheduledjobs;

import java.util.Date;
import org.quartz.Job;

class ScheduledJob {
    private String triggerName;
    private String cronExpression;
    private Class<? extends Job> jobClass;
    private Date previousFireTime;
    private Date nextFireTime;

    public ScheduledJob(String triggerName, String cronExpression, Class<? extends Job> jobClass, Date previousFireTime,
            Date nextFireTime) {
        this.triggerName = triggerName;
        this.cronExpression = cronExpression;
        this.jobClass = jobClass;
        this.previousFireTime = previousFireTime;
        this.nextFireTime = nextFireTime;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public String getCronExpression() {
        return cronExpression;
    }

    public Class<? extends Job> getJobClass() {
        return jobClass;
    }

    public Date getPreviousFireTime() {
        return previousFireTime;
    }

    public Date getNextFireTime() {
        return nextFireTime;
    }
}
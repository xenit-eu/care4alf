package eu.xenit.care4alf.scheduledjobs;

import java.util.Date;

public class ExecutingJob {
    private String groupName;
    private String jobName;
    private Date fireTime;

    public ExecutingJob(String groupName, String jobName, Date fireTime) {
        this.groupName = groupName;
        this.jobName = jobName;
        this.fireTime = fireTime;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getJobName() {
        return jobName;
    }

    public Date getFireTime() {
        return fireTime;
    }
}

package eu.xenit.care4alf;

import eu.xenit.apix.integrationtesting.runner.ApixIntegration;
import eu.xenit.care4alf.scheduledjobs.ScheduledJobs;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Created by willem on 1/19/17.
 */
@Component
@RunWith(ApixIntegration.class)
public class ScheduledJobsTest {

    @Autowired
    ScheduledJobs scheduledJobs;

    @Test
    public void listScheduledJobs() throws SchedulerException {
        Assert.assertTrue(scheduledJobs.getScheduledJobs().size() > 0);
    }

    @Test
    public void executeScheduledJob() throws SchedulerException {
        this.scheduledJobs.execute("downloadCleanerJobDetail");
    }

    @Test
    public void testValidateSchema(){
        scheduledJobs.validateSchema(new PrintWriter(new NullOutputStream()));
    }

    public class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
    }

}
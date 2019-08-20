package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.scheduledjobs.ScheduledJobsImpl;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;

/**
 * Created by willem on 1/19/17.
 */
@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class ScheduledJobsTest {

    @Autowired
    ScheduledJobsImpl scheduledJobsImpl;

    @Test
    public void listScheduledJobs() throws SchedulerException {
        Assert.assertTrue(scheduledJobsImpl.getScheduledJobs("DEFAULT").size() > 0);
    }

    @Test
    public void executeScheduledJob() throws SchedulerException {
        this.scheduledJobsImpl.execute("downloadCleanerTrigger","DEFAULT");
    }

    @Test
    public void testValidateSchema(){
        scheduledJobsImpl.validateSchema(new PrintWriter(new NullOutputStream()));
    }

    public class NullOutputStream extends OutputStream {
        @Override
        public void write(int b) throws IOException {
        }
    }

}
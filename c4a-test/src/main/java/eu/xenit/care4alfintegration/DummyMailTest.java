package eu.xenit.care4alfintegration;

import eu.xenit.care4alf.module.DummyMail;
import eu.xenit.testing.integrationtesting.runner.AlfrescoTestRunner;
import eu.xenit.testing.integrationtesting.runner.UseSpringContextOfBundle;
import java.util.Objects;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@RunWith(AlfrescoTestRunner.class)
@UseSpringContextOfBundle(bundleId = "eu.xenit.care4alf")
public class DummyMailTest {
    @Autowired
    private DummyMail dummyMail;
    @Autowired
    private JavaMailSender mailer;

    private static final String TEST = "TEST";

    @Test
    public void sendMailToSelf() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("test@xenit.eu");
        message.setSubject(TEST);
        message.setText("This is a test of the Care4Alf dummy mail system.");
        message.setTo("c4a@example.com");

        mailer.send(message);

        Assert.assertTrue("Mailstore contains test email",
                dummyMail.getMessages().stream()
                        .filter(Objects::nonNull)
                        .anyMatch(m -> TEST.equals(m.getFirstHeaderValue("Subject"))));

    }
}

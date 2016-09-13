package eu.xenit.care4alf.dumbster.smtp.action;

import eu.xenit.care4alf.dumbster.smtp.MailMessage;
import eu.xenit.care4alf.dumbster.smtp.Response;
import eu.xenit.care4alf.dumbster.smtp.MailStore;
import eu.xenit.care4alf.dumbster.smtp.SmtpState;

public interface Action {

    public abstract String toString();

    public abstract Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage);

}

package xenit.care4alf.dumbster.smtp.action;

import xenit.care4alf.dumbster.smtp.*;

public interface Action {

    public abstract String toString();

    public abstract Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage);

}

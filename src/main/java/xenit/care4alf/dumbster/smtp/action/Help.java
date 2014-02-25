package xenit.care4alf.dumbster.smtp.action;

import xenit.care4alf.dumbster.smtp.MailMessage;
import xenit.care4alf.dumbster.smtp.MailStore;
import xenit.care4alf.dumbster.smtp.Response;
import xenit.care4alf.dumbster.smtp.SmtpState;

public class Help implements Action {

    @Override
    public String toString() {
        return "HELP";
    }

    public Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage) {
        return new Response(211, "No help available", smtpState);
    }

}

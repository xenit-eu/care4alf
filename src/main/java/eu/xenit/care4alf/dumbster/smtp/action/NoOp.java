package eu.xenit.care4alf.dumbster.smtp.action;

import eu.xenit.care4alf.dumbster.smtp.MailMessage;
import eu.xenit.care4alf.dumbster.smtp.MailStore;
import eu.xenit.care4alf.dumbster.smtp.Response;
import eu.xenit.care4alf.dumbster.smtp.SmtpState;

public class NoOp implements Action {

    @Override
    public String toString() {
        return "NOOP";
    }

    public Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage) {
        return new Response(250, "OK", smtpState);
    }

}

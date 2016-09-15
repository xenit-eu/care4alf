package eu.xenit.care4alf.dumbster.smtp.action;

import eu.xenit.care4alf.dumbster.smtp.MailMessage;
import eu.xenit.care4alf.dumbster.smtp.MailStore;
import eu.xenit.care4alf.dumbster.smtp.Response;
import eu.xenit.care4alf.dumbster.smtp.SmtpState;

public class Ehlo implements Action {

    public String toString() {
        return "EHLO";
    }

    public Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage) {
        if (SmtpState.GREET == smtpState) {
            return new Response(250, "OK", SmtpState.MAIL);
        } else {
            return new Response(503, "Bad sequence of commands: "
                    + this, smtpState);
        }
    }

}

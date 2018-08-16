package eu.xenit.care4alf.dumbster.smtp.action;

import eu.xenit.care4alf.dumbster.smtp.MailMessage;
import eu.xenit.care4alf.dumbster.smtp.MailStore;
import eu.xenit.care4alf.dumbster.smtp.Response;
import eu.xenit.care4alf.dumbster.smtp.SmtpState;

public class Connect implements Action {

    public String toString() {
        return "Connect";
    }

    public Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage) {
        if (SmtpState.CONNECT == smtpState) {
            return new Response(220,
                    "localhost Dumbster SMTP service ready", SmtpState.GREET);
        } else {
            return new Response(503, "Bad sequence of commands: " + this,
                    smtpState);
        }
    }

}

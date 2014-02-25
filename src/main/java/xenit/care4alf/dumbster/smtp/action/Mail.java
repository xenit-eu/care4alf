package xenit.care4alf.dumbster.smtp.action;

import xenit.care4alf.dumbster.smtp.MailMessage;
import xenit.care4alf.dumbster.smtp.MailStore;
import xenit.care4alf.dumbster.smtp.Response;
import xenit.care4alf.dumbster.smtp.SmtpState;

public class Mail implements Action {

    @Override
    public String toString() {
        return "MAIL";
    }

    public Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage) {
        if (SmtpState.MAIL == smtpState || SmtpState.QUIT == smtpState) {
            return new Response(250, "OK", SmtpState.RCPT);
        } else {
            return new Response(503,
                    "Bad sequence of commands: " + this, smtpState);
        }
    }

}

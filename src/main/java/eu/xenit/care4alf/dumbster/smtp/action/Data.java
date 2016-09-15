package eu.xenit.care4alf.dumbster.smtp.action;

import eu.xenit.care4alf.dumbster.smtp.MailMessage;
import eu.xenit.care4alf.dumbster.smtp.MailStore;
import eu.xenit.care4alf.dumbster.smtp.Response;
import eu.xenit.care4alf.dumbster.smtp.SmtpState;

public class Data implements Action {

    @Override
    public String toString() {
        return "DATA";
    }

    public Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage) {
        if (SmtpState.RCPT == smtpState) {
            return new Response(354,
                    "Start mail input; end with <CRLF>.<CRLF>",
                    SmtpState.DATA_HDR);
        } else {
            return new Response(503,
                    "Bad sequence of commands: " + this, smtpState);
        }
    }

}

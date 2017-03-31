package eu.xenit.care4alf.dumbster.smtp.action;

import eu.xenit.care4alf.dumbster.smtp.MailMessage;
import eu.xenit.care4alf.dumbster.smtp.MailStore;
import eu.xenit.care4alf.dumbster.smtp.Response;
import eu.xenit.care4alf.dumbster.smtp.SmtpState;

public class DataEnd implements Action {

    @Override
    public String toString() {
        return ".";
    }

    public Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage) {
        if (SmtpState.DATA_HDR == smtpState || SmtpState.DATA_BODY == smtpState) {
            return new Response(250, "OK", SmtpState.QUIT);
        } else {
            return new Response(503, "Bad sequence of commands: " + this, smtpState);
        }
    }

}

package eu.xenit.care4alf.dumbster.smtp.action;

import eu.xenit.care4alf.dumbster.smtp.MailMessage;
import eu.xenit.care4alf.dumbster.smtp.MailStore;
import eu.xenit.care4alf.dumbster.smtp.Response;
import eu.xenit.care4alf.dumbster.smtp.SmtpState;

public class Quit implements Action {

    @Override
    public String toString() {
        return "QUIT";
    }

    public Response response(SmtpState smtpState, MailStore mailStore, MailMessage currentMessage) {
        return new Response(221, "localhost Dumbster service closing transmission channel",
                SmtpState.CONNECT);
    }

}

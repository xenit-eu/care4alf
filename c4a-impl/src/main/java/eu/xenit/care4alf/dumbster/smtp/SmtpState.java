/*
 * Dumbster - a dummy SMTP server
 * Copyright 2004 Jason Paul Kitchen
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package eu.xenit.care4alf.dumbster.smtp;

public enum SmtpState {
    CONNECT("CONNECT"), GREET("GREET"), MAIL("MAIL"), RCPT("RCPT"), DATA_HDR(
            "DATA_HDR"), DATA_BODY("DATA_BODY"), QUIT("QUIT");

    private String description;

    SmtpState(String description) {
        this.description = description;
    }

    public String toString() {
        return this.description;
    }

}

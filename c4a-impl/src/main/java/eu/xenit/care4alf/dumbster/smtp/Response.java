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

/**
 * SMTP response container.
 */
public class Response {
	/**
	 * Response code - see RFC-2821.
	 */
	private int code;
	/**
	 * Response message.
	 */
	private String message;
	/**
	 * New state of the SMTP server once the request has been executed.
	 */
	private SmtpState nextState;

	/**
	 * Constructor.
	 * 
	 * @param code
	 *            response code
	 * @param message
	 *            response message
	 * @param next
	 *            next state of the SMTP server
	 */
	public Response(int code, String message, SmtpState next) {
		this.code = code;
		this.message = message;
		this.nextState = next;
	}

	/**
	 * Get the response code.
	 * 
	 * @return response code
	 */
	public int getCode() {
		return code;
	}

	/**
	 * Get the response message.
	 * 
	 * @return response message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * Get the next SMTP server state.
	 * 
	 * @return state
	 */
	public SmtpState getNextState() {
		return nextState;
	}
}

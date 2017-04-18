//Have a look at https://github.com/MrJabo/SecureDataSocket

package org.sufficientlysecure.keychain.network.secureDataSocket;

import java.io.PrintWriter;
import java.io.StringWriter;

public class SecureDataSocketException extends Exception {

	private Exception originalException;
	private boolean critical = false;

	SecureDataSocketException(String description, Exception originalException) {
		this(description, originalException, true);	
	}

	SecureDataSocketException(String description, Exception originalException, boolean critical) {
		super(description+"\n"+getStackTrace(originalException));
		this.critical = critical;
		this.originalException = originalException;
	}

	private static String getStackTrace(Exception e){
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		return sw.toString();
	}
}

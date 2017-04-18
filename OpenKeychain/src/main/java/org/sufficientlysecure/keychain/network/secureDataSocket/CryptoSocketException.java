//Have a look at https://github.com/MrJabo/SecureDataSocket

package org.sufficientlysecure.keychain.network.secureDataSocket;

public class CryptoSocketException extends Exception {
	
	CryptoSocketException(String description){
		super(description);
	}
}

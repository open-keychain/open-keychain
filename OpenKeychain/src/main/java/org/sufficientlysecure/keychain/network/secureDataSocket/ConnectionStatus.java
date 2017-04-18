//Have a look at https://github.com/MrJabo/SecureDataSocket

package org.sufficientlysecure.keychain.network.secureDataSocket;

public enum ConnectionStatus {
	NotSetup,
	WaitingForPartner,
	Error,
	VerifiedRunning,
	Closed;
}

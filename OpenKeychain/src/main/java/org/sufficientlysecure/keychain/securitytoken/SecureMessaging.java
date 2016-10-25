package org.sufficientlysecure.keychain.securitytoken;

import java.io.IOException;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;

public interface SecureMessaging {

    void clearSession();

    boolean isEstablished();

    CommandAPDU encryptAndSign(CommandAPDU apdu) throws SecureMessagingException;

    ResponseAPDU verifyAndDecrypt(ResponseAPDU apdu) throws SecureMessagingException;
}

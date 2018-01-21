package org.sufficientlysecure.keychain.network;


import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import android.os.Build.VERSION_CODES;
import android.support.annotation.RequiresApi;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManager;


@RequiresApi(api = VERSION_CODES.LOLLIPOP)
class TlsPskCompat {

    static SSLContext createTlsPskSslContext(byte[] presharedKey) {
        try {
            PresharedKeyManager pskKeyManager = new PresharedKeyManager(presharedKey);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(new KeyManager[] { pskKeyManager }, new TrustManager[0], null);

            return sslContext;
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @SuppressWarnings("unused")
    /* This class is a KeyManager that is compatible to TlsPskManager.
     *
     * Due to the way conscrypt works internally, this class will be internally duck typed to
     * PSKKeyManager. This is quite a hack, and relies on conscrypt internals to work - but it
     * works.
     *
     * see also:
     * https://github.com/google/conscrypt/blob/b23e9353ed4e3256379d660cb09491a69b21affb/common/src/main/java/org/conscrypt/SSLParametersImpl.java#L494
     * https://github.com/google/conscrypt/blob/29916ef38dc9cb4e4c6e3fdb87d4e921546d3ef4/common/src/main/java/org/conscrypt/DuckTypedPSKKeyManager.java#L51
     *
     */
    private static class PresharedKeyManager implements KeyManager {
        byte[] presharedKey;

        private PresharedKeyManager(byte[] presharedKey) {
            this.presharedKey = presharedKey;
        }

        public String chooseServerKeyIdentityHint(Socket socket) {
            return null;
        }

        public String chooseServerKeyIdentityHint(SSLEngine engine) {
            return null;
        }

        public String chooseClientKeyIdentity(String identityHint, Socket socket) {
            return identityHint;
        }

        public String chooseClientKeyIdentity(String identityHint, SSLEngine engine) {
            return identityHint;
        }

        public SecretKey getKey(String identityHint, String identity, Socket socket) {
            return new SecretKeySpec(presharedKey, "AES");
        }

        public SecretKey getKey(String identityHint, String identity, SSLEngine engine) {
            return new SecretKeySpec(presharedKey, "AES");
        }
    }
}

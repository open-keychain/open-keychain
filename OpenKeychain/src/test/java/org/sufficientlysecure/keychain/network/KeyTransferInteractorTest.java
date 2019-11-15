package org.sufficientlysecure.keychain.network;


import java.net.URISyntaxException;

import android.os.Build.VERSION_CODES;
import androidx.annotation.RequiresApi;

import junit.framework.Assert;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowLooper;
import org.sufficientlysecure.keychain.network.KeyTransferInteractor.KeyTransferCallback;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;


@SuppressWarnings("WeakerAccess")
// disabled, because we can't easily mock the tls-psk ciphersuite (it's removed in bouncycastle) :(
//@RunWith(KeychainTestRunner.class)
@RequiresApi(api = VERSION_CODES.LOLLIPOP)
public class KeyTransferInteractorTest {
    private static final String DELIM_START = "--";
    private static final String DELIM_END = "--";

    private String receivedQrCodeData;
    private boolean clientConnectionEstablished;
    private boolean serverConnectionEstablished;

//    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;
    }

//    @Test
    public void testServerShouldGiveSuccessCallback() throws URISyntaxException {
        KeyTransferInteractor serverKeyTransferInteractor = new KeyTransferInteractor(DELIM_START, DELIM_END);

        serverKeyTransferInteractor.startServer(new SimpleKeyTransferCallback() {
            @Override
            public void onServerStarted(String qrCodeData) {
                receivedQrCodeData = qrCodeData;
            }

            @Override
            public void onConnectionEstablished(String otherName) {
                serverConnectionEstablished = true;
            }
        }, null);
        waitForLooperCallback();
        Assert.assertNotNull(receivedQrCodeData);

        final KeyTransferInteractor clientKeyTransferInteractor = new KeyTransferInteractor(DELIM_START, DELIM_END);
        clientKeyTransferInteractor.connectToServer(receivedQrCodeData, new SimpleKeyTransferCallback() {
            @Override
            public void onConnectionEstablished(String otherName) {
                clientConnectionEstablished = true;
            }
        });
        waitForLooperCallback();
        waitForLooperCallback();

        assertTrue(clientConnectionEstablished);
        assertTrue(serverConnectionEstablished);

        serverKeyTransferInteractor.sendData(new byte[] { (byte) 1, (byte) 2 }, "passthrough");
        waitForLooperCallback();
    }

    private void waitForLooperCallback() {
        while (!ShadowLooper.getShadowMainLooper().getScheduler().runOneTask());
    }


    static class SimpleKeyTransferCallback implements KeyTransferCallback {
        @Override
        public void onServerStarted(String qrCodeData) {
            fail("unexpected callback: onServerStarted");
        }

        @Override
        public void onConnectionEstablished(String otherName) {
            fail("unexpected callback: onConnectionEstablished");
        }

        @Override
        public void onConnectionLost() {
            fail("unexpected callback: onConnectionLost");
        }

        @Override
        public void onDataReceivedOk(String receivedData) {
            fail("unexpected callback: onDataReceivedOk");
        }

        @Override
        public void onDataSentOk(String passthrough) {
            fail("unexpected callback: onDataSentOk");
        }

        @Override
        public void onConnectionErrorNoRouteToHost(String wifiSsid) {
            fail("unexpected callback: onConnectionErrorNoRouteToHost");
        }

        @Override
        public void onConnectionErrorConnect() {
            fail("unexpected callback: onConnectionErrorConnect");
        }

        @Override
        public void onConnectionErrorListen() {
            fail("unexpected callback: onConnectionErrorListen");
        }

        @Override
        public void onConnectionError(String arg) {
            fail("unexpected callback: onConnectionError");
        }
    }

}

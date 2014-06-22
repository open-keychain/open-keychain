package tests;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.testsupport.KeyringTestingHelper;
import org.sufficientlysecure.keychain.testsupport.PgpVerifyTestingHelper;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class ProviderHelperKeyringTest {

    @Test
    public void testSavePublicKeyring() throws Exception {
        Assert.assertTrue(new KeyringTestingHelper(Robolectric.application).addKeyring());
    }

}

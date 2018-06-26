package org.sufficientlysecure.keychain;


import org.junit.runners.model.InitializationError;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.sufficientlysecure.keychain.shadows.ShadowWorkManager;


public class KeychainTestRunner extends RobolectricTestRunner {

    public KeychainTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Config buildGlobalConfig() {
        return new Config.Builder()
                .setSdk(27)
                .setConstants(WorkaroundBuildConfig.class)
                .setShadows(new Class[] { ShadowWorkManager.class })
                .build();
    }
}

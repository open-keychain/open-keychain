package org.sufficientlysecure.keychain;

import org.sufficientlysecure.keychain.BuildConfig;

/**
 * Temporary workaround for https://github.com/robolectric/robolectric/issues/1747
 */
public final class WorkaroundBuildConfig {
    public static final boolean DEBUG = BuildConfig.DEBUG;
    // Workaround: Use real packageName not applicationId
    public static final String APPLICATION_ID = "org.sufficientlysecure.keychain";
    public static final String BUILD_TYPE = BuildConfig.BUILD_TYPE;
    public static final String FLAVOR = BuildConfig.FLAVOR;
    public static final int VERSION_CODE = BuildConfig.VERSION_CODE;
    public static final String VERSION_NAME = BuildConfig.VERSION_NAME;
}

package org.sufficientlysecure.keychain.shadows;


import androidx.work.WorkManager;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.mockito.Mockito.mock;


@Implements(WorkManager.class)
public class ShadowWorkManager {

    @Implementation
    public static WorkManager getInstance() {
        return mock(WorkManager.class);
    }

}

package org.sufficientlysecure.keychain.resources;

import android.support.test.espresso.IdlingResource;
import android.support.v4.app.FragmentManager;

/**
 * Wait for dialogs to dismiss by tag.
 */
public class DialogFragmentIdlingResource implements IdlingResource {
    protected FragmentManager mManager;
    protected String mTag;
    private ResourceCallback mResourceCallback;

    public DialogFragmentIdlingResource(FragmentManager manager, String tag) {
        mManager = manager;
        mTag = tag;
    }

    @Override
    public String getName() {
        return DialogFragmentIdlingResource.class.getName() + ":" + mTag;
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = (mManager.findFragmentByTag(mTag) == null);
        if (idle) {
            mResourceCallback.onTransitionToIdle();
        }
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
        mResourceCallback = resourceCallback;
    }
}

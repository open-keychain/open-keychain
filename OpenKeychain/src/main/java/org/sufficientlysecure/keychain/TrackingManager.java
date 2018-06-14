package org.sufficientlysecure.keychain;


import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.os.Bundle;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.Tracker;
import org.piwik.sdk.TrackerConfig;
import org.piwik.sdk.extra.DownloadTracker.Extra.ApkChecksum;
import org.piwik.sdk.extra.TrackHelper;


public class TrackingManager {
    private Tracker piwikTracker;

    public static TrackingManager getInstance(Context context) {
        TrackerConfig trackerConfig = new TrackerConfig("https://mugenguild.com/piwik/", 1, "OpenKeychain");
        Tracker tracker = Piwik.getInstance(context).newTracker(trackerConfig);
        tracker.setDispatchInterval(30000);

        return new TrackingManager(tracker);
    }

    private TrackingManager(Tracker piwikTracker) {
        this.piwikTracker = piwikTracker;
    }

    public void initialize(Application application) {
        if (piwikTracker == null) {
            return;
        }
        TrackHelper.track().download().identifier(new ApkChecksum(application)).with(piwikTracker);

        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                TrackHelper.track().screen(activity.getClass().getSimpleName()).with(piwikTracker);
            }

            @Override
            public void onActivityPaused(Activity activity) {

            }

            @Override
            public void onActivityStopped(Activity activity) {

            }

            @Override
            public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

            }

            @Override
            public void onActivityDestroyed(Activity activity) {

            }
        });
    }

    public void trackFragmentImpression(String opClassName, String fragmentName) {
        if (piwikTracker == null) {
            return;
        }

        TrackHelper.track().screen(opClassName + "/" + fragmentName).with(piwikTracker);
    }

    public void trackInternalServiceCall(String opClassName) {
        if (piwikTracker == null) {
            return;
        }
        TrackHelper.track()
                .interaction("internalApiCall", opClassName)
                .with(piwikTracker);
    }

    public void trackApiServiceCall(String opClassName, String currentCallingPackage) {
        if (piwikTracker == null) {
            return;
        }

        TrackHelper.track()
                .interaction("externalApiCall", opClassName)
                .piece(currentCallingPackage.replace(".", "/"))
                .with(piwikTracker);
    }
}

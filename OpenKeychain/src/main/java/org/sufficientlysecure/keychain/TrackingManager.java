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
import org.sufficientlysecure.keychain.util.Preferences;


public class TrackingManager {
    private Tracker piwikTracker;

    public static TrackingManager getInstance(Context context) {
        return new TrackingManager(context);
    }

    private TrackingManager(Context context) {
        refreshSettings(context);
    }

    public void initialize(Application application) {
        if (piwikTracker != null) {
            TrackHelper.track().download().identifier(new ApkChecksum(application)).with(piwikTracker);
        }

        application.registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
            }

            @Override
            public void onActivityStarted(Activity activity) {

            }

            @Override
            public void onActivityResumed(Activity activity) {
                if (piwikTracker == null) {
                    return;
                }
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

    public synchronized void refreshSettings(Context context) {
        boolean analyticsHasConsent = Preferences.getPreferences(context).isAnalyticsHasConsent();
        boolean analyticsEnabled = piwikTracker != null;
        if (analyticsHasConsent != analyticsEnabled) {
            if (analyticsHasConsent) {
                TrackerConfig trackerConfig = new TrackerConfig("https://piwik.openkeychain.org/", 1, "OpenKeychain");
                piwikTracker = Piwik.getInstance(context).newTracker(trackerConfig);
                piwikTracker.setDispatchInterval(60000);
                piwikTracker.setOptOut(false);
            } else {
                piwikTracker.setOptOut(true);
                piwikTracker = null;
            }
        }
    }
}

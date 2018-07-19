package org.sufficientlysecure.keychain.analytics;


import android.app.Activity;
import android.app.Application;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.Tracker;
import org.piwik.sdk.TrackerConfig;
import org.piwik.sdk.extra.DownloadTracker.Extra.ApkChecksum;
import org.piwik.sdk.extra.TrackHelper;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Constants.Defaults;
import org.sufficientlysecure.keychain.Constants.Pref;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public class AnalyticsManager implements OnSharedPreferenceChangeListener {
    private Tracker piwikTracker;

    public static AnalyticsManager getInstance(Context context) {
        return new AnalyticsManager(context);
    }

    private AnalyticsManager(Context context) {
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

        Preferences preferences = Preferences.getPreferences(application);
        preferences.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    // we generally only track booleans. never snoop around in the user's string settings!!
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (piwikTracker == null) {
            return;
        }

        // small exception: check if the user uses a custom keyserver, or one of the well-known ones
        if (Pref.KEY_SERVERS.equals(key)) {
            Timber.d("Tracking pref %s", key);
            String keyServers = sharedPreferences.getString(Pref.KEY_SERVERS, Defaults.KEY_SERVERS);
            String current = keyServers.substring(keyServers.indexOf(','));

            String coarseGranularityKeyserver;
            if (current.contains("keyserver.ubuntu.com")) {
                coarseGranularityKeyserver = "ubuntu";
            } else if (current.contains("pgp.mit.edu")) {
                coarseGranularityKeyserver = "mit";
            } else if (current.contains("pool.sks-keyservers.net")) {
                coarseGranularityKeyserver = "pool";
            } else {
                coarseGranularityKeyserver = "custom";
            }
            TrackHelper.track().interaction("pref_" + Pref.KEY_SERVERS, coarseGranularityKeyserver).with(piwikTracker);
            return;
        }
        // unpack an enum
        if (Pref.THEME.equals(key)) {
            String value = sharedPreferences.getString(Pref.THEME, "empty");
            TrackHelper.track().interaction("pref_" + Pref.THEME, value).with(piwikTracker);
            return;
        }
        // all other values we track are individual booleans
        if (Pref.ANALYTICS_PREFS.contains(key)) {
            Timber.d("Tracking pref %s", key);
            if (!sharedPreferences.contains(key)) {
                TrackHelper.track().interaction("pref_" + key, "empty").with(piwikTracker);
                return;
            }
            boolean value = sharedPreferences.getBoolean(key, false);
            TrackHelper.track().interaction("pref_" + key, value ? "true" : "false").with(piwikTracker);
        }
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
        boolean shouldEnableAnalytics = shouldEnableAnalytics(context);
        boolean analyticsEnabled = piwikTracker != null;
        if (shouldEnableAnalytics != analyticsEnabled) {
            if (shouldEnableAnalytics) {
                TrackerConfig trackerConfig;
                if (Constants.DEBUG) {
                    trackerConfig = new TrackerConfig("https://piwik.openkeychain.org/", 3, "OpenKeychainDebug");
                } else {
                    trackerConfig = new TrackerConfig("https://piwik.openkeychain.org/", 2, "OpenKeychain");
                }
                piwikTracker = Piwik.getInstance(context).newTracker(trackerConfig);
                piwikTracker.setDispatchInterval(60000);
                piwikTracker.setOptOut(false);
            } else {
                piwikTracker.setOptOut(true);
                piwikTracker = null;
            }
        }
    }

    private boolean shouldEnableAnalytics(Context context) {
        Preferences preferences = Preferences.getPreferences(context);
        return preferences.isAnalyticsHasConsent() && !preferences.getUseTorProxy();
    }
}

package org.sufficientlysecure.keychain.analytics;


import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceActivity;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.KeychainApplication;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.SettingsActivity;
import org.sufficientlysecure.keychain.ui.SettingsActivity.ExperimentalPrefsFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.Preferences;


public class AnalyticsConsentRequester {
    private final Activity activity;

    public static AnalyticsConsentRequester getInstance(Activity activity) {
        return new AnalyticsConsentRequester(activity);
    }

    private AnalyticsConsentRequester(Activity activity) {
        this.activity = activity;
    }

    public void maybeAskForAnalytics() {
        Preferences preferences = Preferences.getPreferences(activity);
        if (preferences.isAnalyticsHasConsent()) {
            return;
        }

        boolean askedBeforeAndWasRejected =
                preferences.isAnalyticsAskedPolitely() && !preferences.isAnalyticsHasConsent();
        if (askedBeforeAndWasRejected) {
            return;
        }

        try {
            long firstInstallTime =
                    activity.getPackageManager().getPackageInfo(BuildConfig.APPLICATION_ID, 0).firstInstallTime;
            long threeDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(3);
            boolean installedLessThanThreeDaysAgo = firstInstallTime > threeDaysAgo;
            if (installedLessThanThreeDaysAgo) {
                return;
            }
        } catch (NameNotFoundException e) {
            return;
        }

        long twentyFourHoursAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1);
        boolean askedLessThan24HoursAgo = preferences.getAnalyticsLastAsked() > twentyFourHoursAgo;
        if (askedLessThan24HoursAgo) {
            return;
        }

        preferences.setAnalyticsLastAskedNow();

        AnalyticsManager analyticsManager = ((KeychainApplication) activity.getApplication()).getAnalyticsManager();
        AlertDialog alertDialog = new Builder(activity)
                .setMessage(R.string.dialog_analytics_consent)
                .setPositiveButton(R.string.button_analytics_yes, (dialog, which) -> {
                    preferences.setAnalyticsAskedPolitely();
                    preferences.setAnalyticsGotUserConsent(true);
                    analyticsManager.refreshSettings(activity);
                    Notify.create(activity, R.string.snack_analytics_accept, Style.OK,
                            this::startExperimentalSettingsActivity, R.string.snackbutton_analytics_settings).show();
                })
                .setNegativeButton(R.string.button_analytics_no, (dialog, which) -> {
                    preferences.setAnalyticsAskedPolitely();
                    preferences.setAnalyticsGotUserConsent(false);
                    analyticsManager.refreshSettings(activity);
                    Notify.create(activity, R.string.snack_analytics_reject, Style.OK,
                            this::startExperimentalSettingsActivity, R.string.snackbutton_analytics_settings).show();
                })
                .show();
        alertDialog.<TextView>findViewById(android.R.id.message).setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void startExperimentalSettingsActivity() {
        Intent resultIntent = new Intent(activity, SettingsActivity.class);
        String experimentalPrefsName = ExperimentalPrefsFragment.class.getName();
        resultIntent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, experimentalPrefsName);
        activity.startActivity(resultIntent);
    }
}

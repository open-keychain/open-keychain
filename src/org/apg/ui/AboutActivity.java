package org.apg.ui;

import org.apg.Constants;
import org.apg.R;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class AboutActivity extends Activity {
    Activity mActivity;

    /**
     * Instantiate View for this Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.about_activity);

        mActivity = this;

        TextView versionText = (TextView) findViewById(R.id.about_version);
        versionText.setText(getString(R.string.about_version) + " " + getVersion());
    }

    /**
     * Get the current package version.
     * 
     * @return The current version.
     */
    private String getVersion() {
        String result = "";
        try {
            PackageManager manager = mActivity.getPackageManager();
            PackageInfo info = manager.getPackageInfo(mActivity.getPackageName(), 0);

            result = String.format("%s (%s)", info.versionName, info.versionCode);
        } catch (NameNotFoundException e) {
            Log.w(Constants.TAG, "Unable to get application version: " + e.getMessage());
            result = "Unable to get application version.";
        }

        return result;
    }
}

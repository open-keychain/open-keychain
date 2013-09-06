package org.sufficientlysecure.keychain.remote_api;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class AppSettingsActivity extends SherlockFragmentActivity {
    private Uri mAppUri;

    private AppSettingsFragment settingsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Inflate a "Done" custom action bar view to serve as the "Up" affordance.
        final LayoutInflater inflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                .getSystemService(LAYOUT_INFLATER_SERVICE);
        final View customActionBarView = inflater
                .inflate(R.layout.actionbar_custom_view_done, null);

        ((TextView) customActionBarView.findViewById(R.id.actionbar_done_text))
                .setText(R.string.api_settings_save);
        customActionBarView.findViewById(R.id.actionbar_done).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // "Done"
                        save();
                    }
                });

        // Show the custom action bar view and hide the normal Home icon and title.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM, ActionBar.DISPLAY_SHOW_CUSTOM
                | ActionBar.DISPLAY_SHOW_HOME | ActionBar.DISPLAY_SHOW_TITLE);
        actionBar.setCustomView(customActionBarView);

        setContentView(R.layout.api_app_settings_activity);

        settingsFragment = (AppSettingsFragment) getSupportFragmentManager().findFragmentById(
                R.id.api_app_settings_fragment);

        Intent intent = getIntent();
        mAppUri = intent.getData();
        if (mAppUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of app!");
            finish();
            return;
        } else {
            Log.d(Constants.TAG, "uri: " + mAppUri);
            loadData(mAppUri);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.api_app_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.menu_api_settings_revoke:
            revokeAccess();
            return true;
        case R.id.menu_api_settings_cancel:
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadData(Uri appUri) {
        AppSettings settings = ProviderHelper.getApiAppSettings(this, appUri);
        settingsFragment.setAppSettings(settings);
    }

    private void revokeAccess() {
        if (getContentResolver().delete(mAppUri, null, null) <= 0) {
            throw new RuntimeException();
        }
        finish();
    }

    private void save() {
        ProviderHelper.updateApiApp(this, settingsFragment.getAppSettings(), mAppUri);

        finish();
    }

}

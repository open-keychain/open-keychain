package org.sufficientlysecure.keychain.remote_api;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.util.Log;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class AppSettingsActivity extends SherlockFragmentActivity {
    private PackageManager pm;

    // model
    Uri appUri;
    String packageName;
    long keyId;

    // model, derived
    String appName;

    // view
    TextView selectedKey;
    Button selectKeyButton;
    CheckBox asciiArmorCheckBox;
    Button saveButton;
    Button revokeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pm = getApplicationContext().getPackageManager();

        // BEGIN_INCLUDE (inflate_set_custom_view)
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
        // END_INCLUDE (inflate_set_custom_view)

        setContentView(R.layout.api_app_settings_activity);

        selectedKey = (TextView) findViewById(R.id.api_app_settings_selected_key);
        selectKeyButton = (Button) findViewById(R.id.api_app_settings_select_key_button);
        asciiArmorCheckBox = (CheckBox) findViewById(R.id.api_app_ascii_armor);

        Intent intent = getIntent();
        appUri = intent.getData();
        if (appUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of app!");
            finish();
            return;
        } else {
            Log.d(Constants.TAG, "uri: " + appUri);
            loadData(appUri);
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
        Cursor cur = getContentResolver().query(appUri, null, null, null, null);
        if (cur.moveToFirst()) {
            packageName = cur.getString(cur.getColumnIndex(KeychainContract.ApiApps.PACKAGE_NAME));
            // get application name
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);

                appName = (String) pm.getApplicationLabel(ai);
            } catch (final NameNotFoundException e) {
                appName = getString(R.string.api_unknown_app);
            }
            setTitle(appName);

            try {
                boolean asciiArmor = (cur.getInt(cur
                        .getColumnIndexOrThrow(KeychainContract.ApiApps.ASCII_ARMOR)) == 1);
                asciiArmorCheckBox.setChecked(asciiArmor);

            } catch (IllegalArgumentException e) {
                Log.e(Constants.TAG, "AppSettingsActivity", e);
            }
        }
    }

    private void revokeAccess() {
        if (getContentResolver().delete(appUri, null, null) <= 0) {
            throw new RuntimeException();
        }
        finish();
    }

    private void save() {
        final ContentValues cv = new ContentValues();
        // cv.put(KeychainContract.ApiApps.PACKAGE_NAME, packageName);
        cv.put(KeychainContract.ApiApps.ASCII_ARMOR, asciiArmorCheckBox.isChecked());
        // TODO: other parameters
        if (getContentResolver().update(appUri, cv, null, null) <= 0) {
            throw new RuntimeException();
        }

        finish();
    }

}

package org.sufficientlysecure.keychain.remote_api;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.util.Log;

import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class AppSettingsActivity extends SherlockFragmentActivity {

    private PackageManager pm;

    long id;

    String packageName;
    long keyId;
    boolean asciiArmor;

    // derived
    String appName;

    // view
    TextView selectedKey;
    Button selectKeyButton;
    CheckBox asciiArmorCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        pm = getApplicationContext().getPackageManager();

        setContentView(R.layout.api_app_settings_activity);

        selectedKey = (TextView) findViewById(R.id.api_app_settings_selected_key);
        selectKeyButton = (Button) findViewById(R.id.api_app_settings_select_key_button);
        asciiArmorCheckBox = (CheckBox) findViewById(R.id.api_app_ascii_armor);

        Intent intent = getIntent();
        Uri appUri = intent.getData();
        if (appUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of app!");
            finish();
            return;
        } else {
            Log.d(Constants.TAG, "uri: " + appUri);
            loadData(appUri);
        }
    }

    private void loadData(Uri appUri) {
        Cursor cur = getContentResolver().query(appUri, null, null, null, null);
        id = ContentUris.parseId(appUri);
        if (cur.moveToFirst()) {
            do {
                packageName = cur.getString(cur
                        .getColumnIndex(KeychainContract.CryptoConsumers.PACKAGE_NAME));
                // get application name
                try {
                    ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);

                    appName = (String) pm.getApplicationLabel(ai);
                } catch (final NameNotFoundException e) {
                    appName = getString(R.string.api_unknown_app);
                }
//                asciiArmor = (cur.getInt(cur
//                        .getColumnIndex(KeychainContract.CryptoConsumers.ASCII_ARMOR)) == 1);

                // display values
//                asciiArmorCheckBox.setChecked(asciiArmor);

            } while (cur.moveToNext());
        }

    }

}

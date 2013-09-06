package org.sufficientlysecure.keychain.remote_api;

import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.PgpHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.SelectSecretKeyActivity;
import org.sufficientlysecure.keychain.util.Log;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class AppSettingsActivity extends SherlockFragmentActivity {
    private PackageManager pm;

    // model
    Uri mAppUri;
    String mPackageName;
    long mSecretKeyId = Id.key.none;

    // view
    TextView appNameView;
    ImageView appIconView;
    TextView keyUserId;
    TextView keyUserIdRest;
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

        appNameView = (TextView) findViewById(R.id.api_app_settings_app_name);
        appIconView = (ImageView) findViewById(R.id.api_app_settings_app_icon);
        keyUserId = (TextView) findViewById(R.id.api_app_settings_user_id);
        keyUserIdRest = (TextView) findViewById(R.id.api_app_settings_user_id_rest);
        selectKeyButton = (Button) findViewById(R.id.api_app_settings_select_key_button);
        asciiArmorCheckBox = (CheckBox) findViewById(R.id.api_app_ascii_armor);

        selectKeyButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                selectSecretKey();
            }
        });

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
        Cursor cur = getContentResolver().query(appUri, null, null, null, null);
        if (cur.moveToFirst()) {
            mPackageName = cur.getString(cur.getColumnIndex(KeychainContract.ApiApps.PACKAGE_NAME));

            // get application name and icon from package manager
            String appName = null;
            Drawable appIcon = null;
            try {
                ApplicationInfo ai = pm.getApplicationInfo(mPackageName, 0);

                appName = (String) pm.getApplicationLabel(ai);
                appIcon = pm.getApplicationIcon(ai);
            } catch (final NameNotFoundException e) {
                appName = getString(R.string.api_unknown_app);
            }
            appNameView.setText(appName);
            appIconView.setImageDrawable(appIcon);

            try {
                mSecretKeyId = (cur.getLong(cur
                        .getColumnIndexOrThrow(KeychainContract.ApiApps.KEY_ID)));
                Log.d(Constants.TAG, "mSecretKeyId: " + mSecretKeyId);
                updateSelectedKeyView(mSecretKeyId);

                boolean asciiArmor = (cur.getInt(cur
                        .getColumnIndexOrThrow(KeychainContract.ApiApps.ASCII_ARMOR)) == 1);
                asciiArmorCheckBox.setChecked(asciiArmor);

            } catch (IllegalArgumentException e) {
                Log.e(Constants.TAG, "AppSettingsActivity", e);
            }
        }
    }

    private void revokeAccess() {
        if (getContentResolver().delete(mAppUri, null, null) <= 0) {
            throw new RuntimeException();
        }
        finish();
    }

    private void save() {
        final ContentValues cv = new ContentValues();
        cv.put(KeychainContract.ApiApps.KEY_ID, mSecretKeyId);

        cv.put(KeychainContract.ApiApps.ASCII_ARMOR, asciiArmorCheckBox.isChecked());
        // TODO: other parameters

        if (getContentResolver().update(mAppUri, cv, null, null) <= 0) {
            throw new RuntimeException();
        }

        finish();
    }

    private void selectSecretKey() {
        Intent intent = new Intent(this, SelectSecretKeyActivity.class);
        startActivityForResult(intent, Id.request.secret_keys);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

        case Id.request.secret_keys: {
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                mSecretKeyId = bundle.getLong(SelectSecretKeyActivity.RESULT_EXTRA_MASTER_KEY_ID);
            } else {
                mSecretKeyId = Id.key.none;
            }
            updateSelectedKeyView(mSecretKeyId);
            break;
        }

        default: {
            break;
        }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void updateSelectedKeyView(long secretKeyId) {
        if (secretKeyId == Id.key.none) {
            keyUserId.setText(R.string.api_settings_no_key);
            keyUserIdRest.setText("");
        } else {
            String uid = getResources().getString(R.string.unknownUserId);
            String uidExtra = "";
            PGPSecretKeyRing keyRing = ProviderHelper.getPGPSecretKeyRingByMasterKeyId(this,
                    secretKeyId);
            if (keyRing != null) {
                PGPSecretKey key = PgpHelper.getMasterKey(keyRing);
                if (key != null) {
                    String userId = PgpHelper.getMainUserIdSafe(this, key);
                    String chunks[] = userId.split(" <", 2);
                    uid = chunks[0];
                    if (chunks.length > 1) {
                        uidExtra = "<" + chunks[1];
                    }
                }
            }
            keyUserId.setText(uid);
            keyUserIdRest.setText(uidExtra);
        }
    }

}

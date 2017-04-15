package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;

public class PrivateKeyExportActivity extends BaseActivity {
    public static String EXTRA_MASTER_KEY_ID = "master_key_id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // noinspection ConstantConditions, we know this activity has an action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            long masterKeyId = intent.getLongExtra(EXTRA_MASTER_KEY_ID, 0);

            Fragment frag = PrivateKeyExportFragment.newInstance(masterKeyId);

            FragmentManager fragMan = getSupportFragmentManager();
            fragMan.beginTransaction()
                    .setCustomAnimations(0, 0)
                    .replace(R.id.content_frame, frag)
                    .commit();
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.private_key_import_export_activity);
    }
}

package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;

public class PrivateKeyImportExportActivity extends BaseActivity {
    public static String EXTRA_IMPORT_KEY = "import_key";
    public static String EXTRA_MASTER_KEY_ID = "master_key_id";

    private static final String TAG_FRAG = "frag";
    private static final String KEY_FRAG = "key_frag";

    private Fragment mFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // noinspection ConstantConditions, we know this activity has an action bar
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            boolean importKey = intent.getBooleanExtra(EXTRA_IMPORT_KEY, true);
            long masterKeyId = intent.getLongExtra(EXTRA_MASTER_KEY_ID, 0);

            mFragment = importKey ? PrivateKeyImportFragment.newInstance() : PrivateKeyExportFragment.newInstance(masterKeyId);

            FragmentManager fragMan = getSupportFragmentManager();
            fragMan.beginTransaction()
                    .setCustomAnimations(0, 0)
                    .replace(R.id.content_frame, mFragment)
                    .commit();
        }

//        if (savedInstanceState == null) {
//            mFragment = importKey ? PrivateKeyImportFragment.newInstance() : PrivateKeyExportFragment.newInstance(masterKeyId);
//        } else {
//            mFragment = getSupportFragmentManager().getFragment(savedInstanceState, KEY_FRAG);
//        }
//
//        getSupportFragmentManager().beginTransaction()
//                .replace(R.id.import_export_private_key_container, mFragment, TAG_FRAG)
//                .commit();
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.private_key_import_export_activity);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //getSupportFragmentManager().putFragment(outState, KEY_FRAG, mFragment);
    }
}

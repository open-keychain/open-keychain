package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

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

        setFullScreenDialogClose(Activity.RESULT_CANCELED, true);

        boolean importKey = getIntent().getBooleanExtra(EXTRA_IMPORT_KEY, true);
        long masterKeyId = getIntent().getLongExtra(EXTRA_MASTER_KEY_ID, 0);

        setTitle(importKey ? R.string.title_import_private_key : R.string.title_export_private_key);

        if (savedInstanceState == null) {
            mFragment = importKey ? PrivateKeyImportFragment.newInstance() : PrivateKeyExportFragment.newInstance(masterKeyId);
        } else {
            mFragment = getSupportFragmentManager().getFragment(savedInstanceState, KEY_FRAG);
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.import_export_private_key_container, mFragment, TAG_FRAG)
                .commit();
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.import_export_private_key_activity);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        getSupportFragmentManager().putFragment(outState, KEY_FRAG, mFragment);
    }
}

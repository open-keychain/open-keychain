package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;

public class PrivateKeyImportExportActivity extends BaseActivity {
    public static String EXTRA_IMPORT_KEY = "import_key";

    private static final String TAG_FRAG = "frag";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(Activity.RESULT_CANCELED, true);

        boolean importKey = getIntent().getBooleanExtra(EXTRA_IMPORT_KEY, true);

        setTitle(importKey ? R.string.title_import_private_key : R.string.title_export_private_key);

        Fragment fragment = importKey ? PrivateKeyImportFragment.newInstance() : PrivateKeyExportFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.import_export_private_key_container, fragment, TAG_FRAG)
                .commit();
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.import_export_private_key_activity);
    }
}

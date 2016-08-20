package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import org.sufficientlysecure.keychain.KeychainApplication;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.passphrasedialog.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.util.Passphrase;

public class AppLockActivity extends AppCompatActivity {
    private static final int REQUEST_FOR_PASSPHRASE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applock_activity);
    }

    @Override
    protected void onStart() {
        super.onStart();
        askForPassphrase();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void askForPassphrase() {
        Intent intent = new Intent(this, PassphraseDialogActivity.class);
        intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT,
                RequiredInputParcel.createRequiredAppLockPassphrase());
        startActivityForResult(intent, REQUEST_FOR_PASSPHRASE);

    }

    @Override
    public void onBackPressed() {
        ActivityCompat.finishAffinity(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_FOR_PASSPHRASE: {
                if (resultCode == RESULT_OK) {
                    finish();
                } else {
                    ActivityCompat.finishAffinity(this);
                }
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}

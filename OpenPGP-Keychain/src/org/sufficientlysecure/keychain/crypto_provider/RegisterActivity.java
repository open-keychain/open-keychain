package org.sufficientlysecure.keychain.crypto_provider;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class RegisterActivity extends Activity {

    public static final String ACTION_REGISTER = "com.android.crypto.REGISTER";

    public static final String EXTRA_PACKAGE_NAME = "packageName";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handleActions(getIntent());
    }

    protected void handleActions(Intent intent) {
        String action = intent.getAction();
        Bundle extras = intent.getExtras();

        if (extras == null) {
            extras = new Bundle();
        }

        final String callingPackageName = this.getCallingPackage();

        /**
         * com.android.crypto actions
         */
        if (ACTION_REGISTER.equals(action)) {
            setContentView(R.layout.register_crypto_consumer_activity);

            Button allowButton = (Button) findViewById(R.id.register_crypto_consumer_allow);
            Button disallowButton = (Button) findViewById(R.id.register_crypto_consumer_disallow);

            allowButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    ProviderHelper.addCryptoConsumer(RegisterActivity.this, callingPackageName);
                    Intent data = new Intent();
                    data.putExtra(EXTRA_PACKAGE_NAME, "org.sufficientlysecure.keychain");

                    setResult(RESULT_OK, data);
                    finish();
                }
            });

            disallowButton.setOnClickListener(new OnClickListener() {

                @Override
                public void onClick(View v) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });

        } else {
            Log.e(Constants.TAG, "Please use com.android.crypto.REGISTER as intent action!");
            finish();
        }
    }
}

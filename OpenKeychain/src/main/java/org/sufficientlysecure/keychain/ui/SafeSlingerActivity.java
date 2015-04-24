/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.NumberPicker;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;

import java.io.IOException;
import java.util.ArrayList;

import edu.cmu.cylab.starslinger.exchange.ExchangeActivity;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SafeSlingerActivity extends BaseActivity {

    private static final int REQUEST_CODE_SAFE_SLINGER = 211;

    public static final String EXTRA_MASTER_KEY_ID = "master_key_id";

    private long mMasterKeyId;
    private int mSelectedNumber = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mMasterKeyId = getIntent().getLongExtra(EXTRA_MASTER_KEY_ID, 0);

        NumberPicker picker = (NumberPicker) findViewById(R.id.safe_slinger_picker);
        picker.setMinValue(2);
        picker.setMaxValue(10);
        picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                mSelectedNumber = newVal;
            }
        });

        ImageView buttonIcon = (ImageView) findViewById(R.id.safe_slinger_button_image);
        buttonIcon.setColorFilter(getResources().getColor(R.color.tertiary_text_light),
                PorterDuff.Mode.SRC_IN);

        View button = findViewById(R.id.safe_slinger_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExchange(mMasterKeyId, mSelectedNumber);
            }
        });
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.safe_slinger_activity);
    }

    private void startExchange(long masterKeyId, int number) {
        // retrieve public key blob and start SafeSlinger
        Uri uri = KeychainContract.KeyRingData.buildPublicKeyRingUri(masterKeyId);
        try {
            byte[] keyBlob = (byte[]) new ProviderHelper(this).getGenericData(
                    uri, KeychainContract.KeyRingData.KEY_RING_DATA, ProviderHelper.FIELD_TYPE_BLOB);

            Intent slingerIntent = new Intent(this, ExchangeActivity.class);

            slingerIntent.putExtra(ExchangeConfig.extra.NUM_USERS, number);
            slingerIntent.putExtra(ExchangeConfig.extra.USER_DATA, keyBlob);
            slingerIntent.putExtra(ExchangeConfig.extra.HOST_NAME, Constants.SAFESLINGER_SERVER);
            startActivityForResult(slingerIntent, REQUEST_CODE_SAFE_SLINGER);
        } catch (ProviderHelper.NotFoundException e) {
            Log.e(Constants.TAG, "personal key not found", e);
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_SAFE_SLINGER) {
            if (resultCode == ExchangeActivity.RESULT_EXCHANGE_CANCELED) {
                return;
            }

            final FragmentActivity activity = SafeSlingerActivity.this;

            // Message is received after importing is done in KeychainIntentService
            ServiceProgressHandler saveHandler = new ServiceProgressHandler(
                    activity,
                    getString(R.string.progress_importing),
                    ProgressDialog.STYLE_HORIZONTAL,
                    true,
                    ProgressDialogFragment.ServiceType.KEYCHAIN_INTENT) {
                public void handleMessage(Message message) {
                    // handle messages by standard KeychainIntentServiceHandler first
                    super.handleMessage(message);

                    if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                        // get returned data bundle
                        Bundle returnData = message.getData();
                        if (returnData == null) {
                            return;
                        }
                        final ImportKeyResult result =
                                returnData.getParcelable(OperationResult.EXTRA_RESULT);
                        if (result == null) {
                            Log.e(Constants.TAG, "result == null");
                            return;
                        }

                        if (!result.success()) {
//                            result.createNotify(activity).show();
                            // only return if no success...
                            Intent data = new Intent();
                            data.putExtras(returnData);
                            setResult(RESULT_OK, data);
                            finish();
                            return;
                        }

//                        if (mExchangeMasterKeyId == null) {
//                            return;
//                        }

                        Intent certifyIntent = new Intent(activity, CertifyKeyActivity.class);
                        certifyIntent.putExtra(CertifyKeyActivity.EXTRA_RESULT, result);
                        certifyIntent.putExtra(CertifyKeyActivity.EXTRA_KEY_IDS, result.getImportedMasterKeyIds());
                        certifyIntent.putExtra(CertifyKeyActivity.EXTRA_CERTIFY_KEY_ID, mMasterKeyId);
                        startActivityForResult(certifyIntent, 0);

//                        mExchangeMasterKeyId = null;
                    }
                }
            };

            Log.d(Constants.TAG, "importKeys started");

            // Send all information needed to service to import key in other thread
            Intent intent = new Intent(activity, KeychainIntentService.class);

            intent.setAction(KeychainIntentService.ACTION_IMPORT_KEYRING);

            // instead of giving the entries by Intent extra, cache them into a
            // file to prevent Java Binder problems on heavy imports
            // read FileImportCache for more info.
            try {
                // import exchanged keys
                ArrayList<ParcelableKeyRing> it = getSlingedKeys(data.getExtras());

                // We parcel this iteratively into a file - anything we can
                // display here, we should be able to import.
                ParcelableFileCache<ParcelableKeyRing> cache =
                        new ParcelableFileCache<>(activity, "key_import.pcl");
                cache.writeCache(it.size(), it.iterator());

                // fill values for this action
                Bundle bundle = new Bundle();
                intent.putExtra(KeychainIntentService.EXTRA_DATA, bundle);

                // Create a new Messenger for the communication back
                Messenger messenger = new Messenger(saveHandler);
                intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

                // show progress dialog
                saveHandler.showProgressDialog(activity);

                // start service with intent
                activity.startService(intent);
            } catch (IOException e) {
                Log.e(Constants.TAG, "Problem writing cache file", e);
                Notify.create(activity, "Problem writing cache file!", Notify.Style.ERROR).show();
            }
        } else {
            // give everything else down to KeyListActivity!
            setResult(resultCode, data);
            finish();
        }
    }

    private static ArrayList<ParcelableKeyRing> getSlingedKeys(Bundle extras) {
        ArrayList<ParcelableKeyRing> list = new ArrayList<>();

        if (extras != null) {
            byte[] d;
            int i = 0;
            do {
                d = extras.getByteArray(ExchangeConfig.extra.MEMBER_DATA + i);
                if (d != null) {
                    list.add(new ParcelableKeyRing(d));
                    i++;
                }
            } while (d != null);
        }

        return list;
    }

}

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

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ImageView;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.results.GetKeyResult;
import org.sufficientlysecure.keychain.service.results.ImportKeyResult;
import org.sufficientlysecure.keychain.service.results.OperationResult;
import org.sufficientlysecure.keychain.ui.adapter.AsyncTaskResultWrapper;
import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListLoader;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.widget.ExchangeKeySpinner;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner;
import org.sufficientlysecure.keychain.util.InputData;
import org.sufficientlysecure.keychain.util.IntentIntegratorSupportV4;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableFileCache;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import edu.cmu.cylab.starslinger.exchange.ExchangeActivity;
import edu.cmu.cylab.starslinger.exchange.ExchangeConfig;

public class AddKeysActivity extends ActionBarActivity implements
        LoaderManager.LoaderCallbacks<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> {

    ExchangeKeySpinner mSafeSlingerKeySpinner;
    View mActionSafeSlinger;
    ImageView mActionSafeSlingerIcon;
    View mActionQrCode;
    View mActionNfc;
    View mActionSearchCloud;

    ProviderHelper mProviderHelper;

    long mExchangeMasterKeyId = Constants.key.none;

    byte[] mImportBytes;

    private static final int REQUEST_CODE_SAFE_SLINGER = 1;

    private static final int LOADER_ID_BYTES = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mProviderHelper = new ProviderHelper(this);

        setContentView(R.layout.add_keys_activity);

        mSafeSlingerKeySpinner = (ExchangeKeySpinner) findViewById(R.id.add_keys_safeslinger_key_spinner);
        mActionSafeSlinger = findViewById(R.id.add_keys_safeslinger);
        mActionSafeSlingerIcon = (ImageView) findViewById(R.id.add_keys_safeslinger_icon);
        // make certify image gray, like action icons
        mActionSafeSlingerIcon.setColorFilter(getResources().getColor(R.color.tertiary_text_light),
                PorterDuff.Mode.SRC_IN);
        mActionQrCode = findViewById(R.id.add_keys_qr_code);
        mActionNfc = findViewById(R.id.add_keys_nfc);
        mActionSearchCloud = findViewById(R.id.add_keys_search_cloud);

        mSafeSlingerKeySpinner.setOnKeyChangedListener(new KeySpinner.OnKeyChangedListener() {
            @Override
            public void onKeyChanged(long masterKeyId) {
                mExchangeMasterKeyId = masterKeyId;
            }
        });

        mActionSafeSlinger.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startExchange();
            }
        });

        mActionQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startQrCode();
            }
        });

        mActionNfc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // show nfc help
                Intent intent = new Intent(AddKeysActivity.this, HelpActivity.class);
                intent.putExtra(HelpActivity.EXTRA_SELECTED_TAB, HelpActivity.TAB_NFC);
                startActivityForResult(intent, 0);
            }
        });

        mActionSearchCloud.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchCloud();
            }
        });

    }

    private void startExchange() {
        if (mExchangeMasterKeyId == 0) {
            Notify.showNotify(this, getString(R.string.select_key_for_exchange),
                    Notify.Style.ERROR);
        } else {
            // retrieve public key blob and start SafeSlinger
            Uri uri = KeychainContract.KeyRingData.buildPublicKeyRingUri(mExchangeMasterKeyId);
            try {
                byte[] keyBlob = (byte[]) mProviderHelper.getGenericData(
                        uri, KeychainContract.KeyRingData.KEY_RING_DATA, ProviderHelper.FIELD_TYPE_BLOB);

                Intent slingerIntent = new Intent(this, ExchangeActivity.class);
                slingerIntent.putExtra(ExchangeConfig.extra.USER_DATA, keyBlob);
                slingerIntent.putExtra(ExchangeConfig.extra.HOST_NAME, Constants.SAFESLINGER_SERVER);
                startActivityForResult(slingerIntent, REQUEST_CODE_SAFE_SLINGER);
            } catch (ProviderHelper.NotFoundException e) {
                Log.e(Constants.TAG, "personal key not found", e);
            }
        }
    }

    private void startQrCode() {
        // scan using xzing's Barcode Scanner
        new IntentIntegrator(this).initiateScan();
    }

    private void searchCloud() {
        finish();
        Intent importIntent = new Intent(this, ImportKeysActivity.class);
        startActivity(importIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // if a result has been returned, display a notify
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        } else {
            switch (requestCode) {
                case REQUEST_CODE_SAFE_SLINGER: {
                    switch (resultCode) {
                        case ExchangeActivity.RESULT_EXCHANGE_OK:
                            // import exchanged keys
                            mImportBytes = getSlingedKeys(data);
                            getSupportLoaderManager().restartLoader(LOADER_ID_BYTES, null, this);
                            break;
                        case ExchangeActivity.RESULT_EXCHANGE_CANCELED:
                            // do nothing
                            break;
                    }
                    break;
                }
                case IntentIntegratorSupportV4.REQUEST_CODE: {
                    IntentResult scanResult = IntentIntegratorSupportV4.parseActivityResult(requestCode,
                            resultCode, data);
                    if (scanResult != null && scanResult.getFormatName() != null) {
                        String scannedContent = scanResult.getContents();

                        Log.d(Constants.TAG, "scannedContent: " + scannedContent);

                        // look if it's fingerprint only
                        if (scannedContent.toLowerCase(Locale.ENGLISH).startsWith(Constants.FINGERPRINT_SCHEME)) {
                            importKeys(null, getFingerprintFromUri(Uri.parse(scanResult.getContents())));
                            return;
                        }

                        // is this a full key encoded as qr code?
                        if (scannedContent.startsWith("-----BEGIN PGP")) {
                            // TODO
//                            mImportActivity.loadCallback(new ImportKeysListFragment.BytesLoaderState(scannedContent.getBytes(), null));
                            return;
                        }

                        // fail...
                        Notify.showNotify(this, R.string.import_qr_code_wrong, Notify.Style.ERROR);
                    }

                    break;
                }
            }
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private String getFingerprintFromUri(Uri dataUri) {
        String fingerprint = dataUri.toString().split(":")[1].toLowerCase(Locale.ENGLISH);
        Log.d(Constants.TAG, "fingerprint: " + fingerprint);
        return fingerprint;
    }

    private static byte[] getSlingedKeys(Intent data) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        Bundle extras = data.getExtras();
        if (extras != null) {
            byte[] d;
            int i = 0;
            do {
                d = extras.getByteArray(ExchangeConfig.extra.MEMBER_DATA + i);
                if (d != null) {
                    try {
                        out.write(d);
                    } catch (IOException e) {
                        Log.e(Constants.TAG, "IOException", e);
                    }
                    i++;
                }
            } while (d != null);
        }

        return out.toByteArray();
    }

    @Override
    public Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_BYTES: {
                InputData inputData = new InputData(new ByteArrayInputStream(mImportBytes), mImportBytes.length);
                return new ImportKeysListLoader(this, inputData);
            }

            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> loader,
                               AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>> data) {
        Log.d(Constants.TAG, "data: " + data.getResult());

        GetKeyResult getKeyResult = (GetKeyResult) data.getOperationResult();

        LongSparseArray<ParcelableKeyRing> cachedKeyData = null;

        // TODO: Use parcels!!!!!!!!!!!!!!!
        switch (loader.getId()) {
            case LOADER_ID_BYTES:

                if (getKeyResult.success()) {
                    // No error
                    cachedKeyData = ((ImportKeysListLoader) loader).getParcelableRings();
                } else {
                    getKeyResult.createNotify(this).show();
                }

//                if (error == null) {
//                    // No error
//                    cachedKeyData = ((ImportKeysListLoader) loader).getParcelableRings();
//                    Log.d(Constants.TAG, "no error!:" + cachedKeyData);
//
//                } else if (error instanceof ImportKeysListLoader.NoValidKeysException) {
//                    Notify.showNotify(this, R.string.error_import_no_valid_keys, Notify.Style.ERROR);
//                } else if (error instanceof ImportKeysListLoader.NonPgpPartException) {
//                    Notify.showNotify(this,
//                            ((ImportKeysListLoader.NonPgpPartException) error).getCount() + " " + getResources().
//                                    getQuantityString(R.plurals.error_import_non_pgp_part,
//                                            ((ImportKeysListLoader.NonPgpPartException) error).getCount()),
//                            Notify.Style.OK
//                    );
//                } else {
//                    Notify.showNotify(this, R.string.error_generic_report_bug, Notify.Style.ERROR);
//                }
                break;


            default:
                break;
        }

        importKeys(cachedKeyData, null);
    }

    @Override
    public void onLoaderReset(Loader<AsyncTaskResultWrapper<ArrayList<ImportKeysListEntry>>> loader) {
        switch (loader.getId()) {
            case LOADER_ID_BYTES:
                // Clear the data in the adapter.
//                mAdapter.clear();
                break;
            default:
                break;
        }
    }

    public ParcelableFileCache.IteratorWithSize<ParcelableKeyRing>
    getSelectedData(final LongSparseArray<ParcelableKeyRing> keyData) {
        return new ParcelableFileCache.IteratorWithSize<ParcelableKeyRing>() {
            int i = 0;

            @Override
            public int getSize() {
                return keyData.size();
            }

            @Override
            public boolean hasNext() {
                return (i < getSize());
            }

            @Override
            public ParcelableKeyRing next() {
                // get the object by the key.
                ParcelableKeyRing key = keyData.valueAt(i);
                i++;
                return key;
            }

            @Override
            public void remove() {
                keyData.remove(i);
            }
        };
    }

    public void importKeys(final LongSparseArray<ParcelableKeyRing> keyData, String fingerprint) {
        // Message is received after importing is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                this,
                getString(R.string.progress_importing),
                ProgressDialog.STYLE_HORIZONTAL,
                true) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
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

                    finish();
                    Intent certifyIntent = new Intent(AddKeysActivity.this, MultiCertifyKeyActivity.class);
                    certifyIntent.putExtra(MultiCertifyKeyActivity.EXTRA_RESULT, result);
                    certifyIntent.putExtra(MultiCertifyKeyActivity.EXTRA_KEY_IDS, result.getImportedMasterKeyIds());
                    certifyIntent.putExtra(MultiCertifyKeyActivity.EXTRA_CERTIFY_KEY_ID, mExchangeMasterKeyId);
                    startActivity(certifyIntent);

                    result.createNotify(AddKeysActivity.this).show();
                }
            }
        };

        if (keyData != null) {
            Log.d(Constants.TAG, "importKeys started");

            // Send all information needed to service to import key in other thread
            Intent intent = new Intent(this, KeychainIntentService.class);

            intent.setAction(KeychainIntentService.ACTION_IMPORT_KEYRING);

            // fill values for this action
            Bundle data = new Bundle();

            // instead of giving the entries by Intent extra, cache them into a
            // file to prevent Java Binder problems on heavy imports
            // read FileImportCache for more info.
            try {
                // We parcel this iteratively into a file - anything we can
                // display here, we should be able to import.
                ParcelableFileCache<ParcelableKeyRing> cache =
                        new ParcelableFileCache<ParcelableKeyRing>(this, "key_import.pcl");
                cache.writeCache(getSelectedData(keyData));

                intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

                // Create a new Messenger for the communication back
                Messenger messenger = new Messenger(saveHandler);
                intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

                // show progress dialog
                saveHandler.showProgressDialog(this);

                // start service with intent
                startService(intent);
            } catch (IOException e) {
                Log.e(Constants.TAG, "Problem writing cache file", e);
                Notify.showNotify(this, "Problem writing cache file!", Notify.Style.ERROR);
            }
        } else if (fingerprint != null) {

            // search config
            Preferences prefs = Preferences.getPreferences(this);
            Preferences.CloudSearchPrefs cloudPrefs = new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());

            // Send all information needed to service to query keys in other thread
            Intent intent = new Intent(this, KeychainIntentService.class);

            intent.setAction(KeychainIntentService.ACTION_DOWNLOAD_AND_IMPORT_KEYS);

            // fill values for this action
            Bundle data = new Bundle();

            data.putString(KeychainIntentService.DOWNLOAD_KEY_SERVER, cloudPrefs.keyserver);

            final ImportKeysListEntry keyEntry = new ImportKeysListEntry();
            keyEntry.setFingerprintHex(fingerprint);
            keyEntry.setBitStrength(1337); // TODO: make optional!
            keyEntry.addOrigin(cloudPrefs.keyserver);
            ArrayList<ImportKeysListEntry> selectedEntries = new ArrayList<ImportKeysListEntry>();
            selectedEntries.add(keyEntry);

            data.putParcelableArrayList(KeychainIntentService.DOWNLOAD_KEY_LIST, selectedEntries);

            intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

            // Create a new Messenger for the communication back
            Messenger messenger = new Messenger(saveHandler);
            intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

            // show progress dialog
            saveHandler.showProgressDialog(this);

            // start service with intent
            startService(intent);
        } else {
            Notify.showNotify(this, R.string.error_nothing_import, Notify.Style.ERROR);
        }
    }
}

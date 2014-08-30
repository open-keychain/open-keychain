/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.OperationResultParcel;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.ui.widget.CertifyKeySpinner;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Notify;

import java.util.ArrayList;

public class CertifyKeyFragment extends LoaderFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private CertifyKeyActivity mActivity;

    private CheckBox mUploadKeyCheckbox;
    private Spinner mSelectKeyserverSpinner;
    private ScrollView mScrollView;
    ListView mUserIds;

    private TextView mInfoKeyId, mInfoPrimaryUserId, mInfoFingerprint;

    private CertifyKeySpinner mCertifyKeySpinner;

    private Uri mDataUri;
    private long mPubKeyId = Constants.key.none;
    private long mMasterKeyId = Constants.key.none;

    private UserIdsAdapter mUserIdsAdapter;

    static final String USER_IDS_SELECTION = UserIds.IS_REVOKED + " = 0";

    static final String[] KEYRING_PROJECTION =
            new String[]{
                    KeyRings._ID,
                    KeyRings.MASTER_KEY_ID,
                    KeyRings.FINGERPRINT,
                    KeyRings.USER_ID,
            };
    static final int INDEX_MASTER_KEY_ID = 1;
    static final int INDEX_FINGERPRINT = 2;
    static final int INDEX_USER_ID = 3;

    private static final int LOADER_ID_KEYRING = 0;
    private static final int LOADER_ID_USER_IDS = 1;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Start out with a progress indicator.
        setContentShown(false);

        mDataUri = mActivity.getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of key!");
            mActivity.finish();
            return;
        }
        Log.e(Constants.TAG, "uri: " + mDataUri);

        mUserIdsAdapter = new UserIdsAdapter(mActivity, null, 0, true);

        mUserIds.setAdapter(mUserIdsAdapter);
        mUserIds.setOnItemClickListener(mUserIdsAdapter);

        getLoaderManager().initLoader(LOADER_ID_KEYRING, null, this);
        getLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);

        // is this "the android way"?
        mActivity = (CertifyKeyActivity) getActivity();

        View view = inflater.inflate(R.layout.certify_key_fragment, getContainer());

        mCertifyKeySpinner = (CertifyKeySpinner) view.findViewById(R.id.certify_key_spinner);
        mSelectKeyserverSpinner = (Spinner) view.findViewById(R.id.upload_key_keyserver);
        mUploadKeyCheckbox = (CheckBox) view.findViewById(R.id.sign_key_upload_checkbox);
        mScrollView = (ScrollView) view.findViewById(R.id.certify_scroll_view);
        mUserIds = (ListView) view.findViewById(R.id.view_key_user_ids);

        mInfoKeyId = ((TextView) view.findViewById(R.id.key_id));
        mInfoPrimaryUserId = ((TextView) view.findViewById(R.id.main_user_id));
        mInfoFingerprint = ((TextView) view.findViewById(R.id.view_key_fingerprint));

        // make certify image gray, like action icons
        ImageView vActionCertifyImage =
                (ImageView) view.findViewById(R.id.certify_key_action_certify_image);
        vActionCertifyImage.setColorFilter(getResources().getColor(R.color.tertiary_text_light),
                PorterDuff.Mode.SRC_IN);

        mCertifyKeySpinner.setOnKeyChangedListener(new KeySpinner.OnKeyChangedListener() {
            @Override
            public void onKeyChanged(long masterKeyId) {
                mMasterKeyId = masterKeyId;
            }
        });

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(mActivity,
                android.R.layout.simple_spinner_item,
                Preferences.getPreferences(mActivity).getKeyServers()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSelectKeyserverSpinner.setAdapter(adapter);

        if (!mUploadKeyCheckbox.isChecked()) {
            mSelectKeyserverSpinner.setEnabled(false);
        } else {
            mSelectKeyserverSpinner.setEnabled(true);
        }

        mUploadKeyCheckbox.setOnCheckedChangeListener(new OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    mSelectKeyserverSpinner.setEnabled(false);
                } else {
                    mSelectKeyserverSpinner.setEnabled(true);
                }
            }
        });

        View vCertifyButton = view.findViewById(R.id.certify_key_certify_button);
        vCertifyButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mPubKeyId != 0) {
                    if (mMasterKeyId == 0) {
                        Notify.showNotify(mActivity, getString(R.string.select_key_to_certify),
                                Notify.Style.ERROR);
                        scrollUp();
                    } else {
                        initiateCertifying();
                    }
                }
            }
        });

        return root;
    }

    private void scrollUp() {
        mScrollView.post(new Runnable() {
            public void run() {
                mScrollView.fullScroll(ScrollView.FOCUS_UP);
            }
        });
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_KEYRING: {
                Uri uri = KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(mActivity, uri, KEYRING_PROJECTION, null, null, null);
            }
            case LOADER_ID_USER_IDS: {
                Uri uri = UserIds.buildUserIdsUri(mDataUri);
                return new CursorLoader(mActivity, uri,
                        UserIdsAdapter.USER_IDS_PROJECTION, USER_IDS_SELECTION, null, null);
            }
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case LOADER_ID_KEYRING:
                // the first key here is our master key
                if (data.moveToFirst()) {
                    mPubKeyId = data.getLong(INDEX_MASTER_KEY_ID);
                    mCertifyKeySpinner.setHiddenMasterKeyId(mPubKeyId);
                    String keyIdStr = PgpKeyHelper.convertKeyIdToHex(mPubKeyId);
                    mInfoKeyId.setText(keyIdStr);

                    String mainUserId = data.getString(INDEX_USER_ID);
                    mInfoPrimaryUserId.setText(mainUserId);

                    byte[] fingerprintBlob = data.getBlob(INDEX_FINGERPRINT);
                    String fingerprint = PgpKeyHelper.convertFingerprintToHex(fingerprintBlob);
                    mInfoFingerprint.setText(PgpKeyHelper.colorizeFingerprint(fingerprint));
                }
                break;
            case LOADER_ID_USER_IDS:
                mUserIdsAdapter.swapCursor(data);
                break;
        }
        setContentShown(true, isResumed());
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case LOADER_ID_USER_IDS:
                mUserIdsAdapter.swapCursor(null);
                break;
        }
    }

    /**
     * handles the UI bits of the signing process on the UI thread
     */
    private void initiateCertifying() {
        // get the user's passphrase for this key (if required)
        String passphrase;
        try {
            passphrase = PassphraseCacheService.getCachedPassphrase(mActivity, mMasterKeyId);
        } catch (PassphraseCacheService.KeyNotFoundException e) {
            Log.e(Constants.TAG, "Key not found!", e);
            mActivity.finish();
            return;
        }
        if (passphrase == null) {
            PassphraseDialogFragment.show(mActivity, mMasterKeyId,
                    new Handler() {
                        @Override
                        public void handleMessage(Message message) {
                            if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                                startCertifying();
                            }
                        }
                    }
            );
            // bail out; need to wait until the user has entered the passphrase before trying again
        } else {
            startCertifying();
        }
    }

    /**
     * kicks off the actual signing process on a background thread
     */
    private void startCertifying() {
        // Bail out if there is not at least one user id selected
        ArrayList<String> userIds = mUserIdsAdapter.getSelectedUserIds();
        if (userIds.isEmpty()) {
            Notify.showNotify(mActivity, "No identities selected!",
                    Notify.Style.ERROR);
            return;
        }

        // Send all information needed to service to sign key in other thread
        Intent intent = new Intent(mActivity, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_CERTIFY_KEYRING);

        // fill values for this action
        Bundle data = new Bundle();

        data.putLong(KeychainIntentService.CERTIFY_KEY_MASTER_KEY_ID, mMasterKeyId);
        data.putLong(KeychainIntentService.CERTIFY_KEY_PUB_KEY_ID, mPubKeyId);
        data.putStringArrayList(KeychainIntentService.CERTIFY_KEY_UIDS, userIds);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after signing is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(mActivity,
                getString(R.string.progress_certifying), ProgressDialog.STYLE_SPINNER) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {

//                    Notify.showNotify(CertifyKeyActivity.this, R.string.key_certify_success,
//                            Notify.Style.INFO);

                    OperationResultParcel result = new OperationResultParcel(OperationResultParcel.RESULT_OK, null);
                    Intent intent = new Intent();
                    intent.putExtra(OperationResultParcel.EXTRA_RESULT, result);
                    mActivity.setResult(CertifyKeyActivity.RESULT_OK, intent);
                    mActivity.finish();

                    // check if we need to send the key to the server or not
                    if (mUploadKeyCheckbox.isChecked()) {
                        // upload the newly signed key to the keyserver
                        uploadKey();
                    } else {
                        mActivity.setResult(CertifyKeyActivity.RESULT_OK);
                        mActivity.finish();
                    }
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(mActivity);

        // start service with intent
        mActivity.startService(intent);
    }

    private void uploadKey() {
        // Send all information needed to service to upload key in other thread
        Intent intent = new Intent(mActivity, KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_UPLOAD_KEYRING);

        // set data uri as path to keyring
        Uri blobUri = KeychainContract.KeyRingData.buildPublicKeyRingUri(mDataUri);
        intent.setData(blobUri);

        // fill values for this action
        Bundle data = new Bundle();

        String server = (String) mSelectKeyserverSpinner.getSelectedItem();
        data.putString(KeychainIntentService.UPLOAD_KEY_SERVER, server);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after uploading is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(mActivity,
                getString(R.string.progress_uploading), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    //Notify.showNotify(CertifyKeyActivity.this, R.string.key_send_success,
                    //Notify.Style.INFO);

                    OperationResultParcel result = new OperationResultParcel(OperationResultParcel.RESULT_OK, null);
                    Intent intent = new Intent();
                    intent.putExtra(OperationResultParcel.EXTRA_RESULT, result);
                    mActivity.setResult(CertifyKeyActivity.RESULT_OK, intent);
                    mActivity.finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(mActivity);

        // start service with intent
        mActivity.startService(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Intent viewIntent = NavUtils.getParentActivityIntent(mActivity);
                viewIntent.setData(KeyRings.buildGenericKeyRingUri(mDataUri));
                NavUtils.navigateUpTo(mActivity, viewIntent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

}

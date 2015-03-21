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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.PromoteKeyResult;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentService.IOType;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.FragAction;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity.NfcListenerFragment;
import org.sufficientlysecure.keychain.ui.dialog.DeleteFileDialogFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.widget.NameEditText;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;


public class CreateKeyYubiFragment extends Fragment implements NfcListenerFragment,
        LoaderManager.LoaderCallbacks<Cursor> {

    CreateKeyActivity mCreateKeyActivity;
    NameEditText mNameEdit;
    View mBackButton;
    View mNextButton;
    private TextView mUnknownFingerprint;

    public static final String ARGS_MASTER_KEY_ID = "master_key_id";
    private byte[] mScannedFingerprint;
    private long mScannedMasterKeyId;
    private ViewAnimator mAnimator;
    private TextView mFingerprint;
    private TextView mUserId;

    private YubiImportState mState = YubiImportState.SCAN;

    enum YubiImportState {
        SCAN, // waiting for scan
        UNKNOWN, // scanned unknown key (ready to import)
        BAD_FINGERPRINT, // scanned key, bad fingerprint
        IMPORTED, // imported key (ready to promote)
    }

    private static boolean isEditTextNotEmpty(Context context, EditText editText) {
        boolean output = true;
        if (editText.getText().length() == 0) {
            editText.setError(context.getString(R.string.create_key_empty));
            editText.requestFocus();
            output = false;
        } else {
            editText.setError(null);
        }

        return output;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.create_yubikey_fragment, container, false);

        mAnimator = (ViewAnimator) view.findViewById(R.id.create_yubikey_animator);

        mUnknownFingerprint = (TextView) view.findViewById(R.id.create_yubikey_unknown_fp);

        mFingerprint = (TextView) view.findViewById(R.id.create_yubikey_fingerprint);
        mUserId = (TextView) view.findViewById(R.id.create_yubikey_user_id);

        mBackButton = view.findViewById(R.id.create_key_back_button);
        mNextButton = view.findViewById(R.id.create_key_next_button);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCreateKeyActivity.loadFragment(null, FragAction.TO_LEFT);
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                nextClicked();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCreateKeyActivity = (CreateKeyActivity) getActivity();
    }

    @Override
    public void onNfcPerform() throws IOException {

        mScannedFingerprint = mCreateKeyActivity.nfcGetFingerprint(0);
        mScannedMasterKeyId = getKeyIdFromFingerprint(mScannedFingerprint);

        getLoaderManager().initLoader(0, null, this);

    }

    // These are the rows that we will retrieve.
    static final String[] UNIFIED_PROJECTION = new String[]{
            KeychainContract.KeyRings._ID,
            KeychainContract.KeyRings.MASTER_KEY_ID,
            KeychainContract.KeyRings.USER_ID,
            KeychainContract.KeyRings.IS_REVOKED,
            KeychainContract.KeyRings.IS_EXPIRED,
            KeychainContract.KeyRings.HAS_ANY_SECRET,
            KeychainContract.KeyRings.FINGERPRINT,
    };

    static final int INDEX_MASTER_KEY_ID = 1;
    static final int INDEX_USER_ID = 2;
    static final int INDEX_IS_REVOKED = 3;
    static final int INDEX_IS_EXPIRED = 4;
    static final int INDEX_HAS_ANY_SECRET = 5;
    static final int INDEX_FINGERPRINT = 6;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri baseUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(
                KeyRings.buildUnifiedKeyRingUri(mScannedMasterKeyId)
        );
        return new CursorLoader(getActivity(), baseUri, UNIFIED_PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {

            byte[] fingerprint = data.getBlob(INDEX_FINGERPRINT);
            if (!Arrays.equals(fingerprint, mScannedFingerprint)) {
                mState = YubiImportState.BAD_FINGERPRINT;
                Notify.create(getActivity(), "Fingerprint mismatch!", Style.ERROR);
                return;
            }

            showKey(data);

        } else {
            showUnknownKey();
        }
    }

    public void showUnknownKey() {
        String fp = KeyFormattingUtils.convertFingerprintToHex(mScannedFingerprint);
        mUnknownFingerprint.setText(KeyFormattingUtils.colorizeFingerprint(fp));

        mAnimator.setDisplayedChild(1);
        mState = YubiImportState.UNKNOWN;
    }

    public void showKey(Cursor data) {
        String userId = data.getString(INDEX_USER_ID);
        boolean hasSecret = data.getInt(INDEX_HAS_ANY_SECRET) != 0;

        String fp = KeyFormattingUtils.convertFingerprintToHex(mScannedFingerprint);
        mFingerprint.setText(KeyFormattingUtils.colorizeFingerprint(fp));

        mUserId.setText(userId);

        mAnimator.setDisplayedChild(2);
        mState = YubiImportState.IMPORTED;
    }


    private void nextClicked() {

        switch (mState) {
            case UNKNOWN:
                importKey();
                break;
            case IMPORTED:
                promoteKey();
                break;
        }

    }

    public void promoteKey() {

        // Message is received after decrypting is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity()) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    PromoteKeyResult result =
                            returnData.getParcelable(DecryptVerifyResult.EXTRA_RESULT);

                    result.createNotify(getActivity()).show();
                }

            }
        };

        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action

        intent.setAction(KeychainIntentService.ACTION_PROMOTE_KEYRING);

        Bundle data = new Bundle();
        data.putLong(KeychainIntentService.PROMOTE_MASTER_KEY_ID, mScannedMasterKeyId);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // start service with intent
        getActivity().startService(intent);

    }

    public void importKey() {

        // Message is received after decrypting is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity()) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    ImportKeyResult result =
                            returnData.getParcelable(DecryptVerifyResult.EXTRA_RESULT);

                    result.createNotify(getActivity()).show();
                }

            }
        };

        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action
        Bundle data = new Bundle();

        intent.setAction(KeychainIntentService.ACTION_IMPORT_KEYRING);

        String hexFp = KeyFormattingUtils.convertFingerprintToHex(mScannedFingerprint);
        ArrayList<ParcelableKeyRing> keyList = new ArrayList<>();
        keyList.add(new ParcelableKeyRing(hexFp, null, null));
        data.putParcelableArrayList(KeychainIntentService.IMPORT_KEY_LIST, keyList);

        {
            Preferences prefs = Preferences.getPreferences(getActivity());
            Preferences.CloudSearchPrefs cloudPrefs =
                    new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());
            data.putString(KeychainIntentService.IMPORT_KEY_SERVER, cloudPrefs.keyserver);
        }

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // start service with intent
        getActivity().startService(intent);

    }


    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    static long getKeyIdFromFingerprint(byte[] fingerprint) {
        ByteBuffer buf = ByteBuffer.wrap(fingerprint);
        // skip first 12 bytes of the fingerprint
        buf.position(12);
        // the last eight bytes are the key id (big endian, which is default order in ByteBuffer)
        return buf.getLong();
    }

}

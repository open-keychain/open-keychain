/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2013 Bahtiar 'kalkin' Gadimov
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

import java.util.ArrayList;
import java.util.Date;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.helper.ExportHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserIds;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.adapter.ViewKeyKeysAdapter;
import org.sufficientlysecure.keychain.ui.adapter.ViewKeyUserIdsAdapter;
import org.sufficientlysecure.keychain.ui.dialog.DeleteKeyDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.ShareNfcDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.ShareQrCodeDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.beardedhen.androidbootstrap.BootstrapButton;

@SuppressLint("NewApi")
public class ViewKeyActivity extends SherlockFragmentActivity implements CreateNdefMessageCallback,
        OnNdefPushCompleteCallback, LoaderManager.LoaderCallbacks<Cursor> {

    ExportHelper mExportHelper;

    private Uri mDataUri;

    private TextView mName;
    private TextView mEmail;
    private TextView mComment;
    private TextView mAlgorithm;
    private TextView mKeyId;
    private TextView mExpiry;
    private TextView mCreation;
    private TextView mFingerprint;
    private BootstrapButton mActionEncrypt;

    private ListView mUserIds;
    private ListView mKeys;

    // NFC
    private NfcAdapter mNfcAdapter;
    private byte[] mSharedKeyringBytes;
    private static final int NFC_SENT = 1;

    private static final int LOADER_ID_KEYRING = 0;
    private static final int LOADER_ID_USER_IDS = 1;
    private static final int LOADER_ID_KEYS = 2;
    private ViewKeyUserIdsAdapter mUserIdsAdapter;
    private ViewKeyKeysAdapter mKeysAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mExportHelper = new ExportHelper(this);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setIcon(android.R.color.transparent);
        getSupportActionBar().setHomeButtonEnabled(true);

        setContentView(R.layout.view_key_activity);

        mName = (TextView) findViewById(R.id.name);
        mEmail = (TextView) findViewById(R.id.email);
        mComment = (TextView) findViewById(R.id.comment);
        mKeyId = (TextView) findViewById(R.id.key_id);
        mAlgorithm = (TextView) findViewById(R.id.algorithm);
        mCreation = (TextView) findViewById(R.id.creation);
        mExpiry = (TextView) findViewById(R.id.expiry);
        mFingerprint = (TextView) findViewById(R.id.fingerprint);
        mActionEncrypt = (BootstrapButton) findViewById(R.id.action_encrypt);
        mUserIds = (ListView) findViewById(R.id.user_ids);
        mKeys = (ListView) findViewById(R.id.keys);

        Intent intent = getIntent();
        mDataUri = intent.getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of key!");
            finish();
            return;
        } else {
            Log.d(Constants.TAG, "uri: " + mDataUri);
            loadData(mDataUri);
            initNfc(mDataUri);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.key_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            Intent homeIntent = new Intent(this, KeyListPublicActivity.class);
            homeIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(homeIntent);
            return true;
        case R.id.menu_key_view_update:
            updateFromKeyserver(mDataUri);
            return true;
        case R.id.menu_key_view_sign:
            signKey(mDataUri);
            return true;
        case R.id.menu_key_view_export_keyserver:
            uploadToKeyserver(mDataUri);
            return true;
        case R.id.menu_key_view_export_file:
            mExportHelper.showExportKeysDialog(mDataUri, Id.type.public_key, Constants.path.APP_DIR
                    + "/pubexport.asc");
            return true;
        case R.id.menu_key_view_share_default_fingerprint:
            shareKey(mDataUri, true);
            return true;
        case R.id.menu_key_view_share_default:
            shareKey(mDataUri, false);
            return true;
        case R.id.menu_key_view_share_qr_code_fingerprint:
            shareKeyQrCode(mDataUri, true);
            return true;
        case R.id.menu_key_view_share_qr_code:
            shareKeyQrCode(mDataUri, false);
            return true;
        case R.id.menu_key_view_share_nfc:
            shareNfc();
            return true;
        case R.id.menu_key_view_share_clipboard:
            copyToClipboard(mDataUri);
            return true;
        case R.id.menu_key_view_delete: {
            // Message is received after key is deleted
            Handler returnHandler = new Handler() {
                @Override
                public void handleMessage(Message message) {
                    if (message.what == DeleteKeyDialogFragment.MESSAGE_OKAY) {
                        setResult(RESULT_CANCELED);
                        finish();
                    }
                }
            };

            mExportHelper.deleteKey(mDataUri, Id.type.public_key, returnHandler);
            return true;
        }
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadData(Uri dataUri) {
        mActionEncrypt.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO: don't get object here!!! solve this differently!
                PGPPublicKeyRing ring = (PGPPublicKeyRing) ProviderHelper.getPGPKeyRing(
                        ViewKeyActivity.this, mDataUri);
                PGPPublicKey publicKey = ring.getPublicKey();

                long[] encryptionKeyIds = new long[] { publicKey.getKeyID() };
                Intent intent = new Intent(ViewKeyActivity.this, EncryptActivity.class);
                intent.setAction(EncryptActivity.ACTION_ENCRYPT);
                intent.putExtra(EncryptActivity.EXTRA_ENCRYPTION_KEY_IDS, encryptionKeyIds);
                // used instead of startActivity set actionbar based on callingPackage
                startActivityForResult(intent, 0);
            }
        });

        mUserIdsAdapter = new ViewKeyUserIdsAdapter(this, null, 0);

        mUserIds.setAdapter(mUserIdsAdapter);
        // mUserIds.setEmptyView(findViewById(android.R.id.empty));
        // mUserIds.setClickable(true);
        // mUserIds.setOnItemClickListener(new AdapterView.OnItemClickListener() {
        // @Override
        // public void onItemClick(AdapterView<?> arg0, View arg1, int position, long id) {
        // }
        // });

        mKeysAdapter = new ViewKeyKeysAdapter(this, null, 0);

        mKeys.setAdapter(mKeysAdapter);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getSupportLoaderManager().initLoader(LOADER_ID_KEYRING, null, this);
        getSupportLoaderManager().initLoader(LOADER_ID_USER_IDS, null, this);
        getSupportLoaderManager().initLoader(LOADER_ID_KEYS, null, this);
    }

    static final String[] KEYRING_PROJECTION = new String[] { KeyRings._ID, KeyRings.MASTER_KEY_ID,
            UserIds.USER_ID };
    static final int KEYRING_INDEX_ID = 0;
    static final int KEYRING_INDEX_MASTER_KEY_ID = 1;
    static final int KEYRING_INDEX_USER_ID = 2;

    static final String[] USER_IDS_PROJECTION = new String[] { UserIds._ID, UserIds.USER_ID,
            UserIds.RANK, };
    // not the main user id
    static final String USER_IDS_SELECTION = UserIds.RANK + " > 0 ";
    static final String USER_IDS_SORT_ORDER = UserIds.USER_ID + " COLLATE LOCALIZED ASC";

    static final String[] KEYS_PROJECTION = new String[] { Keys._ID, Keys.KEY_ID,
            Keys.IS_MASTER_KEY, Keys.ALGORITHM, Keys.KEY_SIZE, Keys.CAN_CERTIFY, Keys.CAN_SIGN,
            Keys.CAN_ENCRYPT, Keys.CREATION, Keys.EXPIRY };
    static final String KEYS_SORT_ORDER = Keys.RANK + " ASC";
    static final int KEYS_INDEX_ID = 0;
    static final int KEYS_INDEX_KEY_ID = 1;
    static final int KEYS_INDEX_IS_MASTER_KEY = 2;
    static final int KEYS_INDEX_ALGORITHM = 3;
    static final int KEYS_INDEX_KEY_SIZE = 4;
    static final int KEYS_INDEX_CAN_CERTIFY = 5;
    static final int KEYS_INDEX_CAN_SIGN = 6;
    static final int KEYS_INDEX_CAN_ENCRYPT = 7;
    static final int KEYS_INDEX_CREATION = 8;
    static final int KEYS_INDEX_EXPIRY = 9;

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
        case LOADER_ID_KEYRING: {
            Uri baseUri = mDataUri;

            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(this, baseUri, KEYRING_PROJECTION, null, null, null);
        }
        case LOADER_ID_USER_IDS: {
            Uri baseUri = UserIds.buildUserIdsUri(mDataUri);

            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(this, baseUri, USER_IDS_PROJECTION, USER_IDS_SELECTION, null,
                    USER_IDS_SORT_ORDER);
        }
        case LOADER_ID_KEYS: {
            Uri baseUri = Keys.buildKeysUri(mDataUri);

            // Now create and return a CursorLoader that will take care of
            // creating a Cursor for the data being displayed.
            return new CursorLoader(this, baseUri, KEYS_PROJECTION, null, null, KEYS_SORT_ORDER);
        }

        default:
            return null;
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        switch (loader.getId()) {
        case LOADER_ID_KEYRING:
            if (data.moveToFirst()) {
                // get name, email, and comment from USER_ID
                String[] mainUserId = PgpKeyHelper.splitUserId(data
                        .getString(KEYRING_INDEX_USER_ID));
                setTitle(mainUserId[0]);
                mName.setText(mainUserId[0]);
                mEmail.setText(mainUserId[1]);
                mComment.setText(mainUserId[2]);
            }

            break;
        case LOADER_ID_USER_IDS:
            mUserIdsAdapter.swapCursor(data);
            break;
        case LOADER_ID_KEYS:
            // the first key here is our master key
            if (data.moveToFirst()) {
                // get key id from MASTER_KEY_ID
                long keyId = data.getLong(KEYS_INDEX_KEY_ID);

                String keyIdStr = "0x" + PgpKeyHelper.convertKeyIdToHex(keyId);
                mKeyId.setText(keyIdStr);

                // get creation date from CREATION
                if (data.isNull(KEYS_INDEX_CREATION)) {
                    mCreation.setText(R.string.none);
                } else {
                    Date creationDate = new Date(data.getLong(KEYS_INDEX_CREATION) * 1000);

                    mCreation.setText(DateFormat.getDateFormat(getApplicationContext()).format(
                            creationDate));
                }

                // get creation date from EXPIRY
                if (data.isNull(KEYS_INDEX_EXPIRY)) {
                    mExpiry.setText(R.string.none);
                } else {
                    Date expiryDate = new Date(data.getLong(KEYS_INDEX_EXPIRY) * 1000);

                    mExpiry.setText(DateFormat.getDateFormat(getApplicationContext()).format(
                            expiryDate));
                }

                String algorithmStr = PgpKeyHelper.getAlgorithmInfo(
                        data.getInt(KEYS_INDEX_ALGORITHM), data.getInt(KEYS_INDEX_KEY_SIZE));
                mAlgorithm.setText(algorithmStr);

                // TODO: Can this be done better? fingerprint in db?
                String fingerprint = PgpKeyHelper.getFingerPrint(this, keyId);

                fingerprint = fingerprint.replace("  ", "\n");
                mFingerprint.setText(fingerprint);

                // TODO: get image with getUserAttributes() on key and then
                // PGPUserAttributeSubpacketVector
            }

            mKeysAdapter.swapCursor(data);
            break;

        default:
            break;
        }
    }

    /**
     * This is called when the last Cursor provided to onLoadFinished() above is about to be closed.
     * We need to make sure we are no longer using it.
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
        case LOADER_ID_KEYRING:
            // TODO?
            break;
        case LOADER_ID_USER_IDS:
            mUserIdsAdapter.swapCursor(null);
            break;
        case LOADER_ID_KEYS:
            mKeysAdapter.swapCursor(null);
            break;
        default:
            break;
        }
    }

    private void uploadToKeyserver(Uri dataUri) {
        long keyRingRowId = Long.valueOf(dataUri.getLastPathSegment());

        Intent uploadIntent = new Intent(this, KeyServerUploadActivity.class);
        uploadIntent.setAction(KeyServerUploadActivity.ACTION_EXPORT_KEY_TO_SERVER);
        uploadIntent.putExtra(KeyServerUploadActivity.EXTRA_KEYRING_ROW_ID, (int) keyRingRowId);
        startActivityForResult(uploadIntent, Id.request.export_to_server);
    }

    private void updateFromKeyserver(Uri dataUri) {
        long updateKeyId = 0;
        PGPPublicKeyRing updateRing = (PGPPublicKeyRing) ProviderHelper
                .getPGPKeyRing(this, dataUri);

        if (updateRing != null) {
            updateKeyId = PgpKeyHelper.getMasterKey(updateRing).getKeyID();
        }
        if (updateKeyId == 0) {
            Log.e(Constants.TAG, "this shouldn't happen. KeyId == 0!");
            return;
        }

        Intent queryIntent = new Intent(this, KeyServerQueryActivity.class);
        queryIntent.setAction(KeyServerQueryActivity.ACTION_LOOK_UP_KEY_ID_AND_RETURN);
        queryIntent.putExtra(KeyServerQueryActivity.EXTRA_KEY_ID, updateKeyId);

        // TODO: lookup??
        startActivityForResult(queryIntent, Id.request.look_up_key_id);
    }

    private void signKey(Uri dataUri) {
        long keyId = 0;
        PGPPublicKeyRing signKey = (PGPPublicKeyRing) ProviderHelper.getPGPKeyRing(this, dataUri);

        if (signKey != null) {
            keyId = PgpKeyHelper.getMasterKey(signKey).getKeyID();
        }
        if (keyId == 0) {
            Log.e(Constants.TAG, "this shouldn't happen. KeyId == 0!");
            return;
        }

        Intent signIntent = new Intent(this, SignKeyActivity.class);
        signIntent.putExtra(SignKeyActivity.EXTRA_KEY_ID, keyId);
        startActivity(signIntent);
    }

    private void shareKey(Uri dataUri, boolean fingerprintOnly) {
        String content = null;
        if (fingerprintOnly) {
            long masterKeyId = ProviderHelper.getMasterKeyId(this, dataUri);

            // TODO: dublicated in ShareQrCodeDialog
            content = "openpgp4fpr:";

            String fingerprint = PgpKeyHelper.convertKeyToHex(masterKeyId);

            content = content + fingerprint;
        } else {
            // get public keyring as ascii armored string
            long masterKeyId = ProviderHelper.getMasterKeyId(this, dataUri);
            ArrayList<String> keyringArmored = ProviderHelper.getKeyRingsAsArmoredString(this,
                    dataUri, new long[] { masterKeyId });

            content = keyringArmored.get(0);

            // Android will fail with android.os.TransactionTooLargeException if key is too big
            // see http://www.lonestarprod.com/?p=34
            if (content.length() >= 86389) {
                Toast.makeText(getApplicationContext(), R.string.key_too_big_for_sharing,
                        Toast.LENGTH_LONG).show();
                return;
            }
        }

        // let user choose application
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, content);
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent,
                getResources().getText(R.string.action_share_key_with)));
    }

    private void shareKeyQrCode(Uri dataUri, boolean fingerprintOnly) {
        ShareQrCodeDialogFragment dialog = ShareQrCodeDialogFragment.newInstance(dataUri,
                fingerprintOnly);
        dialog.show(getSupportFragmentManager(), "shareQrCodeDialog");
    }

    private void copyToClipboard(Uri dataUri) {
        // get public keyring as ascii armored string
        long masterKeyId = ProviderHelper.getMasterKeyId(this, dataUri);
        ArrayList<String> keyringArmored = ProviderHelper.getKeyRingsAsArmoredString(this, dataUri,
                new long[] { masterKeyId });

        ClipboardReflection.copyToClipboard(this, keyringArmored.get(0));
        Toast.makeText(getApplicationContext(), R.string.key_copied_to_clipboard, Toast.LENGTH_LONG)
                .show();
    }

    private void shareNfc() {
        ShareNfcDialogFragment dialog = ShareNfcDialogFragment.newInstance();
        dialog.show(getSupportFragmentManager(), "shareNfcDialog");
    }

    /**
     * NFC: Initialize NFC sharing if OS and device supports it
     */
    private void initNfc(Uri dataUri) {
        // check if NFC Beam is supported (>= Android 4.1)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            // Check for available NFC Adapter
            mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
            if (mNfcAdapter != null) {
                // init nfc

                // get public keyring as byte array
                long masterKeyId = ProviderHelper.getMasterKeyId(this, dataUri);
                mSharedKeyringBytes = ProviderHelper.getKeyRingsAsByteArray(this, dataUri,
                        new long[] { masterKeyId });

                // Register callback to set NDEF message
                mNfcAdapter.setNdefPushMessageCallback(this, this);
                // Register callback to listen for message-sent success
                mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
            }
        }
    }

    /**
     * NFC: Implementation for the CreateNdefMessageCallback interface
     */
    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        /**
         * When a device receives a push with an AAR in it, the application specified in the AAR is
         * guaranteed to run. The AAR overrides the tag dispatch system. You can add it back in to
         * guarantee that this activity starts when receiving a beamed message. For now, this code
         * uses the tag dispatch system.
         */
        NdefMessage msg = new NdefMessage(NdefRecord.createMime(Constants.NFC_MIME,
                mSharedKeyringBytes), NdefRecord.createApplicationRecord(Constants.PACKAGE_NAME));
        return msg;
    }

    /**
     * NFC: Implementation for the OnNdefPushCompleteCallback interface
     */
    @Override
    public void onNdefPushComplete(NfcEvent arg0) {
        // A handler is needed to send messages to the activity when this
        // callback occurs, because it happens from a binder thread
        mNfcHandler.obtainMessage(NFC_SENT).sendToTarget();
    }

    /**
     * NFC: This handler receives a message from onNdefPushComplete
     */
    private final Handler mNfcHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case NFC_SENT:
                Toast.makeText(getApplicationContext(), R.string.nfc_successfull, Toast.LENGTH_LONG)
                        .show();
                break;
            }
        }
    };

}

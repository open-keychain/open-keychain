/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.dialog.ShareNfcDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.ShareQrCodeDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

import android.annotation.SuppressLint;
import android.content.Intent;
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
import android.text.format.DateFormat;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

@SuppressLint("NewApi")
public class KeyViewActivity extends KeyActivity implements CreateNdefMessageCallback,
        OnNdefPushCompleteCallback {
    private Uri mDataUri;

    private PGPPublicKey mPublicKey;

    private TextView mAlgorithm;
    private TextView mFingerint;
    private TextView mExpiry;
    private TextView mCreation;

    // NFC
    private NfcAdapter mNfcAdapter;
    private byte[] mSharedKeyringBytes;
    private static final int NFC_SENT = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

        setContentView(R.layout.key_view_activity);

        mFingerint = (TextView) this.findViewById(R.id.fingerprint);
        mExpiry = (TextView) this.findViewById(R.id.expiry);
        mCreation = (TextView) this.findViewById(R.id.creation);
        mAlgorithm = (TextView) this.findViewById(R.id.algorithm);

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
            showExportKeysDialog(mDataUri, Id.type.public_key, Constants.path.APP_DIR
                    + "/pubexport.asc");
            return true;
        case R.id.menu_key_view_share_default:
            shareKey(mDataUri);
            return true;
        case R.id.menu_key_view_share_qr_code:
            shareKeyQrCode(mDataUri);
            return true;
        case R.id.menu_key_view_share_nfc:
            shareNfc();
            return true;
        case R.id.menu_key_view_share_clipboard:
            copyToClipboard(mDataUri);
            return true;
        case R.id.menu_key_view_delete:
            deleteKey(mDataUri, Id.type.public_key);
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void loadData(Uri dataUri) {
        PGPPublicKeyRing ring = (PGPPublicKeyRing) ProviderHelper.getPGPKeyRing(this, dataUri);
        mPublicKey = ring.getPublicKey();

        mFingerint.setText(PgpKeyHelper.shortifyFingerprint(PgpKeyHelper
                .convertFingerprintToHex(mPublicKey.getFingerprint())));
        String[] mainUserId = splitUserId("");

        Date expiryDate = PgpKeyHelper.getExpiryDate(mPublicKey);
        if (expiryDate == null) {
            mExpiry.setText("");
        } else {
            mExpiry.setText(DateFormat.getDateFormat(getApplicationContext()).format(expiryDate));
        }

        mCreation.setText(DateFormat.getDateFormat(getApplicationContext()).format(
                PgpKeyHelper.getCreationDate(mPublicKey)));
        mAlgorithm.setText(PgpKeyHelper.getAlgorithmInfo(mPublicKey));
    }

    /**
     * TODO: does this duplicate functionality from elsewhere? put in helper!
     */
    private String[] splitUserId(String userId) {
        String[] result = new String[] { "", "", "" };
        Log.v("UserID", userId);

        Pattern withComment = Pattern.compile("^(.*) [(](.*)[)] <(.*)>$");
        Matcher matcher = withComment.matcher(userId);
        if (matcher.matches()) {
            result[0] = matcher.group(1);
            result[1] = matcher.group(2);
            result[2] = matcher.group(3);
            return result;
        }

        Pattern withoutComment = Pattern.compile("^(.*) <(.*)>$");
        matcher = withoutComment.matcher(userId);
        if (matcher.matches()) {
            result[0] = matcher.group(1);
            result[1] = matcher.group(2);
            return result;
        }
        return result;
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

        Intent signIntent = new Intent(this, SignKeyActivity.class);
        signIntent.putExtra(SignKeyActivity.EXTRA_KEY_ID, updateKeyId);
        startActivity(signIntent);

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

    private void shareKey(Uri dataUri) {
        // get public keyring as ascii armored string
        long masterKeyId = ProviderHelper.getMasterKeyId(this, dataUri);
        ArrayList<String> keyringArmored = ProviderHelper.getKeyRingsAsArmoredString(this, dataUri,
                new long[] { masterKeyId });

        // let user choose application
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, keyringArmored.get(0));
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent,
                getResources().getText(R.string.action_share_key_with)));
    }

    private void shareKeyQrCode(Uri dataUri) {
        // get public keyring as ascii armored string
        long masterKeyId = ProviderHelper.getMasterKeyId(this, dataUri);
        ArrayList<String> keyringArmored = ProviderHelper.getKeyRingsAsArmoredString(this, dataUri,
                new long[] { masterKeyId });

        ShareQrCodeDialogFragment dialog = ShareQrCodeDialogFragment.newInstance(keyringArmored
                .get(0));
        dialog.show(getSupportFragmentManager(), "shareQrCodeDialog");
    }

    private void copyToClipboard(Uri dataUri) {
        // get public keyring as ascii armored string
        long masterKeyId = ProviderHelper.getMasterKeyId(this, dataUri);
        ArrayList<String> keyringArmored = ProviderHelper.getKeyRingsAsArmoredString(this, dataUri,
                new long[] { masterKeyId });

        ClipboardReflection.copyToClipboard(this, keyringArmored.get(0));
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

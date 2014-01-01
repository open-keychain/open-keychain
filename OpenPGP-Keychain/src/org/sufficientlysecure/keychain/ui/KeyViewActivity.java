/*
 * Copyright (C) 2013 Bahtiar 'kalkin' Gadimov
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.DataUsageFeedback;
import android.text.format.DateFormat;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class KeyViewActivity extends SherlockActivity {
    private Uri mDataUri;

    private PGPPublicKey mPublicKey;

    private TextView mAlgorithm;
    private TextView mFingerint;
    private TextView mExpiry;
    private TextView mCreation;

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
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of app!");
            finish();
            return;
        } else {
            Log.d(Constants.TAG, "uri: " + mDataUri);
            loadData(mDataUri);
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
        // TODO: use data uri in the other activities instead of givin key ring row id!

        long keyRingRowId = Long.valueOf(mDataUri.getLastPathSegment());

        switch (item.getItemId()) {
        case R.id.menu_key_view_update:
            long updateKeyId = 0;
            PGPPublicKeyRing updateKeyRing = ProviderHelper.getPGPPublicKeyRingByRowId(this,
                    keyRingRowId);
            if (updateKeyRing != null) {
                updateKeyId = PgpKeyHelper.getMasterKey(updateKeyRing).getKeyID();
            }
            if (updateKeyId == 0) {
                // this shouldn't happen
                return true;
            }

            Intent queryIntent = new Intent(this, KeyServerQueryActivity.class);
            queryIntent.setAction(KeyServerQueryActivity.ACTION_LOOK_UP_KEY_ID_AND_RETURN);
            queryIntent.putExtra(KeyServerQueryActivity.EXTRA_KEY_ID, updateKeyId);

            // TODO: lookup??
            startActivityForResult(queryIntent, Id.request.look_up_key_id);

            return true;
        case R.id.menu_key_view_sign:
            long keyId = 0;
            PGPPublicKeyRing signKeyRing = ProviderHelper.getPGPPublicKeyRingByRowId(this,
                    keyRingRowId);
            if (signKeyRing != null) {
                keyId = PgpKeyHelper.getMasterKey(signKeyRing).getKeyID();
            }
            if (keyId == 0) {
                // this shouldn't happen
                return true;
            }

            Intent signIntent = new Intent(this, SignKeyActivity.class);
            signIntent.putExtra(SignKeyActivity.EXTRA_KEY_ID, keyId);
            startActivity(signIntent);
            return true;
        case R.id.menu_key_view_export_keyserver:
            Intent uploadIntent = new Intent(this, KeyServerUploadActivity.class);
            uploadIntent.setAction(KeyServerUploadActivity.ACTION_EXPORT_KEY_TO_SERVER);
            uploadIntent.putExtra(KeyServerUploadActivity.EXTRA_KEYRING_ROW_ID, (int) keyRingRowId);
            startActivityForResult(uploadIntent, Id.request.export_to_server);

            return true;
        case R.id.menu_key_view_export_file:
//            long masterKeyId = ProviderHelper.getPublicMasterKeyId(mKeyListActivity, keyRingRowId);
//            if (masterKeyId == -1) {
//                masterKeyId = ProviderHelper.getSecretMasterKeyId(mKeyListActivity, keyRingRowId);
//            }
//
//            mKeyListActivity.showExportKeysDialog(masterKeyId);
            return true;
        case R.id.menu_key_view_share:
            // get master key id using row id
            long masterKeyId3 = ProviderHelper.getPublicMasterKeyId(this, keyRingRowId);

            Intent shareIntent = new Intent(this, ShareActivity.class);
            shareIntent.setAction(ShareActivity.ACTION_SHARE_KEYRING);
            shareIntent.putExtra(ShareActivity.EXTRA_MASTER_KEY_ID, masterKeyId3);
            startActivityForResult(shareIntent, 0);

            return true;
        case R.id.menu_key_view_share_qr_code:
            // get master key id using row id
            long masterKeyId = ProviderHelper.getPublicMasterKeyId(this, keyRingRowId);

            Intent qrCodeIntent = new Intent(this, ShareActivity.class);
            qrCodeIntent.setAction(ShareActivity.ACTION_SHARE_KEYRING_WITH_QR_CODE);
            qrCodeIntent.putExtra(ShareActivity.EXTRA_MASTER_KEY_ID, masterKeyId);
            startActivityForResult(qrCodeIntent, 0);

            return true;
        case R.id.menu_key_view_share_nfc:
            // get master key id using row id
            long masterKeyId2 = ProviderHelper.getPublicMasterKeyId(this, keyRingRowId);

            Intent nfcIntent = new Intent(this, ShareNfcBeamActivity.class);
            nfcIntent.setAction(ShareNfcBeamActivity.ACTION_SHARE_KEYRING_WITH_NFC);
            nfcIntent.putExtra(ShareNfcBeamActivity.EXTRA_MASTER_KEY_ID, masterKeyId2);
            startActivityForResult(nfcIntent, 0);

            return true;
        case R.id.menu_key_view_delete:
//            mKeyListActivity.showDeleteKeyDialog(keyRingRowId);

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
}

/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 Senecaso
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.ui;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.widget.TextView;

import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpConversionHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Date;

/**
 * Swag
 */
public class ViewCertActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[] {
            KeychainContract.Certs._ID,
            KeychainContract.Certs.KEY_ID,
            KeychainContract.UserIds.USER_ID,
            KeychainContract.Certs.RANK,
            KeychainContract.Certs.CREATION,
            KeychainContract.Certs.KEY_ID_CERTIFIER,
            "signer_uid",
            KeychainContract.Certs.KEY_DATA
    };

    private Uri mDataUri;

    private TextView mSigneeKey, mSigneeUid, mRank, mAlgorithm, mType, mCreation, mExpiry;
    private TextView mSignerKey, mSignerUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.view_cert_activity);

        mSigneeKey = (TextView) findViewById(R.id.signee_key);
        mSigneeUid = (TextView) findViewById(R.id.signee_uid);
        mRank = (TextView) findViewById(R.id.subkey_rank);
        mAlgorithm = (TextView) findViewById(R.id.algorithm);
        mType = (TextView) findViewById(R.id.signature_type);
        mCreation = (TextView) findViewById(R.id.creation);
        mExpiry = (TextView) findViewById(R.id.expiry);

        mSignerKey = (TextView) findViewById(R.id.signer_key_id);
        mSignerUid = (TextView) findViewById(R.id.signer_uid);

        mDataUri = getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of key!");
            finish();
            return;
        }

        getSupportLoaderManager().initLoader(0, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, mDataUri, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if(data.moveToFirst()) {
            String signeeKey = "0x" + PgpKeyHelper.convertKeyIdToHex(data.getLong(1));
            mSigneeKey.setText(signeeKey);

            String signeeUid = data.getString(2);
            mSigneeUid.setText(signeeUid);

            String subkey_rank = Integer.toString(data.getInt(3));
            mRank.setText(subkey_rank);

            Date creationDate = new Date(data.getLong(4) * 1000);
            mCreation.setText(DateFormat.getDateFormat(getApplicationContext()).format(creationDate));

            String signerKey = "0x" + PgpKeyHelper.convertKeyIdToHex(data.getLong(5));
            mSignerKey.setText(signerKey);

            String signerUid = data.getString(6);
            if(signerUid != null)
                mSignerUid.setText(signerUid);
            else
                mSignerUid.setText(R.string.unknown_uid);

            byte[] sigData = data.getBlob(7);
            PGPSignature sig = PgpConversionHelper.BytesToPGPSignature(sigData);
            if(sig != null) {
                String algorithmStr = PgpKeyHelper.getAlgorithmInfo(sig.getKeyAlgorithm(), 0);
                mAlgorithm.setText(algorithmStr);

                switch(sig.getSignatureType()) {
                    case PGPSignature.DEFAULT_CERTIFICATION:
                        mType.setText(R.string.sig_type_default); break;
                    case PGPSignature.NO_CERTIFICATION:
                        mType.setText(R.string.sig_type_none); break;
                    case PGPSignature.CASUAL_CERTIFICATION:
                        mType.setText(R.string.sig_type_casual); break;
                    case PGPSignature.POSITIVE_CERTIFICATION:
                        mType.setText(R.string.sig_type_positive); break;
                }

                long expiry = sig.getHashedSubPackets().getSignatureExpirationTime();
                if(expiry == 0)
                    mExpiry.setText("never");
                else {
                    Date expiryDate = new Date(creationDate.getTime() + expiry * 1000);
                    mExpiry.setText(DateFormat.getDateFormat(getApplicationContext()).format(expiryDate));
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

}

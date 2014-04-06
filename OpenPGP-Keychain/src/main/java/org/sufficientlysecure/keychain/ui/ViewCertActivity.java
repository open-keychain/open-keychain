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

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.spongycastle.openpgp.PGPSignature;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpConversionHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Date;

/**
 * Swag
 */
public class ViewCertActivity extends ActionBarActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[] {
            Certs.MASTER_KEY_ID,
            Certs.USER_ID,
            Certs.TYPE,
            Certs.CREATION,
            Certs.EXPIRY,
            Certs.KEY_ID_CERTIFIER,
            Certs.SIGNER_UID,
    };
    private static final int INDEX_MASTER_KEY_ID = 0;
    private static final int INDEX_USER_ID = 1;
    private static final int INDEX_TYPE = 2;
    private static final int INDEX_CREATION = 3;
    private static final int INDEX_EXPIRY = 4;
    private static final int INDEX_KEY_ID_CERTIFIER = 5;
    private static final int INDEX_SIGNER_UID = 6;

    private Uri mDataUri;

    private long mSignerKeyId;

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
            String signeeKey = "0x" + PgpKeyHelper.convertKeyIdToHex(data.getLong(INDEX_MASTER_KEY_ID));
            mSigneeKey.setText(signeeKey);

            String signeeUid = data.getString(INDEX_USER_ID);
            mSigneeUid.setText(signeeUid);

            Date creationDate = new Date(data.getLong(INDEX_CREATION) * 1000);
            mCreation.setText(DateFormat.getDateFormat(getApplicationContext()).format(creationDate));

            mSignerKeyId = data.getLong(INDEX_KEY_ID_CERTIFIER);
            String signerKey = "0x" + PgpKeyHelper.convertKeyIdToHex(mSignerKeyId);
            mSignerKey.setText(signerKey);

            String signerUid = data.getString(INDEX_SIGNER_UID);
            if(signerUid != null)
                mSignerUid.setText(signerUid);
            else
                mSignerUid.setText(R.string.unknown_uid);

            // String algorithmStr = PgpKeyHelper.getAlgorithmInfo(sig.getKeyAlgorithm(), 0);
            // mAlgorithm.setText(algorithmStr);

            switch(data.getInt(INDEX_TYPE)) {
                case PGPSignature.DEFAULT_CERTIFICATION:
                    mType.setText(R.string.cert_default); break;
                case PGPSignature.NO_CERTIFICATION:
                    mType.setText(R.string.cert_none); break;
                case PGPSignature.CASUAL_CERTIFICATION:
                    mType.setText(R.string.cert_casual); break;
                case PGPSignature.POSITIVE_CERTIFICATION:
                    mType.setText(R.string.cert_positive); break;
                case PGPSignature.CERTIFICATION_REVOCATION:
                    mType.setText(R.string.cert_revoke); break;
            }

            long expiry = data.getLong(INDEX_EXPIRY);
            if(expiry == 0) {
                mExpiry.setText(R.string.never);
            } else {
                Date expiryDate = new Date(creationDate.getTime() + expiry * 1000);
                mExpiry.setText(DateFormat.getDateFormat(getApplicationContext()).format(expiryDate));
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.view_cert, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_view_cert_verify:
                // TODO verification routine
                return true;
            case R.id.menu_view_cert_view_signer:
                // can't do this before the data is initialized
                Intent viewIntent = null;
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                    viewIntent = new Intent(this, ViewKeyActivity.class);
                } else {
                    viewIntent = new Intent(this, ViewKeyActivityJB.class);
                }
                //
                long signerMasterKeyId = ProviderHelper.getMasterKeyId(this,
                        KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(Long.toString(mSignerKeyId))
                );
                // TODO notify user of this, maybe offer download?
                if(mSignerKeyId == 0L)
                    return true;
                viewIntent.setData(KeyRings.buildGenericKeyRingUri(
                        Long.toString(signerMasterKeyId))
                );
                startActivity(viewIntent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

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

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.NavUtils;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.pgp.exception.PgpKeyNotFoundException;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Date;

public class ViewCertActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    // These are the rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
            Certs.MASTER_KEY_ID,
            Certs.USER_ID,
            Certs.TYPE,
            Certs.CREATION,
            Certs.KEY_ID_CERTIFIER,
            Certs.SIGNER_UID,
            Certs.DATA,
    };
    private static final int INDEX_MASTER_KEY_ID = 0;
    private static final int INDEX_USER_ID = 1;
    private static final int INDEX_TYPE = 2;
    private static final int INDEX_CREATION = 3;
    private static final int INDEX_KEY_ID_CERTIFIER = 4;
    private static final int INDEX_SIGNER_UID = 5;
    private static final int INDEX_DATA = 6;

    private Uri mDataUri;

    private long mCertifierKeyId;

    private TextView mSigneeKey, mSigneeUid, mAlgorithm, mType, mReason, mCreation;
    private TextView mCertifierKey, mCertifierUid, mStatus;
    private View mRowReason;
    private View mViewCertifierButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mSigneeKey = (TextView) findViewById(R.id.signee_key);
        mSigneeUid = (TextView) findViewById(R.id.signee_uid);
        mAlgorithm = (TextView) findViewById(R.id.algorithm);
        mType = (TextView) findViewById(R.id.signature_type);
        mReason = (TextView) findViewById(R.id.reason);
        mCreation = (TextView) findViewById(R.id.creation);

        mCertifierKey = (TextView) findViewById(R.id.signer_key_id);
        mCertifierUid = (TextView) findViewById(R.id.signer_uid);

        mRowReason = findViewById(R.id.row_reason);

        mViewCertifierButton = findViewById(R.id.view_cert_view_cert_key);

        mDataUri = getIntent().getData();
        if (mDataUri == null) {
            Log.e(Constants.TAG, "Intent data missing. Should be Uri of key!");
            finish();
            return;
        }

        getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.view_cert_activity);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(this, mDataUri, PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (data.moveToFirst()) {
            mSigneeKey.setText(KeyFormattingUtils.beautifyKeyId(data.getLong(INDEX_MASTER_KEY_ID)));

            String signeeUid = data.getString(INDEX_USER_ID);
            mSigneeUid.setText(signeeUid);

            Date creationDate = new Date(data.getLong(INDEX_CREATION) * 1000);
            mCreation.setText(DateFormat.getDateFormat(getApplicationContext()).format(creationDate));

            mCertifierKeyId = data.getLong(INDEX_KEY_ID_CERTIFIER);
            mCertifierKey.setText(KeyFormattingUtils.beautifyKeyId(mCertifierKeyId));

            String certifierUid = data.getString(INDEX_SIGNER_UID);
            if (certifierUid != null) {
                mCertifierUid.setText(certifierUid);
            } else {
                mCertifierUid.setText(R.string.unknown_uid);
            }

            WrappedSignature sig = WrappedSignature.fromBytes(data.getBlob(INDEX_DATA));

            String algorithmStr = KeyFormattingUtils.getAlgorithmInfo(this, sig.getKeyAlgorithm(), null, null);
            mAlgorithm.setText(algorithmStr);

            mRowReason.setVisibility(View.GONE);
            switch (data.getInt(INDEX_TYPE)) {
                case WrappedSignature.DEFAULT_CERTIFICATION:
                    mType.setText(R.string.cert_default);
                    break;
                case WrappedSignature.NO_CERTIFICATION:
                    mType.setText(R.string.cert_none);
                    break;
                case WrappedSignature.CASUAL_CERTIFICATION:
                    mType.setText(R.string.cert_casual);
                    break;
                case WrappedSignature.POSITIVE_CERTIFICATION:
                    mType.setText(R.string.cert_positive);
                    break;
                case WrappedSignature.CERTIFICATION_REVOCATION: {
                    mType.setText(R.string.cert_revoke);
                    try {
                        if (! TextUtils.isEmpty(sig.getRevocationReason())) {
                            mReason.setText(sig.getRevocationReason());
                        } else {
                            mReason.setText(R.string.none);
                        }
                    } catch (PgpGeneralException e) {
                        mReason.setText(R.string.none);
                    }
                    mRowReason.setVisibility(View.VISIBLE);
                    break;
                }
            }
        }

        // can't do this before the data is initialized
        mViewCertifierButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent viewIntent = new Intent(ViewCertActivity.this, ViewKeyActivity.class);

                try {
                    ProviderHelper providerHelper = new ProviderHelper(ViewCertActivity.this);
                    long signerMasterKeyId = providerHelper.mReader.getCachedPublicKeyRing(
                            KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(mCertifierKeyId)).getMasterKeyId();
                    viewIntent.setData(KeyRings.buildGenericKeyRingUri(signerMasterKeyId));
                    startActivity(viewIntent);
                } catch (PgpKeyNotFoundException e) {
                    // TODO notify user of this, maybe offer download?
                    Log.e(Constants.TAG, "key not found!", e);
                }
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                Intent viewIntent = NavUtils.getParentActivityIntent(this);
                viewIntent.setData(KeyRings.buildGenericKeyRingUri(mDataUri));
                NavUtils.navigateUpTo(this, viewIntent);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

}

/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.support.v7.app.ActionBarActivity;
import android.text.format.DateFormat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.spongycastle.bcpg.SignatureSubpacket;
import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.bcpg.sig.RevocationReason;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpConversionHelper;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.security.SignatureException;
import java.util.Date;

public class ViewCertActivity extends ActionBarActivity
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

    private long mSignerKeyId;

    private TextView mSigneeKey, mSigneeUid, mAlgorithm, mType, mRReason, mCreation;
    private TextView mSignerKey, mSignerUid, mStatus;
    private View mRowReason;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.view_cert_activity);

        mStatus = (TextView) findViewById(R.id.status);
        mSigneeKey = (TextView) findViewById(R.id.signee_key);
        mSigneeUid = (TextView) findViewById(R.id.signee_uid);
        mAlgorithm = (TextView) findViewById(R.id.algorithm);
        mType = (TextView) findViewById(R.id.signature_type);
        mRReason = (TextView) findViewById(R.id.reason);
        mCreation = (TextView) findViewById(R.id.creation);

        mSignerKey = (TextView) findViewById(R.id.signer_key_id);
        mSignerUid = (TextView) findViewById(R.id.signer_uid);

        mRowReason = findViewById(R.id.row_reason);

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
        if (data.moveToFirst()) {
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
            if (signerUid != null) {
                mSignerUid.setText(signerUid);
            } else {
                mSignerUid.setText(R.string.unknown_uid);
            }

            PGPSignature sig = PgpConversionHelper.BytesToPGPSignature(data.getBlob(INDEX_DATA));
            try {
                ProviderHelper providerHelper = new ProviderHelper(this);
                PGPKeyRing signeeRing = providerHelper.getPGPKeyRing(
                        KeychainContract.KeyRingData.buildPublicKeyRingUri(
                                Long.toString(data.getLong(INDEX_MASTER_KEY_ID))));
                PGPKeyRing signerRing = providerHelper.getPGPKeyRing(
                        KeychainContract.KeyRingData.buildPublicKeyRingUri(
                                Long.toString(sig.getKeyID())));

                try {
                    sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider(
                            Constants.BOUNCY_CASTLE_PROVIDER_NAME), signerRing.getPublicKey());
                    if (sig.verifyCertification(signeeUid, signeeRing.getPublicKey())) {
                        mStatus.setText(R.string.cert_verify_ok);
                        mStatus.setTextColor(getResources().getColor(R.color.bbutton_success));
                    } else {
                        mStatus.setText(R.string.cert_verify_failed);
                        mStatus.setTextColor(getResources().getColor(R.color.alert));
                    }
                } catch (SignatureException e) {
                    mStatus.setText(R.string.cert_verify_error);
                    mStatus.setTextColor(getResources().getColor(R.color.alert));
                } catch (PGPException e) {
                    mStatus.setText(R.string.cert_verify_error);
                    mStatus.setTextColor(getResources().getColor(R.color.alert));
                }
            } catch (ProviderHelper.NotFoundException e) {
                mStatus.setText(R.string.cert_verify_unavailable);
                mStatus.setTextColor(getResources().getColor(R.color.black));
            }

            String algorithmStr = PgpKeyHelper.getAlgorithmInfo(this, sig.getKeyAlgorithm(), 0);
            mAlgorithm.setText(algorithmStr);

            mRowReason.setVisibility(View.GONE);
            switch (data.getInt(INDEX_TYPE)) {
                case PGPSignature.DEFAULT_CERTIFICATION:
                    mType.setText(R.string.cert_default);
                    break;
                case PGPSignature.NO_CERTIFICATION:
                    mType.setText(R.string.cert_none);
                    break;
                case PGPSignature.CASUAL_CERTIFICATION:
                    mType.setText(R.string.cert_casual);
                    break;
                case PGPSignature.POSITIVE_CERTIFICATION:
                    mType.setText(R.string.cert_positive);
                    break;
                case PGPSignature.CERTIFICATION_REVOCATION: {
                    mType.setText(R.string.cert_revoke);
                    if (sig.getHashedSubPackets().hasSubpacket(SignatureSubpacketTags.REVOCATION_REASON)) {
                        SignatureSubpacket p = sig.getHashedSubPackets().getSubpacket(
                                SignatureSubpacketTags.REVOCATION_REASON);
                        // For some reason, this is missing in SignatureSubpacketInputStream:146
                        if (!(p instanceof RevocationReason)) {
                            p = new RevocationReason(false, p.getData());
                        }
                        String reason = ((RevocationReason) p).getRevocationDescription();
                        mRReason.setText(reason);
                        mRowReason.setVisibility(View.VISIBLE);
                    }
                    break;
                }
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
            case android.R.id.home: {
                Intent viewIntent = NavUtils.getParentActivityIntent(this);
                viewIntent.setData(KeyRings.buildGenericKeyRingUri(mDataUri));
                NavUtils.navigateUpTo(this, viewIntent);
                return true;
            }
            case R.id.menu_view_cert_view_signer:
                // can't do this before the data is initialized
                Intent viewIntent = new Intent(this, ViewKeyActivity.class);

                try {
                    ProviderHelper providerHelper = new ProviderHelper(this);
                    long signerMasterKeyId = providerHelper.getMasterKeyId(
                            KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(Long.toString(mSignerKeyId))
                    );
                    viewIntent.setData(KeyRings.buildGenericKeyRingUri(
                            Long.toString(signerMasterKeyId))
                    );
                    startActivity(viewIntent);
                } catch (ProviderHelper.NotFoundException e) {
                    // TODO notify user of this, maybe offer download?
                    Log.e(Constants.TAG, "key not found!", e);
                }

                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}

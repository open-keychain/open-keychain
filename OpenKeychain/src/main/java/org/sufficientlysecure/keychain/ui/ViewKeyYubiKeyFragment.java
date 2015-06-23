/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

import java.nio.ByteBuffer;
import java.util.Arrays;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.PromoteKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.service.KeychainService;
import org.sufficientlysecure.keychain.service.PromoteKeyringParcel;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


public class ViewKeyYubiKeyFragment
        extends CryptoOperationFragment<PromoteKeyringParcel, PromoteKeyResult>
        implements LoaderCallbacks<Cursor> {

    public static final String ARG_MASTER_KEY_ID = "master_key_id";
    public static final String ARG_FINGERPRINT = "fingerprint";
    public static final String ARG_USER_ID = "user_id";
    public static final String ARG_CARD_AID = "aid";

    private byte[][] mFingerprints;
    private String mUserId;
    private byte[] mCardAid;
    private long mMasterKeyId;
    private long[] mSubKeyIds;

    private Button vButton;
    private TextView vStatus;

    public static ViewKeyYubiKeyFragment newInstance(long masterKeyId,
            byte[] fingerprints, String userId, byte[] aid) {
        ViewKeyYubiKeyFragment frag = new ViewKeyYubiKeyFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_MASTER_KEY_ID, masterKeyId);
        args.putByteArray(ARG_FINGERPRINT, fingerprints);
        args.putString(ARG_USER_ID, userId);
        args.putByteArray(ARG_CARD_AID, aid);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        ByteBuffer buf = ByteBuffer.wrap(args.getByteArray(ARG_FINGERPRINT));
        mFingerprints = new byte[buf.remaining()/20][];
        for (int i = 0; i < mFingerprints.length; i++) {
            mFingerprints[i] = new byte[20];
            buf.get(mFingerprints[i]);
        }
        mUserId = args.getString(ARG_USER_ID);
        mCardAid = args.getByteArray(ARG_CARD_AID);

        mMasterKeyId = args.getLong(ARG_MASTER_KEY_ID);

        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_yubikey, null);

        TextView vSerNo = (TextView) view.findViewById(R.id.yubikey_serno);
        TextView vUserId = (TextView) view.findViewById(R.id.yubikey_userid);

        String serno = Hex.toHexString(mCardAid, 10, 4);
        vSerNo.setText(getString(R.string.yubikey_serno, serno));

        if (!mUserId.isEmpty()) {
            vUserId.setText(getString(R.string.yubikey_key_holder, mUserId));
        } else {
            vUserId.setText(getString(R.string.yubikey_key_holder_not_set));
        }

        vButton = (Button) view.findViewById(R.id.button_bind);
        vButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                promoteToSecretKey();
            }
        });

        vStatus = (TextView) view.findViewById(R.id.yubikey_status);

        return view;
    }

    public void promoteToSecretKey() {
        long[] subKeyIds = new long[mFingerprints.length];
        for (int i = 0; i < subKeyIds.length; i++) {
            subKeyIds[i] = KeyFormattingUtils.getKeyIdFromFingerprint(mFingerprints[i]);
        }

        // mMasterKeyId and mCardAid are already set
        mSubKeyIds = subKeyIds;

        cryptoOperation();
    }

    public static final String[] PROJECTION = new String[]{
            Keys._ID,
            Keys.KEY_ID,
            Keys.RANK,
            Keys.HAS_SECRET,
            Keys.FINGERPRINT
    };
    // private static final int INDEX_KEY_ID = 1;
    // private static final int INDEX_RANK = 2;
    private static final int INDEX_HAS_SECRET = 3;
    private static final int INDEX_FINGERPRINT = 4;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Keys.buildKeysUri(mMasterKeyId),
                PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            // wut?
            return;
        }

        boolean allBound = true;
        boolean noneBound = true;

        do {
            SecretKeyType keyType = SecretKeyType.fromNum(data.getInt(INDEX_HAS_SECRET));
            byte[] fingerprint = data.getBlob(INDEX_FINGERPRINT);
            Integer index = naiveIndexOf(mFingerprints, fingerprint);
            if (index == null) {
                continue;
            }
            if (keyType == SecretKeyType.DIVERT_TO_CARD) {
                noneBound = false;
            } else {
                allBound = false;
            }
        } while (data.moveToNext());

        if (allBound) {
            vButton.setVisibility(View.GONE);
            vStatus.setText(R.string.yubikey_status_bound);
        } else {
            vButton.setVisibility(View.VISIBLE);
            vStatus.setText(noneBound
                    ? R.string.yubikey_status_unbound
                    : R.string.yubikey_status_partly);
        }

    }

    static private Integer naiveIndexOf(byte[][] haystack, byte[] needle) {
        for (int i = 0; i < haystack.length; i++) {
            if (Arrays.equals(needle, haystack[i])) {
                return i;
            }
        }
        return null;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    @Override
    protected PromoteKeyringParcel createOperationInput() {
        return new PromoteKeyringParcel(mMasterKeyId, mCardAid, mSubKeyIds);
    }

    @Override
    protected void onCryptoOperationResult(PromoteKeyResult result) {
        result.createNotify(getActivity()).show();
    }
}

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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Button;
import com.tokenautocomplete.TokenCompleteTextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.CachedPublicKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.ui.widget.EncryptKeyCompletionView;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class EncryptAsymmetricFragment extends Fragment implements EncryptActivityInterface.UpdateListener {
    public static final String ARG_SIGNATURE_KEY_ID = "signature_key_id";
    public static final String ARG_ENCRYPTION_KEY_IDS = "encryption_key_ids";

    ProviderHelper mProviderHelper;

    // view
    private Spinner mSign;
    private EncryptKeyCompletionView mEncryptKeyView;
    private SelectSignKeyCursorAdapter mSignAdapter = new SelectSignKeyCursorAdapter();

    // model
    private EncryptActivityInterface mEncryptInterface;

    @Override
    public void onNotifyUpdate() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mEncryptInterface = (EncryptActivityInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement EncryptActivityInterface");
        }
    }

    private void setSignatureKeyId(long signatureKeyId) {
        mEncryptInterface.setSignatureKey(signatureKeyId);
    }

    private void setEncryptionKeyIds(long[] encryptionKeyIds) {
        mEncryptInterface.setEncryptionKeys(encryptionKeyIds);
    }

    private void setEncryptionUserIds(String[] encryptionUserIds) {
        mEncryptInterface.setEncryptionUsers(encryptionUserIds);
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_asymmetric_fragment, container, false);

        mSign = (Spinner) view.findViewById(R.id.sign);
        mSign.setAdapter(mSignAdapter);
        mSign.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                setSignatureKeyId(parent.getAdapter().getItemId(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                setSignatureKeyId(Constants.key.none);
            }
        });
        mEncryptKeyView = (EncryptKeyCompletionView) view.findViewById(R.id.recipient_list);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        long signatureKeyId = getArguments().getLong(ARG_SIGNATURE_KEY_ID);
        long[] encryptionKeyIds = getArguments().getLongArray(ARG_ENCRYPTION_KEY_IDS);

        mProviderHelper = new ProviderHelper(getActivity());

        // preselect keys given by arguments (given by Intent to EncryptActivity)
        preselectKeys(signatureKeyId, encryptionKeyIds, mProviderHelper);

        getLoaderManager().initLoader(1, null, new LoaderManager.LoaderCallbacks<Cursor>() {
            @Override
            public Loader<Cursor> onCreateLoader(int id, Bundle args) {
                // This is called when a new Loader needs to be created. This
                // sample only has one Loader, so we don't care about the ID.
                Uri baseUri = KeyRings.buildUnifiedKeyRingsUri();

                // These are the rows that we will retrieve.
                String[] projection = new String[]{
                        KeyRings._ID,
                        KeyRings.MASTER_KEY_ID,
                        KeyRings.KEY_ID,
                        KeyRings.USER_ID,
                        KeyRings.EXPIRY,
                        KeyRings.IS_REVOKED,
                        // can certify info only related to master key
                        KeyRings.CAN_CERTIFY,
                        // has sign may be any subkey
                        KeyRings.HAS_SIGN,
                        KeyRings.HAS_ANY_SECRET,
                        KeyRings.HAS_SECRET
                };

                String where = KeyRings.HAS_ANY_SECRET + " = 1";

                // Now create and return a CursorLoader that will take care of
                // creating a Cursor for the data being displayed.
                return new CursorLoader(getActivity(), baseUri, projection, where, null, null);
                /*return new CursorLoader(getActivity(), KeyRings.buildUnifiedKeyRingsUri(),
                        new String[]{KeyRings.USER_ID, KeyRings.KEY_ID, KeyRings.MASTER_KEY_ID, KeyRings.HAS_ANY_SECRET}, SIGN_KEY_SELECTION,
                        null, null);*/
            }

            @Override
            public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
                mSignAdapter.swapCursor(data);
            }

            @Override
            public void onLoaderReset(Loader<Cursor> loader) {
                mSignAdapter.swapCursor(null);
            }
        });
        mEncryptKeyView.setTokenListener(new TokenCompleteTextView.TokenListener() {
            @Override
            public void onTokenAdded(Object token) {
                if (token instanceof EncryptKeyCompletionView.EncryptionKey) {
                    updateEncryptionKeys();
                }
            }

            @Override
            public void onTokenRemoved(Object token) {
                if (token instanceof EncryptKeyCompletionView.EncryptionKey) {
                    updateEncryptionKeys();
                }
            }
        });
    }

    /**
     * If an Intent gives a signatureMasterKeyId and/or encryptionMasterKeyIds, preselect those!
     *
     * @param preselectedSignatureKeyId
     * @param preselectedEncryptionKeyIds
     */
    private void preselectKeys(long preselectedSignatureKeyId, long[] preselectedEncryptionKeyIds,
                               ProviderHelper providerHelper) {
        // TODO all of this works under the assumption that the first suitable subkey is always used!
        // not sure if we need to distinguish between different subkeys here?
        if (preselectedSignatureKeyId != 0) {
            try {
                CachedPublicKeyRing keyring =
                        providerHelper.getCachedPublicKeyRing(
                                KeyRings.buildUnifiedKeyRingUri(preselectedSignatureKeyId));
                if(keyring.hasAnySecret()) {
                    setSignatureKeyId(keyring.getMasterKeyId());
                }
            } catch (PgpGeneralException e) {
                Log.e(Constants.TAG, "key not found!", e);
            }
        }

        if (preselectedEncryptionKeyIds != null) {
            for (long preselectedId : preselectedEncryptionKeyIds) {
                try {
                    CachedPublicKeyRing ring = providerHelper.getCachedPublicKeyRing(
                            KeyRings.buildUnifiedKeyRingsFindBySubkeyUri(preselectedId));
                    mEncryptKeyView.addObject(mEncryptKeyView.new EncryptionKey(ring));
                } catch (PgpGeneralException e) {
                    Log.e(Constants.TAG, "key not found!", e);
                }
            }
            updateEncryptionKeys();
        }
    }

    private void updateEncryptionKeys() {
        List<Object> objects = mEncryptKeyView.getObjects();
        List<Long> keyIds = new ArrayList<Long>();
        List<String> userIds = new ArrayList<String>();
        for (Object object : objects) {
            if (object instanceof EncryptKeyCompletionView.EncryptionKey) {
                keyIds.add(((EncryptKeyCompletionView.EncryptionKey) object).getKeyId());
                userIds.add(((EncryptKeyCompletionView.EncryptionKey) object).getUserId());
            }
        }
        long[] keyIdsArr = new long[keyIds.size()];
        Iterator<Long> iterator = keyIds.iterator();
        for (int i = 0; i < keyIds.size(); i++) {
            keyIdsArr[i] = iterator.next();
        }
        setEncryptionKeyIds(keyIdsArr);
        setEncryptionUserIds(userIds.toArray(new String[userIds.size()]));
    }

    private class SelectSignKeyCursorAdapter extends BaseAdapter implements SpinnerAdapter {
        private CursorAdapter inner;
        private int mIndexUserId;
        private int mIndexKeyId;
        private int mIndexMasterKeyId;

        public SelectSignKeyCursorAdapter() {
            inner = new CursorAdapter(null, null, 0) {
                @Override
                public View newView(Context context, Cursor cursor, ViewGroup parent) {
                    return getActivity().getLayoutInflater().inflate(R.layout.encrypt_asymmetric_signkey, null);
                }

                @Override
                public void bindView(View view, Context context, Cursor cursor) {
                    ((TextView) view.findViewById(android.R.id.text1)).setText(cursor.getString(mIndexUserId));
                    view.findViewById(android.R.id.text2).setVisibility(View.VISIBLE);
                    ((TextView) view.findViewById(android.R.id.text2)).setText(PgpKeyHelper.convertKeyIdToHex(cursor.getLong(mIndexKeyId)));
                }

                @Override
                public long getItemId(int position) {
                    mCursor.moveToPosition(position);
                    return mCursor.getLong(mIndexMasterKeyId);
                }
            };
        }

        public Cursor swapCursor(Cursor newCursor) {
            if (newCursor == null) return inner.swapCursor(null);

            mIndexKeyId = newCursor.getColumnIndex(KeyRings.KEY_ID);
            mIndexUserId = newCursor.getColumnIndex(KeyRings.USER_ID);
            mIndexMasterKeyId = newCursor.getColumnIndex(KeyRings.MASTER_KEY_ID);
            if (newCursor.moveToFirst()) {
                do {
                    if (newCursor.getLong(mIndexMasterKeyId) == mEncryptInterface.getSignatureKey()) {
                        mSign.setSelection(newCursor.getPosition() + 1);
                    }
                } while (newCursor.moveToNext());
            }
            return inner.swapCursor(newCursor);
        }

        @Override
        public int getCount() {
            return inner.getCount() + 1;
        }

        @Override
        public Object getItem(int position) {
            if (position == 0) return null;
            return inner.getItem(position - 1);
        }

        @Override
        public long getItemId(int position) {
            if (position == 0) return Constants.key.none;
            return inner.getItemId(position - 1);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (position == 0) {
                View v;
                if (convertView == null) {
                    v = inner.newView(null, null, parent);
                } else {
                    v = convertView;
                }
                ((TextView) v.findViewById(android.R.id.text1)).setText("None");
                v.findViewById(android.R.id.text2).setVisibility(View.GONE);
                return v;
            } else {
                return inner.getView(position - 1, convertView, parent);
            }
        }
    }

}

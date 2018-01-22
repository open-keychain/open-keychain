/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.ui.adapter.CertSectionedListAdapter.CertCursor;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.adapter.CursorAdapter;
import org.sufficientlysecure.keychain.ui.util.adapter.SectionCursorAdapter;

import java.util.ArrayList;
import java.util.Arrays;

public class CertSectionedListAdapter extends SectionCursorAdapter<CertCursor, String,
        CertSectionedListAdapter.CertItemViewHolder, CertSectionedListAdapter.CertSectionViewHolder> {

    private CertListListener mListener;

    public CertSectionedListAdapter(Context context, CertCursor cursor) {
        super(context, CertCursor.wrap(cursor), 0);
    }

    public void setCertListListener(CertListListener listener) {
        mListener = listener;
    }

    @Override
    public long getIdFromCursor(CertCursor cursor) {
        return cursor.getKeyId();
    }

    @Override
    protected String getSectionFromCursor(CertCursor cursor) throws IllegalStateException {
        return cursor.getRawSignerUserId();
    }

    @Override
    protected CertSectionViewHolder onCreateSectionViewHolder(ViewGroup parent, int viewType) {
        return new CertSectionViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_key_adv_certs_header, parent, false));
    }

    @Override
    protected CertItemViewHolder onCreateItemViewHolder(ViewGroup parent, int viewType) {
        return new CertItemViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_key_adv_certs_item, parent, false));
    }

    @Override
    protected void onBindSectionViewHolder(CertSectionViewHolder holder, String section) {
        holder.bind(section);
    }

    @Override
    protected void onBindItemViewHolder(CertItemViewHolder holder, CertCursor cursor) {
        holder.bind(cursor);
    }

    class CertItemViewHolder extends SectionCursorAdapter.ViewHolder
            implements View.OnClickListener {

        private TextView mSignerKeyId;
        private TextView mSignerName;
        private TextView mSignStatus;

        public CertItemViewHolder(View itemView) {
            super(itemView);

            itemView.setClickable(true);
            itemView.setOnClickListener(this);

            mSignerName = itemView.findViewById(R.id.signerName);
            mSignStatus = itemView.findViewById(R.id.signStatus);
            mSignerKeyId = itemView.findViewById(R.id.signerKeyId);
        }

        public void bind(CertCursor cursor) {
            String signerKeyId = KeyFormattingUtils.beautifyKeyIdWithPrefix(
                    cursor.getCertifierKeyId());

            OpenPgpUtils.UserId userId = cursor.getSignerUserId();
            if (userId.name != null) {
                mSignerName.setText(userId.name);
            } else {
                mSignerName.setText(R.string.user_id_no_name);
            }

            mSignerKeyId.setText(signerKeyId);
            switch (cursor.getType()) {
                case WrappedSignature.DEFAULT_CERTIFICATION: // 0x10
                    mSignStatus.setText(R.string.cert_default);
                    break;
                case WrappedSignature.NO_CERTIFICATION: // 0x11
                    mSignStatus.setText(R.string.cert_none);
                    break;
                case WrappedSignature.CASUAL_CERTIFICATION: // 0x12
                    mSignStatus.setText(R.string.cert_casual);
                    break;
                case WrappedSignature.POSITIVE_CERTIFICATION: // 0x13
                    mSignStatus.setText(R.string.cert_positive);
                    break;
                case WrappedSignature.CERTIFICATION_REVOCATION: // 0x30
                    mSignStatus.setText(R.string.cert_revoke);
                    break;
            }
        }

        @Override
        public void onClick(View v) {
            if(mListener != null) {
                int index = getCursorPositionWithoutSections(getAdapterPosition());
                if (moveCursor(index)) {
                    CertCursor cursor = getCursor();
                    mListener.onClick(
                            cursor.getKeyId(),
                            cursor.getCertifierKeyId(),
                            cursor.getRank()
                    );
                }
            }
        }
    }

    static class CertSectionViewHolder extends SectionCursorAdapter.ViewHolder {
        private TextView mHeaderText;

        public CertSectionViewHolder(View itemView) {
            super(itemView);
            mHeaderText = itemView.findViewById(R.id.stickylist_header_text);
        }

        public void bind(String text) {
            mHeaderText.setText(text);
        }
    }

    public static class CertCursor extends CursorAdapter.SimpleCursor {
        public static final String[] CERTS_PROJECTION;
        static {
            ArrayList<String> projection = new ArrayList<>();
            projection.addAll(Arrays.asList(SimpleCursor.PROJECTION));
            projection.addAll(Arrays.asList(
                    KeychainContract.Certs.MASTER_KEY_ID,
                    KeychainContract.Certs.VERIFIED,
                    KeychainContract.Certs.TYPE,
                    KeychainContract.Certs.RANK,
                    KeychainContract.Certs.KEY_ID_CERTIFIER,
                    KeychainContract.Certs.USER_ID,
                    KeychainContract.Certs.SIGNER_UID
            ));

            CERTS_PROJECTION = projection.toArray(new String[projection.size()]);
        }

        public static final String CERTS_SORT_ORDER =
                KeychainDatabase.Tables.CERTS + "." + KeychainContract.Certs.RANK + " ASC, "
                        + KeychainContract.Certs.VERIFIED + " DESC, "
                        + KeychainDatabase.Tables.CERTS + "." + KeychainContract.Certs.TYPE + " DESC, "
                        + KeychainContract.Certs.SIGNER_UID + " ASC";

        public static CertCursor wrap(Cursor cursor) {
            if(cursor != null) {
                return new CertCursor(cursor);
            } else {
                return null;
            }
        }

        private CertCursor(Cursor cursor) {
            super(cursor);
        }

        public long getKeyId() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs.MASTER_KEY_ID);
            return getLong(index);
        }

        public boolean isVerified() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs.VERIFIED);
            return getInt(index) > 0;
        }

        public int getType() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs.TYPE);
            return getInt(index);
        }

        public long getRank() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs.RANK);
            return getLong(index);
        }

        public long getCertifierKeyId() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs.KEY_ID_CERTIFIER);
            return getLong(index);
        }

        public String getRawUserId() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs.USER_ID);
            return getString(index);
        }

        public String getRawSignerUserId() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs.SIGNER_UID);
            return getString(index);
        }

        public String getName() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs.NAME);
            return getString(index);
        }

        public String getEmail() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs.EMAIL);
            return getString(index);
        }

        public String getComment() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs.COMMENT);
            return getString(index);
        }

        public OpenPgpUtils.UserId getSignerUserId() {
            return KeyRing.splitUserId(getRawSignerUserId());
        }
    }

    public interface CertListListener {
        void onClick(long masterKeyId, long signerKeyId, long rank);
    }
}
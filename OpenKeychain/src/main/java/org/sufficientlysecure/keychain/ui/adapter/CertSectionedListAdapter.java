package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.HashMap;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainDatabase;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.adapter.SectionCursorAdapter;


public class CertSectionedListAdapter extends SectionCursorAdapter<CertSectionedListAdapter.CertCursor, String,
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

    class CertItemViewHolder extends SectionCursorAdapter.ViewHolder implements View.OnClickListener {
        private TextView mSignerKeyId;
        private TextView mSignerName;
        private TextView mSignStatus;

        public CertItemViewHolder(View itemView) {
            super(itemView);

            itemView.setClickable(true);
            itemView.setOnClickListener(this);

            mSignerName = (TextView) itemView.findViewById(R.id.signerName);
            mSignStatus = (TextView) itemView.findViewById(R.id.signStatus);
            mSignerKeyId = (TextView) itemView.findViewById(R.id.signerKeyId);
        }

        public void bind(CertCursor cursor) {
            String signerKeyId = KeyFormattingUtils.beautifyKeyIdWithPrefix(
                    itemView.getContext(), cursor.getCertifierKeyId());

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
            mHeaderText = (TextView) itemView.findViewById(R.id.stickylist_header_text);
        }

        public void bind(String text) {
            mHeaderText.setText(text);
        }
    }

    public static class CertCursor extends CursorWrapper {
        public static final String[] CERTS_PROJECTION = new String[]{
                KeychainContract.Certs._ID,
                KeychainContract.Certs.MASTER_KEY_ID,
                KeychainContract.Certs.VERIFIED,
                KeychainContract.Certs.TYPE,
                KeychainContract.Certs.RANK,
                KeychainContract.Certs.KEY_ID_CERTIFIER,
                KeychainContract.Certs.USER_ID,
                KeychainContract.Certs.SIGNER_UID
        };

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

        private HashMap<String, Integer> mColumnIndices;

        /**
         * Creates a cursor wrapper.
         *
         * @param cursor The underlying cursor to wrap.
         */
        private CertCursor(Cursor cursor) {
            super(cursor);
            mColumnIndices = new HashMap<>(cursor.getColumnCount() * 4 / 3, 0.75f);
        }

        @Override
        public void close() {
            mColumnIndices.clear();
            super.close();
        }

        public int getEntryId() {
            int index = getColumnIndexOrThrow(KeychainContract.Certs._ID);
            return getInt(index);
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

        public OpenPgpUtils.UserId getUserId() {
            return KeyRing.splitUserId(getRawUserId());
        }

        public OpenPgpUtils.UserId getSignerUserId() {
            return KeyRing.splitUserId(getRawSignerUserId());
        }

        @Override
        public int getColumnIndexOrThrow(String colName) {
            Integer colIndex = mColumnIndices.get(colName);
            if(colIndex == null) {
                colIndex = super.getColumnIndexOrThrow(colName);
                mColumnIndices.put(colName, colIndex);
            } else if (colIndex < 0){
                throw new IllegalArgumentException("Could not get column index for name: \"" + colName + "\"");
            }

            return colIndex;
        }

        @Override
        public int getColumnIndex(String colName) {
            Integer colIndex = mColumnIndices.get(colName);
            if(colIndex == null) {
                colIndex = super.getColumnIndex(colName);
                mColumnIndices.put(colName, colIndex);
            }

            return colIndex;
        }
    }

    public interface CertListListener {
        void onClick(long masterKeyId, long signerKeyId, long rank);
    }
}
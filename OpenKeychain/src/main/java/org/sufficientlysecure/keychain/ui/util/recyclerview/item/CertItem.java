package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.database.Cursor;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.WrappedSignature;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.recyclerview.cursor.CertCursor;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractSectionableItem;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by daquexian on 17-2-7.
 */

public class CertItem extends AbstractSectionableItem<CertItem.CertViewHolder, CertHeaderItem> {
    private long mCertifierKeyId;
    private OpenPgpUtils.UserId mUserId;
    private int mType;
    private String mRawSignerUserId;
    private long mMasterKeyId;
    private long mRank;

    public CertItem(CertHeaderItem headerItem, CertCursor cursor) {
        super(headerItem);
        mCertifierKeyId = cursor.getCertifierKeyId();
        mUserId = cursor.getSignerUserId();
        mRawSignerUserId = cursor.getRawSignerUserId();
        mType = cursor.getType();
        mMasterKeyId = cursor.getKeyId();
        mRank = cursor.getRank();
    }

    @Override
    public CertViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new CertViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, CertViewHolder holder, int position, List payloads) {
        holder.bind(this);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.view_key_adv_certs_item;
    }

    @Override
    public boolean equals(Object o) {
        return false;
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    public long getCertifierKeyId() {
        return mCertifierKeyId;
    }

    public long getSignerKeyId() {
        return mCertifierKeyId;
    }

    public OpenPgpUtils.UserId getSignerUserId() {
        return mUserId;
    }

    public int getType() {
        return mType;
    }

    public long getMasterKeyId() {
        return mMasterKeyId;
    }

    public long getRank() {
        return mRank;
    }

    public String getSection() {
        if (TextUtils.isEmpty(mRawSignerUserId)) {
            return "?";
        }
        return mRawSignerUserId;
    }

    static final class CertViewHolder extends FlexibleViewHolder {

        private TextView mSignerKeyId;
        private TextView mSignerName;
        private TextView mSignStatus;

        public CertViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter);

            itemView.setClickable(true);
            itemView.setOnClickListener(this);

            mSignerName = (TextView) itemView.findViewById(R.id.signerName);
            mSignStatus = (TextView) itemView.findViewById(R.id.signStatus);
            mSignerKeyId = (TextView) itemView.findViewById(R.id.signerKeyId);
        }

        public void bind(CertItem certItem) {
            String signerKeyId = KeyFormattingUtils.beautifyKeyIdWithPrefix(
                    certItem.getCertifierKeyId());

            OpenPgpUtils.UserId userId = certItem.getSignerUserId();
            if (userId.name != null) {
                mSignerName.setText(userId.name);
            } else {
                mSignerName.setText(R.string.user_id_no_name);
            }

            mSignerKeyId.setText(signerKeyId);
            switch (certItem.getType()) {
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
    }
}

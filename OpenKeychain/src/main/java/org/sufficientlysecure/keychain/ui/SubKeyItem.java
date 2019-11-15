package org.sufficientlysecure.keychain.ui;


import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import androidx.annotation.StringRes;
import android.text.format.DateFormat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import eu.davidea.viewholders.FlexibleViewHolder;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Builder;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Curve;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange;
import org.sufficientlysecure.keychain.ui.ViewKeyAdvSubkeysFragment.SubkeyEditViewModel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


public class SubKeyItem extends AbstractFlexibleItem<SubKeyItem.SubkeyViewHolder> {
    final SubKey subkeyInfo;
    private final SubkeyEditViewModel viewModel;

    SubKeyItem(SubKey subkeyInfo, SubkeyEditViewModel viewModel) {
        this.subkeyInfo = subkeyInfo;
        this.viewModel = viewModel;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SubKeyItem && ((SubKeyItem) o).subkeyInfo.key_id() == subkeyInfo.key_id();
    }

    @Override
    public int hashCode() {
        long key_id = subkeyInfo.key_id();
        return (int) (key_id ^ (key_id >>> 32));
    }

    @Override
    public int getLayoutRes() {
        return R.layout.view_key_adv_subkey_item;
    }

    @Override
    public SubkeyViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new SubkeyViewHolder(view, adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, SubkeyViewHolder holder, int position,
            List<Object> payloads) {
        holder.bind(subkeyInfo);
        holder.bindSubkeyAction(subkeyInfo, viewModel.skpBuilder);
    }

    @Override
    public int getItemViewType() {
        return ViewKeyAdvSubkeysFragment.SUBKEY_TYPE_DETAIL;
    }

    public static class SubkeyViewHolder extends FlexibleViewHolder {
        final TextView vKeyId;
        final TextView vKeyDetails;
        final TextView vKeyStatus;
        final ImageView vCertifyIcon;
        final ImageView vSignIcon;
        final ImageView vEncryptIcon;
        final ImageView vAuthenticateIcon;
        final View vActionLayout;
        final TextView vActionText;
        final ImageView vActionCancel;

        public SubkeyViewHolder(View itemView, FlexibleAdapter adapter) {
            super(itemView, adapter);

            vKeyId = itemView.findViewById(R.id.subkey_item_key_id);
            vKeyDetails = itemView.findViewById(R.id.subkey_item_details);
            vKeyStatus = itemView.findViewById(R.id.subkey_item_status);
            vCertifyIcon = itemView.findViewById(R.id.subkey_item_ic_certify);
            vSignIcon = itemView.findViewById(R.id.subkey_item_ic_sign);
            vEncryptIcon = itemView.findViewById(R.id.subkey_item_ic_encrypt);
            vAuthenticateIcon = itemView.findViewById(R.id.subkey_item_ic_authenticate);
            vActionLayout = itemView.findViewById(R.id.layout_subkey_action);
            vActionText = itemView.findViewById(R.id.text_subkey_action);
            vActionCancel = itemView.findViewById(R.id.button_subkey_action_cancel);
        }

        void bind(SubKey subkeyInfo) {
            bindKeyId(subkeyInfo.key_id(), subkeyInfo.rank() == 0);
            bindKeyDetails(subkeyInfo.algorithm(), subkeyInfo.key_size(), subkeyInfo.key_curve_oid(), subkeyInfo.has_secret());
            bindKeyFlags(subkeyInfo.can_certify(), subkeyInfo.can_sign(), subkeyInfo.can_encrypt(), subkeyInfo.can_authenticate());

            Date validFrom = new Date(subkeyInfo.validFrom() * 1000);
            Date expiryDate = subkeyInfo.expires() ? new Date(subkeyInfo.expiry() * 1000) : null;
            bindKeyStatus(validFrom, expiryDate, subkeyInfo.is_revoked(), subkeyInfo.is_secure());
        }

        public void bindKeyId(Long keyId, boolean isMasterKey) {
            if (keyId == null) {
                vKeyId.setText(R.string.edit_key_new_subkey);
            } else {
                vKeyId.setText(KeyFormattingUtils.beautifyKeyId(keyId));
            }
            vKeyId.setTypeface(null, isMasterKey ? Typeface.BOLD : Typeface.NORMAL);
        }

        public void bindKeyStatus(Date validFrom, Date expiryDate, boolean isRevoked, boolean isSecure) {
            Context context = itemView.getContext();
            Date now = new Date();

            boolean isNotYetValid = validFrom != null && validFrom.after(now);
            boolean isExpired = expiryDate != null && expiryDate.before(now);
            if (isNotYetValid) {
                Calendar validFromCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                validFromCal.setTime(validFrom);
                // convert from UTC to time zone of device
                validFromCal.setTimeZone(TimeZone.getDefault());

                vKeyStatus.setText(context.getString(R.string.label_valid_from) + ": "
                        + DateFormat.getDateFormat(context).format(validFromCal.getTime()));
            } else if (isRevoked) {
                vKeyStatus.setText(R.string.label_revoked);
            } else if (!isSecure) {
                vKeyStatus.setText(R.string.label_insecure);
            } else if (expiryDate != null) {
                    Calendar expiryCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    expiryCal.setTime(expiryDate);
                    // convert from UTC to time zone of device
                    expiryCal.setTimeZone(TimeZone.getDefault());

                    vKeyStatus.setText(context.getString(R.string.label_expiry) + ": "
                            + DateFormat.getDateFormat(context).format(expiryCal.getTime()));
            } else {
                vKeyStatus.setText("");
            }

            boolean isValid = !isRevoked && !isExpired && !isNotYetValid && isSecure;
            bindValidityStatus(isValid);
        }

        private void bindValidityStatus(boolean isValid) {
            if (!isValid) {
                int key_flag_gray = itemView.getResources().getColor(R.color.key_flag_gray);
                vCertifyIcon.setColorFilter(key_flag_gray, PorterDuff.Mode.SRC_IN);
                vSignIcon.setColorFilter(key_flag_gray, PorterDuff.Mode.SRC_IN);
                vEncryptIcon.setColorFilter(key_flag_gray, PorterDuff.Mode.SRC_IN);
                vAuthenticateIcon.setColorFilter(key_flag_gray, PorterDuff.Mode.SRC_IN);
            } else {
                vCertifyIcon.clearColorFilter();
                vSignIcon.clearColorFilter();
                vEncryptIcon.clearColorFilter();
                vAuthenticateIcon.clearColorFilter();
            }

            vKeyId.setEnabled(isValid);
            vKeyDetails.setEnabled(isValid);
            vKeyStatus.setEnabled(isValid);
        }

        public void bindKeyDetails(Algorithm algorithm, Integer keySize, Curve curveOid, SecretKeyType secretKeyType) {
            Context context = itemView.getContext();

            String algorithmStr = KeyFormattingUtils.getAlgorithmInfo(context, algorithm, keySize, curveOid);
            bindKeyDetails(context, algorithmStr, secretKeyType);
        }

        void bindKeyDetails(int algorithm, Integer keySize, String curveOid, SecretKeyType secretKeyType) {
            Context context = itemView.getContext();

            String algorithmStr = KeyFormattingUtils.getAlgorithmInfo(context, algorithm, keySize, curveOid);
            bindKeyDetails(context, algorithmStr, secretKeyType);
        }

        private void bindKeyDetails(Context context, String algorithmStr, SecretKeyType secretKeyType) {
            switch (secretKeyType) {
                case GNU_DUMMY:
                    algorithmStr += ", " + context.getString(R.string.key_stripped);
                    break;
                case DIVERT_TO_CARD:
                    algorithmStr += ", " + context.getString(R.string.key_divert);
                    break;
            }
            vKeyDetails.setText(algorithmStr);
        }

        private void bindSubkeyAction(SubKey subkeyInfo, Builder saveKeyringParcelBuilder) {
            if (saveKeyringParcelBuilder == null) {
                itemView.setClickable(false);
                vActionLayout.setVisibility(View.GONE);
                return;
            }
            boolean isRevokeAction = (saveKeyringParcelBuilder.getMutableRevokeSubKeys().contains(subkeyInfo.key_id()));
            SubkeyChange change = saveKeyringParcelBuilder.getSubkeyChange(subkeyInfo.key_id());
            boolean hasAction = isRevokeAction || change != null;
            if (!hasAction) {
                itemView.setClickable(true);
                vActionLayout.setVisibility(View.GONE);
                return;
            }

            OnClickListener onClickRemoveModificationListener = v -> {
                saveKeyringParcelBuilder.removeModificationsForSubkey(subkeyInfo.key_id());
                mAdapter.notifyItemChanged(getAdapterPosition());
            };

            if (isRevokeAction) {
                bindSubkeyAction(R.string.subkey_action_revoke, onClickRemoveModificationListener);
                return;
            }

            if (change.getDummyStrip()) {
                bindSubkeyAction(R.string.subkey_action_strip, onClickRemoveModificationListener);
                return;
            }
            Long expiry = change.getExpiry();
            if (expiry != null) {
                if (expiry == 0L) {
                    bindSubkeyAction(R.string.subkey_action_expiry_never, onClickRemoveModificationListener);
                } else {
                    String expiryString = itemView.getContext().getString(R.string.subkey_action_expiry_date,
                            DateFormat.getDateFormat(itemView.getContext()).format(new Date(expiry * 1000)));
                    bindSubkeyAction(expiryString, onClickRemoveModificationListener);
                }
                return;
            }

            throw new UnsupportedOperationException();
        }

        void bindSubkeyAction(String actionText, OnClickListener onClickListener) {
            vActionText.setText(actionText);
            bindSubkeyAction(onClickListener);
        }

        public void bindSubkeyAction(@StringRes int actionTextRes, OnClickListener onClickListener) {
            vActionText.setText(actionTextRes);
            bindSubkeyAction(onClickListener);
        }

        private void bindSubkeyAction(OnClickListener onClickListener) {
            itemView.setClickable(false);
            vActionLayout.setVisibility(View.VISIBLE);
            vActionCancel.setOnClickListener(onClickListener);
        }

        public void bindKeyFlags(boolean canCertify, boolean canSign, boolean canEncrypt, boolean canAuthenticate) {
            vCertifyIcon.setVisibility(canCertify ? View.VISIBLE : View.GONE);
            vSignIcon.setVisibility(canSign ? View.VISIBLE : View.GONE);
            vEncryptIcon.setVisibility(canEncrypt ? View.VISIBLE : View.GONE);
            vAuthenticateIcon.setVisibility(canAuthenticate ? View.VISIBLE : View.GONE);
        }
    }
}
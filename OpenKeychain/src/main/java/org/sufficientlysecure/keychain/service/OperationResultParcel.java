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

package org.sufficientlysecure.keychain.service;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.github.johnpersano.supertoasts.SuperCardToast;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.OnClickWrapper;
import com.github.johnpersano.supertoasts.util.Style;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.LogDisplayActivity;
import org.sufficientlysecure.keychain.ui.LogDisplayFragment;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/** Represent the result of an operation.
 *
 * This class holds a result and the log of an operation. It can be subclassed
 * to include typed additional information specific to the operation. To keep
 * the class structure (somewhat) simple, this class contains an exhaustive
 * list (ie, enum) of all possible log types, which should in all cases be tied
 * to string resource ids.
 *
 *
 */
public class OperationResultParcel implements Parcelable {

    public static final String EXTRA_RESULT = "operation_result";

    /** Holds the overall result, the number specifying varying degrees of success. The first bit
     * is 0 on overall success, 1 on overall failure. All other bits may be used for more specific
     * conditions. */
    final int mResult;

    public static final int RESULT_OK = 0;
    public static final int RESULT_ERROR = 1;

    /// A list of log entries tied to the operation result.
    final OperationLog mLog;

    public OperationResultParcel(int result, OperationLog log) {
        mResult = result;
        mLog = log;
    }

    public OperationResultParcel(Parcel source) {
        mResult = source.readInt();
        mLog = new OperationLog();
        mLog.addAll(source.createTypedArrayList(LogEntryParcel.CREATOR));
    }

    public int getResult() {
        return mResult;
    }

    public boolean success() {
        return (mResult & 1) == 0;
    }

    public OperationLog getLog() {
        return mLog;
    }

    /** One entry in the log. */
    public static class LogEntryParcel implements Parcelable {
        public final LogLevel mLevel;
        public final LogType mType;
        public final Object[] mParameters;
        public final int mIndent;

        public LogEntryParcel(LogLevel level, LogType type, int indent, Object... parameters) {
            mLevel = level;
            mType = type;
            mParameters = parameters;
            mIndent = indent;
            Log.v(Constants.TAG, "log: " + this.toString());
        }

        public LogEntryParcel(Parcel source) {
            mLevel = LogLevel.values()[source.readInt()];
            mType = LogType.values()[source.readInt()];
            mParameters = (Object[]) source.readSerializable();
            mIndent = source.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(mLevel.ordinal());
            dest.writeInt(mType.ordinal());
            dest.writeSerializable(mParameters);
            dest.writeInt(mIndent);
        }

        public static final Creator<LogEntryParcel> CREATOR = new Creator<LogEntryParcel>() {
            public LogEntryParcel createFromParcel(final Parcel source) {
                return new LogEntryParcel(source);
            }

            public LogEntryParcel[] newArray(final int size) {
                return new LogEntryParcel[size];
            }
        };

        @Override
        public String toString() {
            return "LogEntryParcel{" +
                    "mLevel=" + mLevel +
                    ", mType=" + mType +
                    ", mParameters=" + Arrays.toString(mParameters) +
                    ", mIndent=" + mIndent +
                    '}';
        }
    }

    public SuperCardToast createNotify(final Activity activity) {

        int resultType = getResult();

        String str;
        int duration, color;

        // Not an overall failure
        if ((resultType & OperationResultParcel.RESULT_ERROR) == 0) {

            if (getLog().containsWarnings()) {
                duration = 0;
                color = Style.ORANGE;
            } else {
                duration = SuperToast.Duration.LONG;
                color = Style.GREEN;
            }

            str = "operation succeeded!";
            // str = activity.getString(R.string.import_error);

        } else {

            duration = 0;
            color = Style.RED;

            str = "operation failed";
            // str = activity.getString(R.string.import_error);

        }

        boolean button = getLog() != null && !getLog().isEmpty();
        SuperCardToast toast = new SuperCardToast(activity,
                button ? SuperToast.Type.BUTTON : SuperToast.Type.STANDARD,
                Style.getStyle(color, SuperToast.Animations.POPUP));
        toast.setText(str);
        toast.setDuration(duration);
        toast.setIndeterminate(duration == 0);
        toast.setSwipeToDismiss(true);
        // If we have a log and it's non-empty, show a View Log button
        if (button) {
            toast.setButtonIcon(R.drawable.ic_action_view_as_list,
                    activity.getResources().getString(R.string.view_log));
            toast.setButtonTextColor(activity.getResources().getColor(R.color.black));
            toast.setTextColor(activity.getResources().getColor(R.color.black));
            toast.setOnClickWrapper(new OnClickWrapper("supercardtoast",
                    new SuperToast.OnClickListener() {
                        @Override
                        public void onClick(View view, Parcelable token) {
                            Intent intent = new Intent(
                                    activity, LogDisplayActivity.class);
                            intent.putExtra(LogDisplayFragment.EXTRA_RESULT, OperationResultParcel.this);
                            activity.startActivity(intent);
                        }
                    }
            ));
        }

        return toast;

    }

    /** This is an enum of all possible log events.
     *
     * Element names should generally be prefixed with MSG_XX_ where XX is an
     * identifier based on the related activity.
     *
     * Log messages should occur for each distinguishable action group.  For
     * each such group, one message is displayed followed by warnings or
     * errors, and optionally subactions. The granularity should generally be
     * optimistic: No "success" messages are printed except for the outermost
     * operations - the success of an action group is indicated by the
     * beginning message of the next action group.
     *
     * Log messages should be in present tense, There should be no trailing
     * punctuation, except for error messages which may end in an exclamation
     * mark.
     *
     */
    public static enum LogType {

        INTERNAL_ERROR (R.string.internal_error),

        // import public
        MSG_IP(R.string.msg_ip),
        MSG_IP_APPLY_BATCH (R.string.msg_ip_apply_batch),
        MSG_IP_BAD_TYPE_SECRET (R.string.msg_ip_bad_type_secret),
        MSG_IP_DELETE_OLD_FAIL (R.string.msg_ip_delete_old_fail),
        MSG_IP_DELETE_OLD_OK (R.string.msg_ip_delete_old_ok),
        MSG_IP_ENCODE_FAIL (R.string.msg_ip_encode_fail),
        MSG_IP_FAIL_IO_EXC (R.string.msg_ip_fail_io_exc),
        MSG_IP_FAIL_OP_EXC (R.string.msg_ip_fail_op_exc),
        MSG_IP_FAIL_REMOTE_EX (R.string.msg_ip_fail_remote_ex),
        MSG_IP_INSERT_KEYRING (R.string.msg_ip_insert_keyring),
        MSG_IP_INSERT_SUBKEYS (R.string.msg_ip_insert_keys),
        MSG_IP_PREPARE (R.string.msg_ip_prepare),
        MSG_IP_REINSERT_SECRET (R.string.msg_ip_reinsert_secret),
        MSG_IP_MASTER (R.string.msg_ip_master),
        MSG_IP_MASTER_EXPIRED (R.string.msg_ip_master_expired),
        MSG_IP_MASTER_EXPIRES (R.string.msg_ip_master_expires),
        MSG_IP_MASTER_FLAGS_CES (R.string.msg_ip_master_flags_ces),
        MSG_IP_MASTER_FLAGS_CEX (R.string.msg_ip_master_flags_cex),
        MSG_IP_MASTER_FLAGS_CXS (R.string.msg_ip_master_flags_cxs),
        MSG_IP_MASTER_FLAGS_XES (R.string.msg_ip_master_flags_xes),
        MSG_IP_MASTER_FLAGS_CXX (R.string.msg_ip_master_flags_cxx),
        MSG_IP_MASTER_FLAGS_XEX (R.string.msg_ip_master_flags_xex),
        MSG_IP_MASTER_FLAGS_XXS (R.string.msg_ip_master_flags_xxs),
        MSG_IP_MASTER_FLAGS_XXX (R.string.msg_ip_master_flags_xxx),
        MSG_IP_SUBKEY (R.string.msg_ip_subkey),
        MSG_IP_SUBKEY_EXPIRED (R.string.msg_ip_subkey_expired),
        MSG_IP_SUBKEY_EXPIRES (R.string.msg_ip_subkey_expires),
        MSG_IP_SUBKEY_FLAGS_CES (R.string.msg_ip_subkey_flags_ces),
        MSG_IP_SUBKEY_FLAGS_CEX (R.string.msg_ip_subkey_flags_cex),
        MSG_IP_SUBKEY_FLAGS_CXS (R.string.msg_ip_subkey_flags_cxs),
        MSG_IP_SUBKEY_FLAGS_XES (R.string.msg_ip_subkey_flags_xes),
        MSG_IP_SUBKEY_FLAGS_CXX (R.string.msg_ip_subkey_flags_cxx),
        MSG_IP_SUBKEY_FLAGS_XEX (R.string.msg_ip_subkey_flags_xex),
        MSG_IP_SUBKEY_FLAGS_XXS (R.string.msg_ip_subkey_flags_xxs),
        MSG_IP_SUBKEY_FLAGS_XXX (R.string.msg_ip_subkey_flags_xxx),
        MSG_IP_SUCCESS (R.string.msg_ip_success),
        MSG_IP_SUCCESS_IDENTICAL (R.string.msg_ip_success_identical),
        MSG_IP_UID_CERT_BAD (R.string.msg_ip_uid_cert_bad),
        MSG_IP_UID_CERT_ERROR (R.string.msg_ip_uid_cert_error),
        MSG_IP_UID_CERT_GOOD (R.string.msg_ip_uid_cert_good),
        MSG_IP_UID_CERTS_UNKNOWN (R.plurals.msg_ip_uid_certs_unknown),
        MSG_IP_UID_CLASSIFYING_ZERO (R.string.msg_ip_uid_classifying_zero),
        MSG_IP_UID_CLASSIFYING (R.plurals.msg_ip_uid_classifying),
        MSG_IP_UID_REORDER(R.string.msg_ip_uid_reorder),
        MSG_IP_UID_PROCESSING (R.string.msg_ip_uid_processing),
        MSG_IP_UID_REVOKED (R.string.msg_ip_uid_revoked),

        // import secret
        MSG_IS(R.string.msg_is),
        MSG_IS_BAD_TYPE_PUBLIC (R.string.msg_is_bad_type_public),
        MSG_IS_DB_EXCEPTION (R.string.msg_is_db_exception),
        MSG_IS_FAIL_IO_EXC (R.string.msg_is_io_exc),
        MSG_IS_IMPORTING_SUBKEYS (R.string.msg_is_importing_subkeys),
        MSG_IS_PUBRING_GENERATE (R.string.msg_is_pubring_generate),
        MSG_IS_SUBKEY_NONEXISTENT (R.string.msg_is_subkey_nonexistent),
        MSG_IS_SUBKEY_OK (R.string.msg_is_subkey_ok),
        MSG_IS_SUBKEY_STRIPPED (R.string.msg_is_subkey_stripped),
        MSG_IS_SUCCESS_IDENTICAL (R.string.msg_is_success_identical),
        MSG_IS_SUCCESS (R.string.msg_is_success),

        // keyring canonicalization
        MSG_KC_V3_KEY (R.string.msg_kc_v3_key),
        MSG_KC_PUBLIC (R.string.msg_kc_public),
        MSG_KC_SECRET (R.string.msg_kc_secret),
        MSG_KC_FATAL_NO_UID (R.string.msg_kc_fatal_no_uid),
        MSG_KC_MASTER (R.string.msg_kc_master),
        MSG_KC_REVOKE_BAD_ERR (R.string.msg_kc_revoke_bad_err),
        MSG_KC_REVOKE_BAD_LOCAL (R.string.msg_kc_revoke_bad_local),
        MSG_KC_REVOKE_BAD_TIME (R.string.msg_kc_revoke_bad_time),
        MSG_KC_REVOKE_BAD_TYPE (R.string.msg_kc_revoke_bad_type),
        MSG_KC_REVOKE_BAD_TYPE_UID (R.string.msg_kc_revoke_bad_type_uid),
        MSG_KC_REVOKE_BAD (R.string.msg_kc_revoke_bad),
        MSG_KC_REVOKE_DUP (R.string.msg_kc_revoke_dup),
        MSG_KC_SUB (R.string.msg_kc_sub),
        MSG_KC_SUB_BAD(R.string.msg_kc_sub_bad),
        MSG_KC_SUB_BAD_ERR(R.string.msg_kc_sub_bad_err),
        MSG_KC_SUB_BAD_LOCAL(R.string.msg_kc_sub_bad_local),
        MSG_KC_SUB_BAD_KEYID(R.string.msg_kc_sub_bad_keyid),
        MSG_KC_SUB_BAD_TIME(R.string.msg_kc_sub_bad_time),
        MSG_KC_SUB_BAD_TYPE(R.string.msg_kc_sub_bad_type),
        MSG_KC_SUB_DUP (R.string.msg_kc_sub_dup),
        MSG_KC_SUB_PRIMARY_BAD(R.string.msg_kc_sub_primary_bad),
        MSG_KC_SUB_PRIMARY_BAD_ERR(R.string.msg_kc_sub_primary_bad_err),
        MSG_KC_SUB_PRIMARY_NONE(R.string.msg_kc_sub_primary_none),
        MSG_KC_SUB_NO_CERT(R.string.msg_kc_sub_no_cert),
        MSG_KC_SUB_REVOKE_BAD_ERR (R.string.msg_kc_sub_revoke_bad_err),
        MSG_KC_SUB_REVOKE_BAD (R.string.msg_kc_sub_revoke_bad),
        MSG_KC_SUB_REVOKE_DUP (R.string.msg_kc_sub_revoke_dup),
        MSG_KC_SUCCESS_BAD (R.plurals.msg_kc_success_bad),
        MSG_KC_SUCCESS_BAD_AND_RED (R.string.msg_kc_success_bad_and_red),
        MSG_KC_SUCCESS_REDUNDANT (R.plurals.msg_kc_success_redundant),
        MSG_KC_SUCCESS (R.string.msg_kc_success),
        MSG_KC_UID_BAD_ERR (R.string.msg_kc_uid_bad_err),
        MSG_KC_UID_BAD_LOCAL (R.string.msg_kc_uid_bad_local),
        MSG_KC_UID_BAD_TIME (R.string.msg_kc_uid_bad_time),
        MSG_KC_UID_BAD_TYPE (R.string.msg_kc_uid_bad_type),
        MSG_KC_UID_BAD (R.string.msg_kc_uid_bad),
        MSG_KC_UID_DUP (R.string.msg_kc_uid_dup),
        MSG_KC_UID_FOREIGN (R.string.msg_kc_uid_foreign),
        MSG_KC_UID_NO_CERT (R.string.msg_kc_uid_no_cert),
        MSG_KC_UID_REVOKE_DUP (R.string.msg_kc_uid_revoke_dup),
        MSG_KC_UID_REVOKE_OLD (R.string.msg_kc_uid_revoke_old),
        MSG_KC_UID_REMOVE (R.string.msg_kc_uid_remove),


        // keyring consolidation
        MSG_MG_PUBLIC (R.string.msg_mg_public),
        MSG_MG_SECRET (R.string.msg_mg_secret),
        MSG_MG_FATAL_ENCODE (R.string.msg_mg_fatal_encode),
        MSG_MG_HETEROGENEOUS (R.string.msg_mg_heterogeneous),
        MSG_MG_NEW_SUBKEY (R.string.msg_mg_new_subkey),
        MSG_MG_FOUND_NEW (R.string.msg_mg_found_new),
        MSG_MG_UNCHANGED (R.string.msg_mg_unchanged),

        // secret key create
        MSG_CR (R.string.msg_cr),
        MSG_CR_ERROR_NO_MASTER (R.string.msg_cr_error_no_master),
        MSG_CR_ERROR_NO_USER_ID (R.string.msg_cr_error_no_user_id),
        MSG_CR_ERROR_NO_CERTIFY (R.string.msg_cr_error_no_certify),
        MSG_CR_ERROR_NULL_EXPIRY(R.string.msg_cr_error_null_expiry),
        MSG_CR_ERROR_KEYSIZE_512 (R.string.msg_cr_error_keysize_512),
        MSG_CR_ERROR_UNKNOWN_ALGO (R.string.msg_cr_error_unknown_algo),
        MSG_CR_ERROR_INTERNAL_PGP (R.string.msg_cr_error_internal_pgp),
        MSG_CR_ERROR_MASTER_ELGAMAL (R.string.msg_cr_error_master_elgamal),

        // secret key modify
        MSG_MF (R.string.msg_mr),
        MSG_MF_ERROR_ENCODE (R.string.msg_mf_error_encode),
        MSG_MF_ERROR_FINGERPRINT (R.string.msg_mf_error_fingerprint),
        MSG_MF_ERROR_KEYID (R.string.msg_mf_error_keyid),
        MSG_MF_ERROR_INTEGRITY (R.string.msg_mf_error_integrity),
        MSG_MF_ERROR_MASTER_NONE(R.string.msg_mf_error_master_none),
        MSG_MF_ERROR_NO_CERTIFY (R.string.msg_cr_error_no_certify),
        MSG_MF_ERROR_NOEXIST_PRIMARY (R.string.msg_mf_error_noexist_primary),
        MSG_MF_ERROR_NOEXIST_REVOKE (R.string.msg_mf_error_noexist_revoke),
        MSG_MF_ERROR_NULL_EXPIRY (R.string.msg_mf_error_null_expiry),
        MSG_MF_ERROR_PASSPHRASE_MASTER(R.string.msg_mf_error_passphrase_master),
        MSG_MF_ERROR_PAST_EXPIRY(R.string.msg_mf_error_past_expiry),
        MSG_MF_ERROR_PGP (R.string.msg_mf_error_pgp),
        MSG_MF_ERROR_REVOKED_PRIMARY (R.string.msg_mf_error_revoked_primary),
        MSG_MF_ERROR_SIG (R.string.msg_mf_error_sig),
        MSG_MF_ERROR_SUBKEY_MISSING(R.string.msg_mf_error_subkey_missing),
        MSG_MF_MASTER (R.string.msg_mf_master),
        MSG_MF_PASSPHRASE (R.string.msg_mf_passphrase),
        MSG_MF_PASSPHRASE_KEY (R.string.msg_mf_passphrase_key),
        MSG_MF_PASSPHRASE_EMPTY_RETRY (R.string.msg_mf_passphrase_empty_retry),
        MSG_MF_PASSPHRASE_FAIL (R.string.msg_mf_passphrase_fail),
        MSG_MF_PRIMARY_REPLACE_OLD (R.string.msg_mf_primary_replace_old),
        MSG_MF_PRIMARY_NEW (R.string.msg_mf_primary_new),
        MSG_MF_SUBKEY_CHANGE (R.string.msg_mf_subkey_change),
        MSG_MF_SUBKEY_NEW_ID (R.string.msg_mf_subkey_new_id),
        MSG_MF_SUBKEY_NEW (R.string.msg_mf_subkey_new),
        MSG_MF_SUBKEY_REVOKE (R.string.msg_mf_subkey_revoke),
        MSG_MF_SUCCESS (R.string.msg_mf_success),
        MSG_MF_UID_ADD (R.string.msg_mf_uid_add),
        MSG_MF_UID_PRIMARY (R.string.msg_mf_uid_primary),
        MSG_MF_UID_REVOKE (R.string.msg_mf_uid_revoke),
        MSG_MF_UID_ERROR_EMPTY (R.string.msg_mf_uid_error_empty),
        MSG_MF_UNLOCK_ERROR (R.string.msg_mf_unlock_error),
        MSG_MF_UNLOCK (R.string.msg_mf_unlock),
        ;

        private final int mMsgId;
        LogType(int msgId) {
            mMsgId = msgId;
        }
        public int getMsgId() {
            return mMsgId;
        }
    }

    /** Enumeration of possible log levels. */
    public static enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR, // should occur once at the end of a failed operation
        START, // should occur once at the start of each independent operation
        OK, // should occur once at the end of a successful operation
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mResult);
        dest.writeTypedList(mLog.toList());
    }

    public static final Creator<OperationResultParcel> CREATOR = new Creator<OperationResultParcel>() {
        public OperationResultParcel createFromParcel(final Parcel source) {
            return new OperationResultParcel(source);
        }

        public OperationResultParcel[] newArray(final int size) {
            return new OperationResultParcel[size];
        }
    };

    public static class OperationLog implements Iterable<LogEntryParcel> {

        private final List<LogEntryParcel> mParcels = new ArrayList<LogEntryParcel>();

        /// Simple convenience method
        public void add(LogLevel level, LogType type, int indent, Object... parameters) {
            mParcels.add(new OperationResultParcel.LogEntryParcel(level, type, indent, parameters));
        }

        public void add(LogLevel level, LogType type, int indent) {
            mParcels.add(new OperationResultParcel.LogEntryParcel(level, type, indent, (Object[]) null));
        }

        public boolean containsType(LogType type) {
            for(LogEntryParcel entry : new IterableIterator<LogEntryParcel>(mParcels.iterator())) {
                if (entry.mType == type) {
                    return true;
                }
            }
            return false;
        }

        public boolean containsWarnings() {
            for(LogEntryParcel entry : new IterableIterator<LogEntryParcel>(mParcels.iterator())) {
                if (entry.mLevel == LogLevel.WARN || entry.mLevel == LogLevel.ERROR) {
                    return true;
                }
            }
            return false;
        }

        public void addAll(List<LogEntryParcel> parcels) {
            mParcels.addAll(parcels);
        }

        public List<LogEntryParcel> toList() {
            return mParcels;
        }

        public boolean isEmpty() {
            return mParcels.isEmpty();
        }

        @Override
        public Iterator<LogEntryParcel> iterator() {
            return mParcels.iterator();
        }
    }

}

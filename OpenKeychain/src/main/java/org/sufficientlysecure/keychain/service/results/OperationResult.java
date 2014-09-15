/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.service.results;

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
 */
public abstract class OperationResult implements Parcelable {

    public static final String EXTRA_RESULT = "operation_result";

    /** Holds the overall result, the number specifying varying degrees of success:
     *  - The first bit is 0 on overall success, 1 on overall failure
     *  - The second bit indicates if the action was cancelled - may still be an error or success!
     *  - The third bit should be set if the operation succeeded with warnings
     * All other bits may be used for more specific conditions. */
    final int mResult;

    public static final int RESULT_OK = 0;
    public static final int RESULT_ERROR = 1;
    public static final int RESULT_CANCELLED = 2;
    public static final int RESULT_WARNINGS = 4;

    /// A list of log entries tied to the operation result.
    final OperationLog mLog;

    public OperationResult(int result, OperationLog log) {
        mResult = result;
        mLog = log;
    }

    public OperationResult(Parcel source) {
        mResult = source.readInt();
        mLog = new OperationLog();
        mLog.addAll(source.createTypedArrayList(LogEntryParcel.CREATOR));
    }

    public int getResult() {
        return mResult;
    }

    public boolean success() {
        return (mResult & RESULT_ERROR) == 0;
    }

    public boolean cancelled() {
        return (mResult & RESULT_CANCELLED) == RESULT_CANCELLED;
    }

    public OperationLog getLog() {
        return mLog;
    }

    /** One entry in the log. */
    public static class LogEntryParcel implements Parcelable {
        public final LogType mType;
        public final Object[] mParameters;
        public final int mIndent;

        public LogEntryParcel(LogType type, int indent, Object... parameters) {
            mType = type;
            mParameters = parameters;
            mIndent = indent;
            Log.v(Constants.TAG, "log: " + this.toString());
        }

        public LogEntryParcel(Parcel source) {
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
                    "mLevel=" + mType.mLevel +
                    ", mType=" + mType +
                    ", mParameters=" + Arrays.toString(mParameters) +
                    ", mIndent=" + mIndent +
                    '}';
        }
    }

    public SuperCardToast createNotify(final Activity activity) {

        int color;

        // Take the last message as string
        String str = activity.getString(mLog.getLast().mType.getMsgId());

        // Not an overall failure
        if (cancelled()) {
            color = Style.RED;
        } else if (success()) {
            if (getLog().containsWarnings()) {
                color = Style.ORANGE;
            } else {
                color = Style.GREEN;
            }
        } else {
            color = Style.RED;
        }

        boolean button = getLog() != null && !getLog().isEmpty();
        SuperCardToast toast = new SuperCardToast(activity,
                button ? SuperToast.Type.BUTTON : SuperToast.Type.STANDARD,
                Style.getStyle(color, SuperToast.Animations.POPUP));
        toast.setText(str);
        toast.setDuration(SuperToast.Duration.EXTRA_LONG);
        toast.setIndeterminate(false);
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
                            intent.putExtra(LogDisplayFragment.EXTRA_RESULT, OperationResult.this);
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

        MSG_INTERNAL_ERROR (LogLevel.ERROR, R.string.msg_internal_error),
        MSG_OPERATION_CANCELLED (LogLevel.CANCELLED, R.string.msg_cancelled),

        // import public
        MSG_IP(LogLevel.START, R.string.msg_ip),
        MSG_IP_APPLY_BATCH (LogLevel.DEBUG, R.string.msg_ip_apply_batch),
        MSG_IP_BAD_TYPE_SECRET (LogLevel.WARN, R.string.msg_ip_bad_type_secret),
        MSG_IP_DELETE_OLD_FAIL (LogLevel.DEBUG, R.string.msg_ip_delete_old_fail),
        MSG_IP_DELETE_OLD_OK (LogLevel.DEBUG, R.string.msg_ip_delete_old_ok),
        MSG_IP_ENCODE_FAIL (LogLevel.DEBUG, R.string.msg_ip_encode_fail),
        MSG_IP_ERROR_IO_EXC (LogLevel.ERROR, R.string.msg_ip_error_io_exc),
        MSG_IP_ERROR_OP_EXC (LogLevel.ERROR, R.string.msg_ip_error_op_exc),
        MSG_IP_ERROR_REMOTE_EX (LogLevel.ERROR, R.string.msg_ip_error_remote_ex),
        MSG_IP_INSERT_KEYRING (LogLevel.DEBUG, R.string.msg_ip_insert_keyring),
        MSG_IP_INSERT_SUBKEYS (LogLevel.DEBUG, R.string.msg_ip_insert_keys),
        MSG_IP_PREPARE (LogLevel.DEBUG, R.string.msg_ip_prepare),
        MSG_IP_REINSERT_SECRET (LogLevel.DEBUG, R.string.msg_ip_reinsert_secret),
        MSG_IP_MASTER (LogLevel.DEBUG, R.string.msg_ip_master),
        MSG_IP_MASTER_EXPIRED (LogLevel.DEBUG, R.string.msg_ip_master_expired),
        MSG_IP_MASTER_EXPIRES (LogLevel.DEBUG, R.string.msg_ip_master_expires),
        MSG_IP_MASTER_FLAGS_CES (LogLevel.DEBUG, R.string.msg_ip_master_flags_ces),
        MSG_IP_MASTER_FLAGS_CEX (LogLevel.DEBUG, R.string.msg_ip_master_flags_cex),
        MSG_IP_MASTER_FLAGS_CXS (LogLevel.DEBUG, R.string.msg_ip_master_flags_cxs),
        MSG_IP_MASTER_FLAGS_XES (LogLevel.DEBUG, R.string.msg_ip_master_flags_xes),
        MSG_IP_MASTER_FLAGS_CXX (LogLevel.DEBUG, R.string.msg_ip_master_flags_cxx),
        MSG_IP_MASTER_FLAGS_XEX (LogLevel.DEBUG, R.string.msg_ip_master_flags_xex),
        MSG_IP_MASTER_FLAGS_XXS (LogLevel.DEBUG, R.string.msg_ip_master_flags_xxs),
        MSG_IP_MASTER_FLAGS_XXX (LogLevel.DEBUG, R.string.msg_ip_master_flags_xxx),
        MSG_IP_SUBKEY (LogLevel.DEBUG, R.string.msg_ip_subkey),
        MSG_IP_SUBKEY_EXPIRED (LogLevel.DEBUG, R.string.msg_ip_subkey_expired),
        MSG_IP_SUBKEY_EXPIRES (LogLevel.DEBUG, R.string.msg_ip_subkey_expires),
        MSG_IP_SUBKEY_FLAGS_CES (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_ces),
        MSG_IP_SUBKEY_FLAGS_CEX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cex),
        MSG_IP_SUBKEY_FLAGS_CXS (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cxs),
        MSG_IP_SUBKEY_FLAGS_XES (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xes),
        MSG_IP_SUBKEY_FLAGS_CXX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cxx),
        MSG_IP_SUBKEY_FLAGS_XEX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xex),
        MSG_IP_SUBKEY_FLAGS_XXS (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xxs),
        MSG_IP_SUBKEY_FLAGS_XXX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xxx),
        MSG_IP_SUCCESS (LogLevel.OK, R.string.msg_ip_success),
        MSG_IP_SUCCESS_IDENTICAL (LogLevel.OK, R.string.msg_ip_success_identical),
        MSG_IP_UID_CERT_BAD (LogLevel.WARN, R.string.msg_ip_uid_cert_bad),
        MSG_IP_UID_CERT_ERROR (LogLevel.ERROR, R.string.msg_ip_uid_cert_error),
        MSG_IP_UID_CERT_GOOD (LogLevel.DEBUG, R.string.msg_ip_uid_cert_good),
        MSG_IP_UID_CERTS_UNKNOWN (LogLevel.DEBUG, R.plurals.msg_ip_uid_certs_unknown),
        MSG_IP_UID_CLASSIFYING_ZERO (LogLevel.DEBUG, R.string.msg_ip_uid_classifying_zero),
        MSG_IP_UID_CLASSIFYING (LogLevel.DEBUG, R.plurals.msg_ip_uid_classifying),
        MSG_IP_UID_REORDER(LogLevel.DEBUG, R.string.msg_ip_uid_reorder),
        MSG_IP_UID_PROCESSING (LogLevel.DEBUG, R.string.msg_ip_uid_processing),
        MSG_IP_UID_REVOKED (LogLevel.DEBUG, R.string.msg_ip_uid_revoked),

        // import secret
        MSG_IS(LogLevel.START, R.string.msg_is),
        MSG_IS_BAD_TYPE_PUBLIC (LogLevel.WARN, R.string.msg_is_bad_type_public),
        MSG_IS_DB_EXCEPTION (LogLevel.DEBUG, R.string.msg_is_db_exception),
        MSG_IS_ERROR_IO_EXC(LogLevel.DEBUG, R.string.msg_is_error_io_exc),
        MSG_IS_IMPORTING_SUBKEYS (LogLevel.DEBUG, R.string.msg_is_importing_subkeys),
        MSG_IS_PUBRING_GENERATE (LogLevel.DEBUG, R.string.msg_is_pubring_generate),
        MSG_IS_SUBKEY_NONEXISTENT (LogLevel.DEBUG, R.string.msg_is_subkey_nonexistent),
        MSG_IS_SUBKEY_OK (LogLevel.OK, R.string.msg_is_subkey_ok),
        MSG_IS_SUBKEY_STRIPPED (LogLevel.DEBUG, R.string.msg_is_subkey_stripped),
        MSG_IS_SUBKEY_DIVERT (LogLevel.DEBUG, R.string.msg_is_subkey_divert),
        MSG_IS_SUBKEY_EMPTY (LogLevel.DEBUG, R.string.msg_is_subkey_empty),
        MSG_IS_SUCCESS_IDENTICAL (LogLevel.OK, R.string.msg_is_success_identical),
        MSG_IS_SUCCESS (LogLevel.OK, R.string.msg_is_success),

        // keyring canonicalization
        MSG_KC_PUBLIC (LogLevel.DEBUG, R.string.msg_kc_public),
        MSG_KC_SECRET (LogLevel.DEBUG, R.string.msg_kc_secret),
        MSG_KC_ERROR_V3 (LogLevel.ERROR, R.string.msg_kc_error_v3),
        MSG_KC_ERROR_NO_UID (LogLevel.ERROR, R.string.msg_kc_error_no_uid),
        MSG_KC_ERROR_MASTER_ALGO (LogLevel.ERROR, R.string.msg_kc_error_master_algo),
        MSG_KC_MASTER (LogLevel.DEBUG, R.string.msg_kc_master),
        MSG_KC_REVOKE_BAD_ERR (LogLevel.WARN, R.string.msg_kc_revoke_bad_err),
        MSG_KC_REVOKE_BAD_LOCAL (LogLevel.WARN, R.string.msg_kc_revoke_bad_local),
        MSG_KC_REVOKE_BAD_TIME (LogLevel.WARN, R.string.msg_kc_revoke_bad_time),
        MSG_KC_REVOKE_BAD_TYPE (LogLevel.WARN, R.string.msg_kc_revoke_bad_type),
        MSG_KC_REVOKE_BAD_TYPE_UID (LogLevel.WARN, R.string.msg_kc_revoke_bad_type_uid),
        MSG_KC_REVOKE_BAD (LogLevel.WARN, R.string.msg_kc_revoke_bad),
        MSG_KC_REVOKE_DUP (LogLevel.DEBUG, R.string.msg_kc_revoke_dup),
        MSG_KC_SUB (LogLevel.DEBUG, R.string.msg_kc_sub),
        MSG_KC_SUB_BAD(LogLevel.WARN, R.string.msg_kc_sub_bad),
        MSG_KC_SUB_BAD_ERR(LogLevel.WARN, R.string.msg_kc_sub_bad_err),
        MSG_KC_SUB_BAD_LOCAL(LogLevel.WARN, R.string.msg_kc_sub_bad_local),
        MSG_KC_SUB_BAD_KEYID(LogLevel.WARN, R.string.msg_kc_sub_bad_keyid),
        MSG_KC_SUB_BAD_TIME(LogLevel.WARN, R.string.msg_kc_sub_bad_time),
        MSG_KC_SUB_BAD_TYPE(LogLevel.WARN, R.string.msg_kc_sub_bad_type),
        MSG_KC_SUB_DUP (LogLevel.DEBUG, R.string.msg_kc_sub_dup),
        MSG_KC_SUB_PRIMARY_BAD(LogLevel.WARN, R.string.msg_kc_sub_primary_bad),
        MSG_KC_SUB_PRIMARY_BAD_ERR(LogLevel.WARN, R.string.msg_kc_sub_primary_bad_err),
        MSG_KC_SUB_PRIMARY_NONE(LogLevel.DEBUG, R.string.msg_kc_sub_primary_none),
        MSG_KC_SUB_NO_CERT(LogLevel.DEBUG, R.string.msg_kc_sub_no_cert),
        MSG_KC_SUB_REVOKE_BAD_ERR (LogLevel.WARN, R.string.msg_kc_sub_revoke_bad_err),
        MSG_KC_SUB_REVOKE_BAD (LogLevel.WARN, R.string.msg_kc_sub_revoke_bad),
        MSG_KC_SUB_REVOKE_DUP (LogLevel.DEBUG, R.string.msg_kc_sub_revoke_dup),
        MSG_KC_SUB_UNKNOWN_ALGO (LogLevel.WARN, R.string.msg_kc_sub_unknown_algo),
        MSG_KC_SUCCESS_BAD (LogLevel.OK, R.plurals.msg_kc_success_bad),
        MSG_KC_SUCCESS_BAD_AND_RED (LogLevel.OK, R.string.msg_kc_success_bad_and_red),
        MSG_KC_SUCCESS_REDUNDANT (LogLevel.OK, R.plurals.msg_kc_success_redundant),
        MSG_KC_SUCCESS (LogLevel.OK, R.string.msg_kc_success),
        MSG_KC_UID_BAD_ERR (LogLevel.WARN, R.string.msg_kc_uid_bad_err),
        MSG_KC_UID_BAD_LOCAL (LogLevel.WARN, R.string.msg_kc_uid_bad_local),
        MSG_KC_UID_BAD_TIME (LogLevel.WARN, R.string.msg_kc_uid_bad_time),
        MSG_KC_UID_BAD_TYPE (LogLevel.WARN, R.string.msg_kc_uid_bad_type),
        MSG_KC_UID_BAD (LogLevel.WARN, R.string.msg_kc_uid_bad),
        MSG_KC_UID_CERT_DUP (LogLevel.DEBUG, R.string.msg_kc_uid_cert_dup),
        MSG_KC_UID_DUP (LogLevel.DEBUG, R.string.msg_kc_uid_dup),
        MSG_KC_UID_FOREIGN (LogLevel.DEBUG, R.string.msg_kc_uid_foreign),
        MSG_KC_UID_NO_CERT (LogLevel.DEBUG, R.string.msg_kc_uid_no_cert),
        MSG_KC_UID_REVOKE_DUP (LogLevel.DEBUG, R.string.msg_kc_uid_revoke_dup),
        MSG_KC_UID_REVOKE_OLD (LogLevel.DEBUG, R.string.msg_kc_uid_revoke_old),
        MSG_KC_UID_REMOVE (LogLevel.DEBUG, R.string.msg_kc_uid_remove),
        MSG_KC_UID_WARN_ENCODING (LogLevel.WARN, R.string.msg_kc_uid_warn_encoding),


        // keyring consolidation
        MSG_MG_ERROR_SECRET_DUMMY(LogLevel.ERROR, R.string.msg_mg_error_secret_dummy),
        MSG_MG_ERROR_ENCODE(LogLevel.ERROR, R.string.msg_mg_error_encode),
        MSG_MG_ERROR_HETEROGENEOUS(LogLevel.ERROR, R.string.msg_mg_error_heterogeneous),
        MSG_MG_PUBLIC (LogLevel.START, R.string.msg_mg_public),
        MSG_MG_SECRET (LogLevel.START, R.string.msg_mg_secret),
        MSG_MG_NEW_SUBKEY (LogLevel.DEBUG, R.string.msg_mg_new_subkey),
        MSG_MG_FOUND_NEW (LogLevel.OK, R.string.msg_mg_found_new),
        MSG_MG_UNCHANGED (LogLevel.OK, R.string.msg_mg_unchanged),

        // secret key create
        MSG_CR (LogLevel.START, R.string.msg_cr),
        MSG_CR_ERROR_NO_MASTER (LogLevel.ERROR, R.string.msg_cr_error_no_master),
        MSG_CR_ERROR_NO_USER_ID (LogLevel.ERROR, R.string.msg_cr_error_no_user_id),
        MSG_CR_ERROR_NO_CERTIFY (LogLevel.ERROR, R.string.msg_cr_error_no_certify),
        MSG_CR_ERROR_NULL_EXPIRY(LogLevel.ERROR, R.string.msg_cr_error_null_expiry),
        MSG_CR_ERROR_KEYSIZE_512 (LogLevel.ERROR, R.string.msg_cr_error_keysize_512),
        MSG_CR_ERROR_NO_KEYSIZE (LogLevel.ERROR, R.string.msg_cr_error_no_keysize),
        MSG_CR_ERROR_NO_CURVE (LogLevel.ERROR, R.string.msg_cr_error_no_curve),
        MSG_CR_ERROR_UNKNOWN_ALGO (LogLevel.ERROR, R.string.msg_cr_error_unknown_algo),
        MSG_CR_ERROR_INTERNAL_PGP (LogLevel.ERROR, R.string.msg_cr_error_internal_pgp),
        MSG_CR_ERROR_FLAGS_DSA (LogLevel.ERROR, R.string.msg_cr_error_flags_dsa),
        MSG_CR_ERROR_FLAGS_ELGAMAL (LogLevel.ERROR, R.string.msg_cr_error_flags_elgamal),
        MSG_CR_ERROR_FLAGS_ECDSA (LogLevel.ERROR, R.string.msg_cr_error_flags_ecdsa),
        MSG_CR_ERROR_FLAGS_ECDH (LogLevel.ERROR, R.string.msg_cr_error_flags_ecdh),

        // secret key modify
        MSG_MF (LogLevel.START, R.string.msg_mr),
        MSG_MF_ERROR_ENCODE (LogLevel.ERROR, R.string.msg_mf_error_encode),
        MSG_MF_ERROR_FINGERPRINT (LogLevel.ERROR, R.string.msg_mf_error_fingerprint),
        MSG_MF_ERROR_KEYID (LogLevel.ERROR, R.string.msg_mf_error_keyid),
        MSG_MF_ERROR_INTEGRITY (LogLevel.ERROR, R.string.msg_mf_error_integrity),
        MSG_MF_ERROR_MASTER_NONE(LogLevel.ERROR, R.string.msg_mf_error_master_none),
        MSG_MF_ERROR_NO_CERTIFY (LogLevel.ERROR, R.string.msg_cr_error_no_certify),
        MSG_MF_ERROR_NOEXIST_PRIMARY (LogLevel.ERROR, R.string.msg_mf_error_noexist_primary),
        MSG_MF_ERROR_NOEXIST_REVOKE (LogLevel.ERROR, R.string.msg_mf_error_noexist_revoke),
        MSG_MF_ERROR_NULL_EXPIRY (LogLevel.ERROR, R.string.msg_mf_error_null_expiry),
        MSG_MF_ERROR_PASSPHRASE_MASTER(LogLevel.ERROR, R.string.msg_mf_error_passphrase_master),
        MSG_MF_ERROR_PAST_EXPIRY(LogLevel.ERROR, R.string.msg_mf_error_past_expiry),
        MSG_MF_ERROR_PGP (LogLevel.ERROR, R.string.msg_mf_error_pgp),
        MSG_MF_ERROR_REVOKED_PRIMARY (LogLevel.ERROR, R.string.msg_mf_error_revoked_primary),
        MSG_MF_ERROR_SIG (LogLevel.ERROR, R.string.msg_mf_error_sig),
        MSG_MF_ERROR_SUBKEY_MISSING(LogLevel.ERROR, R.string.msg_mf_error_subkey_missing),
        MSG_MF_MASTER (LogLevel.DEBUG, R.string.msg_mf_master),
        MSG_MF_PASSPHRASE (LogLevel.INFO, R.string.msg_mf_passphrase),
        MSG_MF_PASSPHRASE_KEY (LogLevel.DEBUG, R.string.msg_mf_passphrase_key),
        MSG_MF_PASSPHRASE_EMPTY_RETRY (LogLevel.DEBUG, R.string.msg_mf_passphrase_empty_retry),
        MSG_MF_PASSPHRASE_FAIL (LogLevel.DEBUG, R.string.msg_mf_passphrase_fail),
        MSG_MF_PRIMARY_REPLACE_OLD (LogLevel.DEBUG, R.string.msg_mf_primary_replace_old),
        MSG_MF_PRIMARY_NEW (LogLevel.DEBUG, R.string.msg_mf_primary_new),
        MSG_MF_SUBKEY_CHANGE (LogLevel.INFO, R.string.msg_mf_subkey_change),
        MSG_MF_SUBKEY_NEW_ID (LogLevel.DEBUG, R.string.msg_mf_subkey_new_id),
        MSG_MF_SUBKEY_NEW (LogLevel.INFO, R.string.msg_mf_subkey_new),
        MSG_MF_SUBKEY_REVOKE (LogLevel.INFO, R.string.msg_mf_subkey_revoke),
        MSG_MF_SUBKEY_STRIP (LogLevel.INFO, R.string.msg_mf_subkey_strip),
        MSG_MF_SUCCESS (LogLevel.OK, R.string.msg_mf_success),
        MSG_MF_UID_ADD (LogLevel.INFO, R.string.msg_mf_uid_add),
        MSG_MF_UID_PRIMARY (LogLevel.INFO, R.string.msg_mf_uid_primary),
        MSG_MF_UID_REVOKE (LogLevel.INFO, R.string.msg_mf_uid_revoke),
        MSG_MF_UID_ERROR_EMPTY (LogLevel.ERROR, R.string.msg_mf_uid_error_empty),
        MSG_MF_UNLOCK_ERROR (LogLevel.ERROR, R.string.msg_mf_unlock_error),
        MSG_MF_UNLOCK (LogLevel.DEBUG, R.string.msg_mf_unlock),

        // consolidate
        MSG_CON_CRITICAL_IN (LogLevel.DEBUG, R.string.msg_con_critical_in),
        MSG_CON_CRITICAL_OUT (LogLevel.DEBUG, R.string.msg_con_critical_out),
        MSG_CON_DB_CLEAR (LogLevel.DEBUG, R.string.msg_con_db_clear),
        MSG_CON_DELETE_PUBLIC (LogLevel.DEBUG, R.string.msg_con_delete_public),
        MSG_CON_DELETE_SECRET (LogLevel.DEBUG, R.string.msg_con_delete_secret),
        MSG_CON_ERROR_BAD_STATE (LogLevel.ERROR, R.string.msg_con_error_bad_state),
        MSG_CON_ERROR_CONCURRENT(LogLevel.ERROR, R.string.msg_con_error_concurrent),
        MSG_CON_ERROR_DB (LogLevel.ERROR, R.string.msg_con_error_db),
        MSG_CON_ERROR_IO_PUBLIC (LogLevel.ERROR, R.string.msg_con_error_io_public),
        MSG_CON_ERROR_IO_SECRET (LogLevel.ERROR, R.string.msg_con_error_io_secret),
        MSG_CON_ERROR_PUBLIC (LogLevel.ERROR, R.string.msg_con_error_public),
        MSG_CON_ERROR_SECRET (LogLevel.ERROR, R.string.msg_con_error_secret),
        MSG_CON_RECOVER (LogLevel.DEBUG, R.plurals.msg_con_recover),
        MSG_CON_RECOVER_UNKNOWN (LogLevel.DEBUG, R.string.msg_con_recover_unknown),
        MSG_CON_REIMPORT_PUBLIC (LogLevel.DEBUG, R.plurals.msg_con_reimport_public),
        MSG_CON_REIMPORT_PUBLIC_SKIP (LogLevel.DEBUG, R.string.msg_con_reimport_public_skip),
        MSG_CON_REIMPORT_SECRET (LogLevel.DEBUG, R.plurals.msg_con_reimport_secret),
        MSG_CON_REIMPORT_SECRET_SKIP (LogLevel.DEBUG, R.string.msg_con_reimport_secret_skip),
        MSG_CON (LogLevel.START, R.string.msg_con),
        MSG_CON_SAVE_PUBLIC (LogLevel.DEBUG, R.string.msg_con_save_public),
        MSG_CON_SAVE_SECRET (LogLevel.DEBUG, R.string.msg_con_save_secret),
        MSG_CON_SUCCESS (LogLevel.OK, R.string.msg_con_success),
        MSG_CON_WARN_DELETE_PUBLIC (LogLevel.WARN, R.string.msg_con_warn_delete_public),
        MSG_CON_WARN_DELETE_SECRET (LogLevel.WARN, R.string.msg_con_warn_delete_secret),

        // messages used in UI code
        MSG_EK_ERROR_DIVERT (LogLevel.ERROR, R.string.msg_ek_error_divert),
        MSG_EK_ERROR_DUMMY (LogLevel.ERROR, R.string.msg_ek_error_dummy),
        MSG_EK_ERROR_NOT_FOUND (LogLevel.ERROR, R.string.msg_ek_error_not_found),

        // decryptverify
        MSG_DC_ASKIP_NO_KEY (LogLevel.DEBUG, R.string.msg_dc_askip_no_key),
        MSG_DC_ASKIP_NOT_ALLOWED (LogLevel.DEBUG, R.string.msg_dc_askip_not_allowed),
        MSG_DC_ASYM (LogLevel.DEBUG, R.string.msg_dc_asym),
        MSG_DC_CLEAR_DATA (LogLevel.DEBUG, R.string.msg_dc_clear_data),
        MSG_DC_CLEAR_DECOMPRESS (LogLevel.DEBUG, R.string.msg_dc_clear_decompress),
        MSG_DC_CLEAR_META_FILE (LogLevel.DEBUG, R.string.msg_dc_clear_meta_file),
        MSG_DC_CLEAR_META_MIME (LogLevel.DEBUG, R.string.msg_dc_clear_meta_mime),
        MSG_DC_CLEAR_META_SIZE (LogLevel.DEBUG, R.string.msg_dc_clear_meta_size),
        MSG_DC_CLEAR_META_TIME (LogLevel.DEBUG, R.string.msg_dc_clear_meta_time),
        MSG_DC_CLEAR (LogLevel.DEBUG, R.string.msg_dc_clear),
        MSG_DC_CLEAR_SIGNATURE_BAD (LogLevel.WARN, R.string.msg_dc_clear_signature_bad),
        MSG_DC_CLEAR_SIGNATURE_CHECK (LogLevel.DEBUG, R.string.msg_dc_clear_signature_check),
        MSG_DC_CLEAR_SIGNATURE_OK (LogLevel.OK, R.string.msg_dc_clear_signature_ok),
        MSG_DC_CLEAR_SIGNATURE (LogLevel.DEBUG, R.string.msg_dc_clear_signature),
        MSG_DC_ERROR_BAD_PASSPHRASE (LogLevel.ERROR, R.string.msg_dc_error_bad_passphrase),
        MSG_DC_ERROR_EXTRACT_KEY (LogLevel.ERROR, R.string.msg_dc_error_extract_key),
        MSG_DC_ERROR_INTEGRITY_CHECK (LogLevel.ERROR, R.string.msg_dc_error_integrity_check),
        MSG_DC_ERROR_INVALID_SIGLIST(LogLevel.ERROR, R.string.msg_dc_error_invalid_siglist),
        MSG_DC_ERROR_IO (LogLevel.ERROR, R.string.msg_dc_error_io),
        MSG_DC_ERROR_NO_DATA (LogLevel.ERROR, R.string.msg_dc_error_no_data),
        MSG_DC_ERROR_NO_KEY (LogLevel.ERROR, R.string.msg_dc_error_no_key),
        MSG_DC_ERROR_PGP_EXCEPTION (LogLevel.ERROR, R.string.msg_dc_error_pgp_exception),
        MSG_DC_INTEGRITY_CHECK_OK (LogLevel.INFO, R.string.msg_dc_integrity_check_ok),
        MSG_DC_OK_META_ONLY (LogLevel.OK, R.string.msg_dc_ok_meta_only),
        MSG_DC_OK (LogLevel.OK, R.string.msg_dc_ok),
        MSG_DC_PASS_CACHED (LogLevel.DEBUG, R.string.msg_dc_pass_cached),
        MSG_DC_PENDING_NFC (LogLevel.INFO, R.string.msg_dc_pending_nfc),
        MSG_DC_PENDING_PASSPHRASE (LogLevel.INFO, R.string.msg_dc_pending_passphrase),
        MSG_DC_PREP_STREAMS (LogLevel.DEBUG, R.string.msg_dc_prep_streams),
        MSG_DC (LogLevel.DEBUG, R.string.msg_dc),
        MSG_DC_SYM (LogLevel.DEBUG, R.string.msg_dc_sym),
        MSG_DC_SYM_SKIP (LogLevel.DEBUG, R.string.msg_dc_sym_skip),
        MSG_DC_TRAIL_ASYM (LogLevel.DEBUG, R.string.msg_dc_trail_asym),
        MSG_DC_TRAIL_SYM (LogLevel.DEBUG, R.string.msg_dc_trail_sym),
        MSG_DC_TRAIL_UNKNOWN (LogLevel.DEBUG, R.string.msg_dc_trail_unknown),
        MSG_DC_UNLOCKING (LogLevel.INFO, R.string.msg_dc_unlocking),

        // signencrypt
        MSG_SE_ASYMMETRIC (LogLevel.INFO, R.string.msg_se_asymmetric),
        MSG_SE_CLEARSIGN_ONLY (LogLevel.DEBUG, R.string.msg_se_clearsign_only),
        MSG_SE_COMPRESSING (LogLevel.DEBUG, R.string.msg_se_compressing),
        MSG_SE_ENCRYPTING (LogLevel.DEBUG, R.string.msg_se_encrypting),
        MSG_SE_ERROR_BAD_PASSPHRASE (LogLevel.ERROR, R.string.msg_se_error_bad_passphrase),
        MSG_SE_ERROR_IO (LogLevel.ERROR, R.string.msg_se_error_io),
        MSG_SE_ERROR_SIGN_KEY(LogLevel.ERROR, R.string.msg_se_error_sign_key),
        MSG_SE_ERROR_KEY_SIGN (LogLevel.ERROR, R.string.msg_se_error_key_sign),
        MSG_SE_ERROR_NFC (LogLevel.ERROR, R.string.msg_se_error_nfc),
        MSG_SE_ERROR_NO_PASSPHRASE (LogLevel.ERROR, R.string.msg_se_error_no_passphrase),
        MSG_SE_ERROR_PGP (LogLevel.ERROR, R.string.msg_se_error_pgp),
        MSG_SE_ERROR_SIG (LogLevel.ERROR, R.string.msg_se_error_sig),
        MSG_SE_ERROR_UNLOCK (LogLevel.ERROR, R.string.msg_se_error_unlock),
        MSG_SE_KEY_OK (LogLevel.OK, R.string.msg_se_key_ok),
        MSG_SE_KEY_UNKNOWN (LogLevel.DEBUG, R.string.msg_se_key_unknown),
        MSG_SE_KEY_WARN (LogLevel.WARN, R.string.msg_se_key_warn),
        MSG_SE_OK (LogLevel.OK, R.string.msg_se_ok),
        MSG_SE_PENDING_NFC (LogLevel.INFO, R.string.msg_se_pending_nfc),
        MSG_SE (LogLevel.DEBUG, R.string.msg_se),
        MSG_SE_SIGNING (LogLevel.DEBUG, R.string.msg_se_signing),
        MSG_SE_SIGCRYPTING (LogLevel.DEBUG, R.string.msg_se_sigcrypting),
        MSG_SE_SYMMETRIC (LogLevel.INFO, R.string.msg_se_symmetric),

        MSG_CRT_UPLOAD_SUCCESS (LogLevel.OK, R.string.msg_crt_upload_success),
        MSG_CRT_SUCCESS (LogLevel.OK, R.string.msg_crt_success),

        MSG_ACC_SAVED (LogLevel.INFO, R.string.api_settings_save)

        ;

        public final int mMsgId;
        public final LogLevel mLevel;
        LogType(LogLevel level, int msgId) {
            mLevel = level;
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
        CANCELLED, // should occur once at the end of a cancelled operation
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mResult);
        if (mLog != null) {
            dest.writeTypedList(mLog.toList());
        }
    }

    public static class OperationLog implements Iterable<LogEntryParcel> {

        private final List<LogEntryParcel> mParcels = new ArrayList<LogEntryParcel>();

        /// Simple convenience method
        public void add(LogType type, int indent, Object... parameters) {
            mParcels.add(new OperationResult.LogEntryParcel(type, indent, parameters));
        }

        public void add(LogType type, int indent) {
            mParcels.add(new OperationResult.LogEntryParcel(type, indent, (Object[]) null));
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
                if (entry.mType.mLevel == LogLevel.WARN || entry.mType.mLevel == LogLevel.ERROR) {
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

        public LogEntryParcel getLast() {
            if (mParcels.isEmpty()) {
                return null;
            }
            return mParcels.get(mParcels.size() -1);
        }

        @Override
        public Iterator<LogEntryParcel> iterator() {
            return mParcels.iterator();
        }
    }

}

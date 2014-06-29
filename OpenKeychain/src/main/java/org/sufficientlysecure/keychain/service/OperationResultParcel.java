package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;

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
        }
        public LogEntryParcel(LogLevel level, LogType type, Object... parameters) {
            this(level, type, 0, parameters);
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
        MSG_KC_PUBLIC (R.string.msg_kc_public),
        MSG_KC_SECRET (R.string.msg_kc_secret),
        MSG_KC_FATAL_NO_UID (R.string.msg_kc_fatal_no_uid),
        MSG_KC_MASTER (R.string.msg_kc_master),
        MSG_KC_REVOKE_BAD_ERR (R.string.msg_kc_revoke_bad_err),
        MSG_KC_REVOKE_BAD_LOCAL (R.string.msg_kc_revoke_bad_local),
        MSG_KC_REVOKE_BAD_TIME (R.string.msg_kc_revoke_bad_time),
        MSG_KC_REVOKE_BAD_TYPE (R.string.msg_kc_revoke_bad_type),
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


        // keyring consolidation
        MSG_MG_PUBLIC (R.string.msg_mg_public),
        MSG_MG_SECRET (R.string.msg_mg_secret),
        MSG_MG_FATAL_ENCODE (R.string.msg_mg_fatal_encode),
        MSG_MG_HETEROGENEOUS (R.string.msg_mg_heterogeneous),
        MSG_MG_NEW_SUBKEY (R.string.msg_mg_new_subkey),
        MSG_MG_FOUND_NEW (R.string.msg_mg_found_new),

        // secret key create
        MSG_CR_ERROR_NO_MASTER (R.string.msg_mr),

        // secret key modify
        MSG_MF (R.string.msg_mr),
        MSG_MF_ERROR_ENCODE (R.string.msg_mf_error_encode),
        MSG_MF_ERROR_FINGERPRINT (R.string.msg_mf_error_fingerprint),
        MSG_MF_ERROR_KEYID (R.string.msg_mf_error_keyid),
        MSG_MF_ERROR_PGP (R.string.msg_mf_error_pgp),
        MSG_MF_ERROR_SIG (R.string.msg_mf_error_sig),
        MSG_MF_PASSPHRASE (R.string.msg_mf_passphrase),
        MSG_MF_SUBKEY_CHANGE (R.string.msg_mf_subkey_change),
        MSG_MF_SUBKEY_MISSING (R.string.msg_mf_subkey_missing),
        MSG_MF_SUBKEY_NEW_ID (R.string.msg_mf_subkey_new_id),
        MSG_MF_SUBKEY_NEW (R.string.msg_mf_subkey_new),
        MSG_MF_SUBKEY_PAST_EXPIRY (R.string.msg_mf_subkey_past_expiry),
        MSG_MF_SUBKEY_REVOKE (R.string.msg_mf_subkey_revoke),
        MSG_MF_SUCCESS (R.string.msg_mf_success),
        MSG_MF_UID_ADD (R.string.msg_mf_uid_add),
        MSG_MF_UID_PRIMARY (R.string.msg_mf_uid_primary),
        MSG_MF_UID_REVOKE (R.string.msg_mf_uid_revoke),
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
        dest.writeTypedList(mLog);
    }

    public static final Creator<OperationResultParcel> CREATOR = new Creator<OperationResultParcel>() {
        public OperationResultParcel createFromParcel(final Parcel source) {
            return new OperationResultParcel(source);
        }

        public OperationResultParcel[] newArray(final int size) {
            return new OperationResultParcel[size];
        }
    };

    public static class OperationLog extends ArrayList<LogEntryParcel> {

        /// Simple convenience method
        public void add(LogLevel level, LogType type, int indent, Object... parameters) {
            Log.d(Constants.TAG, type.toString());
            add(new OperationResultParcel.LogEntryParcel(level, type, indent, parameters));
        }

        public void add(LogLevel level, LogType type, int indent) {
            add(new OperationResultParcel.LogEntryParcel(level, type, indent, (Object[]) null));
        }

        public boolean containsWarnings() {
            for(LogEntryParcel entry : new IterableIterator<LogEntryParcel>(iterator())) {
                if (entry.mLevel == LogLevel.WARN || entry.mLevel == LogLevel.ERROR) {
                    return true;
                }
            }
            return false;
        }

    }

}

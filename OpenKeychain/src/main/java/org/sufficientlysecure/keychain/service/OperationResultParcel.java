package org.sufficientlysecure.keychain.service;

import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.util.ArrayList;

/** Represent the result of an operation.
 *
 * This class holds a result and the log of an operation. It can be subclassed
 * to include typed additional information specific to the operation. To keep
 * the class structure (somewhat) simple, this class contains an exhaustive
 * list (ie, enum) of all possible log types, which should in all cases be tied
 * to string resource ids.
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
        public final String[] mParameters;
        public final int mIndent;

        public LogEntryParcel(LogLevel level, LogType type, String[] parameters, int indent) {
            mLevel = level;
            mType = type;
            mParameters = parameters;
            mIndent = indent;
        }
        public LogEntryParcel(LogLevel level, LogType type, String[] parameters) {
            this(level, type, parameters, 0);
        }

        public LogEntryParcel(Parcel source) {
            mLevel = LogLevel.values()[source.readInt()];
            mType = LogType.values()[source.readInt()];
            mParameters = source.createStringArray();
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
            dest.writeStringArray(mParameters);
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

    public static enum LogType {

        // import public
        MSG_IP(R.string.msg_ip),
        MSG_IP_APPLY_BATCH (R.string.msg_ip_apply_batch),
        MSG_IP_BAD_TYPE_SECRET (R.string.msg_ip_bad_type_secret),
        MSG_IP_DELETE_OLD_FAIL (R.string.msg_ip_delete_old_fail),
        MSG_IP_DELETE_OLD_OK (R.string.msg_ip_delete_old_ok),
        MSG_IP_ENCODE_FAIL (R.string.msg_ip_encode_fail),
        MSG_IP_FAIL_IO_EXC (R.string.msg_ip_fail_io_exc),
        MSG_IP_FAIL_OP_EX (R.string.msg_ip_fail_op_ex),
        MSG_IP_FAIL_REMOTE_EX (R.string.msg_ip_fail_remote_ex),
        MSG_IP_INSERT_KEYRING (R.string.msg_ip_insert_keyring),
        MSG_IP_INSERT_SUBKEYS (R.string.msg_ip_insert_subkeys),
        MSG_IP_PRESERVING_SECRET (R.string.msg_ip_preserving_secret),
        MSG_IP_REINSERT_SECRET (R.string.msg_ip_reinsert_secret),
        MSG_IP_SUBKEY (R.string.msg_ip_subkey),
        MSG_IP_SUBKEY_EXPIRED (R.string.msg_ip_subkey_expired),
        MSG_IP_SUBKEY_EXPIRES (R.string.msg_ip_subkey_expires),
        MSG_IP_SUBKEY_FLAGS (R.string.msg_ip_subkey_flags),
        MSG_IP_SUBKEY_FLAGS_CES (R.string.msg_ip_subkey_flags_ces),
        MSG_IP_SUBKEY_FLAGS_CEX (R.string.msg_ip_subkey_flags_cex),
        MSG_IP_SUBKEY_FLAGS_CXS (R.string.msg_ip_subkey_flags_cxs),
        MSG_IP_SUBKEY_FLAGS_XES (R.string.msg_ip_subkey_flags_xes),
        MSG_IP_SUBKEY_FLAGS_CXX (R.string.msg_ip_subkey_flags_cxx),
        MSG_IP_SUBKEY_FLAGS_XEX (R.string.msg_ip_subkey_flags_xex),
        MSG_IP_SUBKEY_FLAGS_XXS (R.string.msg_ip_subkey_flags_xxs),
        MSG_IP_SUBKEY_FLAGS_XXX (R.string.msg_ip_subkey_flags_xxx),
        MSG_IP_SUBKEY_FUTURE (R.string.msg_ip_subkey_future),
        MSG_IP_SUCCESS (R.string.msg_ip_success),
        MSG_IP_TRUST_RETRIEVE (R.string.msg_ip_trust_retrieve),
        MSG_IP_TRUST_USING (R.string.msg_ip_trust_using),
        MSG_IP_UID_CERT_BAD (R.string.msg_ip_uid_cert_bad),
        MSG_IP_UID_CERT_ERROR (R.string.msg_ip_uid_cert_error),
        MSG_IP_UID_CERT_GOOD (R.string.msg_ip_uid_cert_good),
        MSG_IP_UID_CERTS_UNKNOWN (R.string.msg_ip_uid_certs_unknown),
        MSG_IP_UID_CLASSIFYING (R.string.msg_ip_uid_classifying),
        MSG_IP_UID_INSERT (R.string.msg_ip_uid_insert),
        MSG_IP_UID_PROCESSING (R.string.msg_ip_uid_processing),
        MSG_IP_UID_REVOKED (R.string.msg_ip_uid_revoked),
        MSG_IP_UID_SELF_BAD (R.string.msg_ip_uid_self_bad),
        MSG_IP_UID_SELF_GOOD (R.string.msg_ip_uid_self_good),
        MSG_IP_UID_SELF_IGNORING_OLD (R.string.msg_ip_uid_self_ignoring_old),
        MSG_IP_UID_SELF_NEWER (R.string.msg_ip_uid_self_newer),

        // import secret
        MSG_IS(R.string.msg_is),
        MSG_IS_BAD_TYPE_PUBLIC (R.string.msg_is_bad_type_public),
        MSG_IS_IMPORTING_SUBKEYS (R.string.msg_is_importing_subkeys),
        MSG_IS_IO_EXCPTION (R.string.msg_is_io_excption),
        MSG_IS_SUBKEY_NONEXISTENT (R.string.msg_is_subkey_nonexistent),
        MSG_IS_SUBKEY_OK (R.string.msg_is_subkey_ok),
        MSG_IS_SUBKEY_STRIPPED (R.string.msg_is_subkey_stripped),
        MSG_IS_SUCCESS (R.string.msg_is_success),

        // keyring canonicalization
        MSG_KC (R.string.msg_kc),
        MSG_KC_CERT_BAD_ERR (R.string.msg_kc_cert_bad_err),
        MSG_KC_CERT_BAD_KEYID (R.string.msg_kc_cert_bad_keyid),
        MSG_KC_CERT_BAD (R.string.msg_kc_cert_bad),
        MSG_KC_CERT_BAD_TYPE (R.string.msg_kc_cert_bad_type),
        MSG_KC_MASTER (R.string.msg_kc_master),
        MSG_KC_MASTER_SUCCESS (R.string.msg_kc_master_success),
        MSG_KC_REVOKE_BAD_ERR (R.string.msg_kc_revoke_bad_err),
        MSG_KC_REVOKE_BAD (R.string.msg_kc_revoke_bad),
        MSG_KC_REVOKE_DUP (R.string.msg_kc_revoke_dup),
        MSG_KC_SUBKEY_NO_CERT (R.string.msg_kc_subkey_no_cert),
        MSG_KC_SUBKEY (R.string.msg_kc_subkey),
        MSG_KC_SUBKEY_SUCCESS (R.string.msg_kc_subkey_success),
        MSG_KC_SUCCESS_REMOVED (R.string.msg_kc_success_removed),
        MSG_KC_SUCCESS (R.string.msg_kc_success),
        MSG_KC_UID_BAD_ERR (R.string.msg_kc_uid_bad_err),
        MSG_KC_UID_BAD (R.string.msg_kc_uid_bad),
        MSG_KC_UID_DUP (R.string.msg_kc_uid_dup),
        MSG_KC_UID_REVOKE_DUP (R.string.msg_kc_uid_revoke_dup),
        MSG_KC_UID_REVOKE_OLD (R.string.msg_kc_uid_revoke_old),
        MSG_KC_UID_UNKNOWN_CERT (R.string.msg_kc_uid_unknown_cert),
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
        public void add(LogLevel level, LogType type, String[] parameters, int indent) {
            add(new OperationResultParcel.LogEntryParcel(level, type, parameters, indent));
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

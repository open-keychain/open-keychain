package org.sufficientlysecure.keychain.pgp;

import android.os.Parcel;
import android.os.Parcelable;

import org.sufficientlysecure.keychain.R;

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
    /** Holds the overall result. A value of 0 is considered a success, all
     * other values may represent failure or varying degrees of success. */
    final int mResult;

    /// A list of log entries tied to the operation result.
    final ArrayList<LogEntryParcel> mLog;

    public OperationResultParcel(int result, ArrayList<LogEntryParcel> log) {
        mResult = result;
        mLog = log;
    }

    public OperationResultParcel(Parcel source) {
        mResult = source.readInt();
        mLog = source.createTypedArrayList(LogEntryParcel.CREATOR);
    }

    public boolean isSuccessful() {
        return mResult == 0;
    }

    /** One entry in the log. */
    public static class LogEntryParcel implements Parcelable {
        final LogLevel mLevel;
        final LogType mType;
        final String[] mParameters;
        final int mIndent;

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
        MSG_IP_APPLY_BATCH (R.string.msg_ip_apply_batch),
        MSG_IP_BAD_TYPE_SECRET (R.string.msg_ip_bad_type_secret),
        MSG_IP_DELETE_OLD_FAIL (R.string.msg_ip_delete_old_fail),
        MSG_IP_DELETE_OLD_OK (R.string.msg_ip_delete_old_ok),
        MSG_IP_ENCODE_FAIL (R.string.msg_ip_encode_fail),
        MSG_IP_FAIL_IO_EXC (R.string.msg_ip_fail_io_exc),
        MSG_IP_FAIL_OP_EX (R.string.msg_ip_fail_op_ex),
        MSG_IP_FAIL_REMOTE_EX (R.string.msg_ip_fail_remote_ex),
        MSG_IP_IMPORTING (R.string.msg_ip_importing),
        MSG_IP_INSERT_KEYRING (R.string.msg_ip_insert_keyring),
        MSG_IP_INSERT_SUBKEY (R.string.msg_ip_insert_subkey),
        MSG_IP_INSERT_SUBKEYS (R.string.msg_ip_insert_subkeys),
        MSG_IP_PRESERVING_SECRET (R.string.msg_ip_preserving_secret),
        MSG_IP_REINSERT_SECRET (R.string.msg_ip_reinsert_secret),
        MSG_IP_SUCCESS (R.string.msg_ip_success),
        MSG_IP_TRUST_RETRIEVE (R.string.msg_ip_trust_retrieve),
        MSG_IP_TRUST_USING (R.string.msg_ip_trust_using),
        MSG_IP_TRUST_USING_SEC (R.string.msg_ip_trust_using_sec),
        MSG_IP_UID_CERT_BAD (R.string.msg_ip_uid_cert_bad),
        MSG_IP_UID_CERT_ERROR (R.string.msg_ip_uid_cert_error),
        MSG_IP_UID_CERT_GOOD (R.string.msg_ip_uid_cert_good),
        MSG_IP_UID_CERTS_UNKNOWN (R.string.msg_ip_uid_certs_unknown),
        MSG_IP_UID_CLASSIFYING (R.string.msg_ip_uid_classifying),
        MSG_IP_UID_INSERT (R.string.msg_ip_uid_insert),
        MSG_IP_UID_PROCESSING (R.string.msg_ip_uid_processing),
        MSG_IP_UID_SELF_BAD (R.string.msg_ip_uid_self_bad),
        MSG_IP_UID_SELF_GOOD (R.string.msg_ip_uid_self_good),
        MSG_IP_UID_SELF_IGNORING_OLD (R.string.msg_ip_uid_self_ignoring_old),
        MSG_IP_UID_SELF_NEWER (R.string.msg_ip_uid_self_newer),
        MSG_IS_BAD_TYPE_PUBLIC (R.string.msg_is_bad_type_public),
        MSG_IS_IMPORTING (R.string.msg_is_importing),
        MSG_IS_IMPORTING_SUBKEYS (R.string.msg_is_importing_subkeys),
        MSG_IS_IO_EXCPTION (R.string.msg_is_io_excption),
        MSG_IS_SUBKEY_NONEXISTENT (R.string.msg_is_subkey_nonexistent),
        MSG_IS_SUBKEY_OK (R.string.msg_is_subkey_ok),
        MSG_IS_SUBKEY_STRIPPED (R.string.msg_is_subkey_stripped),
        MSG_IS_SUCCESS (R.string.msg_is_success),
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
        /** If any ERROR log entry is included in the result, the overall operation should have failed. */
        ERROR,
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

}

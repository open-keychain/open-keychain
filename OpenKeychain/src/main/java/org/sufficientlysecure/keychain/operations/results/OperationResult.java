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

package org.sufficientlysecure.keychain.operations.results;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.LogDisplayActivity;
import org.sufficientlysecure.keychain.ui.LogDisplayFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Showable;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.ParcelableCache;
import timber.log.Timber;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Represent the result of an operation.
 *
 * This class holds a result and the log of an operation. It can be subclassed
 * to include typed additional information specific to the operation. To keep
 * the class structure (somewhat) simple, this class contains an exhaustive
 * list (ie, enum) of all possible log types, which should in all cases be tied
 * to string resource ids.
 */
public abstract class OperationResult implements Parcelable {

    final static String INDENTATION_WHITESPACE = "                                                                ";

    public static final String EXTRA_RESULT = "operation_result";

    /**
     * Instead of parceling the logs, they are cached to overcome the 1 MB boundary of
     * Android's Binder. See ParcelableCache
     */
    private static ParcelableCache<OperationLog> logCache;
    static {
        logCache = new ParcelableCache<>();
    }

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
    protected OperationLog mLog;

    public OperationResult(int result, OperationLog log) {
        mResult = result;
        mLog = log;
    }

    public OperationResult(Parcel source) {
        mResult = source.readInt();
        // get log out of cache based on UUID from source
        mLog = logCache.readFromParcelAndGetFromCache(source);
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
        SubLogEntryParcel singleSubLog = mLog.getSubResultIfSingle();
        if (singleSubLog != null) {
            return singleSubLog.getSubResult().getLog();
        }
        // Otherwse, return our regular log
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
            Timber.v("log: " + this);
        }

        /** Clones this LogEntryParcel, adding extra indent. Note that the parameter array is NOT cloned! */
        public LogEntryParcel (LogEntryParcel original, int extraIndent) {
            mType = original.mType;
            mParameters = original.mParameters;
            mIndent = original.mIndent +extraIndent;
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
            dest.writeInt(0);
            dest.writeInt(mType.ordinal());
            dest.writeSerializable(mParameters);
            dest.writeInt(mIndent);
        }

        public static final Creator<LogEntryParcel> CREATOR = new Creator<LogEntryParcel>() {
            public LogEntryParcel createFromParcel(final Parcel source) {
                // Actually create LogEntryParcel or SubLogEntryParcel depending on type indicator
                if (source.readInt() == 0) {
                    return new LogEntryParcel(source);
                } else {
                    return new SubLogEntryParcel(source);
                }
            }

            public LogEntryParcel[] newArray(final int size) {
                return new LogEntryParcel[size];
            }
        };

        @Override
        public String toString() {
            return getClass().getSimpleName() + "{" +
                    "mLevel=" + mType.mLevel +
                    ", mType=" + mType +
                    ", mParameters=" + Arrays.toString(mParameters) +
                    ", mIndent=" + mIndent +
                    '}';
        }

        StringBuilder getPrintableLogEntry(Resources resources, int indent) {

            StringBuilder result = new StringBuilder();
            int padding = mIndent +indent;
            if (padding > INDENTATION_WHITESPACE.length()) {
                padding = INDENTATION_WHITESPACE.length();
            }
            result.append(INDENTATION_WHITESPACE, 0, padding);
            result.append(LOG_LEVEL_NAME[mType.mLevel.ordinal()]).append(' ');

            // special case: first parameter may be a quantity
            if (mParameters != null && mParameters.length > 0 && mParameters[0] instanceof Integer) {
                result.append(resources.getQuantityString(mType.getMsgId(), (Integer) mParameters[0], mParameters));
            } else {
                result.append(resources.getString(mType.getMsgId(), mParameters));
            }

            return result;
        }

    }

    public static class SubLogEntryParcel extends LogEntryParcel {

        @NonNull OperationResult mSubResult;

        public SubLogEntryParcel(@NonNull OperationResult subResult, LogType type, int indent, Object... parameters) {
            super(type, indent, parameters);
            mSubResult = subResult;

            Timber.v("log: " + this);
        }

        public SubLogEntryParcel(Parcel source) {
            super(source);
            mSubResult = source.readParcelable(SubLogEntryParcel.class.getClassLoader());
        }

        public OperationResult getSubResult() {
            return mSubResult;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(1);
            dest.writeInt(mType.ordinal());
            dest.writeSerializable(mParameters);
            dest.writeInt(mIndent);
            dest.writeParcelable(mSubResult, 0);
        }

        public static final Parcelable.Creator<SubLogEntryParcel> CREATOR = new Parcelable.Creator<SubLogEntryParcel>() {
            @Override
            public SubLogEntryParcel createFromParcel(Parcel in) {
                return new SubLogEntryParcel(in);
            }

            @Override
            public SubLogEntryParcel[] newArray(int size) {
                return new SubLogEntryParcel[size];
            }
        };

        @Override
        StringBuilder getPrintableLogEntry(Resources resources, int indent) {

            LogEntryParcel subEntry = mSubResult.getLog().getLast();
            if (subEntry != null) {
                return subEntry.getPrintableLogEntry(resources, mIndent +indent);
            } else {
                return super.getPrintableLogEntry(resources, indent);
            }
        }

    }

    public Showable createNotify(final Activity activity) {

        // Take the last message as string
        String logText;

        LogEntryParcel entryParcel = mLog.getLast();
        if (entryParcel == null) {
            Timber.e("Tried to show empty log!");
            return Notify.create(activity, R.string.error_empty_log, Style.ERROR);
        }
        // special case: first parameter may be a quantity
        if (entryParcel.mParameters != null && entryParcel.mParameters.length > 0
                && entryParcel.mParameters[0] instanceof Integer) {
            logText = activity.getResources().getQuantityString(entryParcel.mType.getMsgId(),
                    (Integer) entryParcel.mParameters[0],
                    entryParcel.mParameters);
        } else {
            logText = activity.getString(entryParcel.mType.getMsgId(),
                    entryParcel.mParameters);
        }

        Style style;

        // Not an overall failure
        if (cancelled()) {
            style = Style.ERROR;
        } else if (success()) {
            if (getLog().containsWarnings()) {
                style = Style.WARN;
            } else {
                style = Style.OK;
            }
        } else {
            style = Style.ERROR;
        }

        if (getLog() == null || getLog().isEmpty()) {
            return Notify.create(activity, logText, Notify.LENGTH_LONG, style);
        }

        return Notify.create(activity, logText, Notify.LENGTH_LONG, style,
                new ActionListener() {
                    @Override
                    public void onAction() {
                        Intent intent = new Intent(
                                activity, LogDisplayActivity.class);
                        intent.putExtra(LogDisplayFragment.EXTRA_RESULT, OperationResult.this);
                        activity.startActivity(intent);
                    }
                }, R.string.snackbar_details);

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
    public enum LogType {

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
        MSG_IP_FINGERPRINT_ERROR (LogLevel.ERROR, R.string.msg_ip_fingerprint_error),
        MSG_IP_FINGERPRINT_OK (LogLevel.INFO, R.string.msg_ip_fingerprint_ok),
        MSG_IP_INSERT_KEYRING (LogLevel.DEBUG, R.string.msg_ip_insert_keyring),
        MSG_IP_INSERT_SUBKEYS (LogLevel.DEBUG, R.string.msg_ip_insert_keys),
        MSG_IP_PREPARE (LogLevel.DEBUG, R.string.msg_ip_prepare),
        MSG_IP_REINSERT_SECRET (LogLevel.DEBUG, R.string.msg_ip_reinsert_secret),
        MSG_IP_MASTER (LogLevel.DEBUG, R.string.msg_ip_master),
        MSG_IP_MASTER_EXPIRED (LogLevel.DEBUG, R.string.msg_ip_master_expired),
        MSG_IP_MASTER_EXPIRES (LogLevel.DEBUG, R.string.msg_ip_master_expires),
        MSG_IP_MASTER_FLAGS_UNSPECIFIED (LogLevel.DEBUG, R.string.msg_ip_master_flags_unspecified),
        MSG_IP_MASTER_FLAGS_CESA (LogLevel.DEBUG, R.string.msg_ip_master_flags_cesa),
        MSG_IP_MASTER_FLAGS_CESX (LogLevel.DEBUG, R.string.msg_ip_master_flags_cesx),
        MSG_IP_MASTER_FLAGS_CEXA (LogLevel.DEBUG, R.string.msg_ip_master_flags_cexa),
        MSG_IP_MASTER_FLAGS_CEXX (LogLevel.DEBUG, R.string.msg_ip_master_flags_cexx),
        MSG_IP_MASTER_FLAGS_CXSA (LogLevel.DEBUG, R.string.msg_ip_master_flags_cxsa),
        MSG_IP_MASTER_FLAGS_CXSX (LogLevel.DEBUG, R.string.msg_ip_master_flags_cxsx),
        MSG_IP_MASTER_FLAGS_CXXA (LogLevel.DEBUG, R.string.msg_ip_master_flags_cxxa),
        MSG_IP_MASTER_FLAGS_CXXX (LogLevel.DEBUG, R.string.msg_ip_master_flags_cxxx),
        MSG_IP_MASTER_FLAGS_XESA (LogLevel.DEBUG, R.string.msg_ip_master_flags_xesa),
        MSG_IP_MASTER_FLAGS_XESX (LogLevel.DEBUG, R.string.msg_ip_master_flags_xesx),
        MSG_IP_MASTER_FLAGS_XEXA (LogLevel.DEBUG, R.string.msg_ip_master_flags_xexa),
        MSG_IP_MASTER_FLAGS_XEXX (LogLevel.DEBUG, R.string.msg_ip_master_flags_xexx),
        MSG_IP_MASTER_FLAGS_XXSA (LogLevel.DEBUG, R.string.msg_ip_master_flags_xxsa),
        MSG_IP_MASTER_FLAGS_XXSX (LogLevel.DEBUG, R.string.msg_ip_master_flags_xxsx),
        MSG_IP_MASTER_FLAGS_XXXA (LogLevel.DEBUG, R.string.msg_ip_master_flags_xxxa),
        MSG_IP_MASTER_FLAGS_XXXX (LogLevel.DEBUG, R.string.msg_ip_master_flags_xxxx),
        MSG_IP_MERGE_PUBLIC (LogLevel.DEBUG, R.string.msg_ip_merge_public),
        MSG_IP_MERGE_SECRET (LogLevel.DEBUG, R.string.msg_ip_merge_secret),
        MSG_IP_SUBKEY (LogLevel.DEBUG, R.string.msg_ip_subkey),
        MSG_IP_SUBKEY_EXPIRED (LogLevel.DEBUG, R.string.msg_ip_subkey_expired),
        MSG_IP_SUBKEY_EXPIRES (LogLevel.DEBUG, R.string.msg_ip_subkey_expires),
        MSG_IP_SUBKEY_FLAGS_UNSPECIFIED (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cesa),
        MSG_IP_SUBKEY_FLAGS_CESA (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cesa),
        MSG_IP_SUBKEY_FLAGS_CESX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cesx),
        MSG_IP_SUBKEY_FLAGS_CEXA (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cexa),
        MSG_IP_SUBKEY_FLAGS_CEXX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cexx),
        MSG_IP_SUBKEY_FLAGS_CXSA (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cxsa),
        MSG_IP_SUBKEY_FLAGS_CXSX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cxsx),
        MSG_IP_SUBKEY_FLAGS_CXXA (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cxxa),
        MSG_IP_SUBKEY_FLAGS_CXXX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_cxxx),
        MSG_IP_SUBKEY_FLAGS_XESA (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xesa),
        MSG_IP_SUBKEY_FLAGS_XESX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xesx),
        MSG_IP_SUBKEY_FLAGS_XEXA (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xexa),
        MSG_IP_SUBKEY_FLAGS_XEXX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xexx),
        MSG_IP_SUBKEY_FLAGS_XXSA (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xxsa),
        MSG_IP_SUBKEY_FLAGS_XXSX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xxsx),
        MSG_IP_SUBKEY_FLAGS_XXXA (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xxxa),
        MSG_IP_SUBKEY_FLAGS_XXXX (LogLevel.DEBUG, R.string.msg_ip_subkey_flags_xxxx),
        MSG_IP_SUCCESS (LogLevel.OK, R.string.msg_ip_success),
        MSG_IP_SUCCESS_IDENTICAL (LogLevel.OK, R.string.msg_ip_success_identical),
        MSG_IP_UID_CERT_BAD (LogLevel.WARN, R.string.msg_ip_uid_cert_bad),
        MSG_IP_UID_CERT_ERROR (LogLevel.WARN, R.string.msg_ip_uid_cert_error),
        MSG_IP_UID_CERT_OLD (LogLevel.DEBUG, R.string.msg_ip_uid_cert_old),
        MSG_IP_UID_CERT_NONREVOKE (LogLevel.DEBUG, R.string.msg_ip_uid_cert_nonrevoke),
        MSG_IP_UID_CERT_NEW (LogLevel.DEBUG, R.string.msg_ip_uid_cert_new),
        MSG_IP_UID_CERT_GOOD (LogLevel.DEBUG, R.string.msg_ip_uid_cert_good),
        MSG_IP_UID_CERT_GOOD_REVOKE (LogLevel.DEBUG, R.string.msg_ip_uid_cert_good_revoke),
        MSG_IP_UID_CERTS_UNKNOWN (LogLevel.DEBUG, R.plurals.msg_ip_uid_certs_unknown),
        MSG_IP_UID_CLASSIFYING_ZERO (LogLevel.DEBUG, R.string.msg_ip_uid_classifying_zero),
        MSG_IP_UID_CLASSIFYING (LogLevel.DEBUG, R.plurals.msg_ip_uid_classifying),
        MSG_IP_UID_REORDER(LogLevel.DEBUG, R.string.msg_ip_uid_reorder),
        MSG_IP_UID_PROCESSING (LogLevel.DEBUG, R.string.msg_ip_uid_processing),
        MSG_IP_UID_REVOKED (LogLevel.DEBUG, R.string.msg_ip_uid_revoked),
        MSG_IP_UAT_CLASSIFYING (LogLevel.DEBUG, R.string.msg_ip_uat_classifying),
        MSG_IP_UAT_PROCESSING_IMAGE (LogLevel.DEBUG, R.string.msg_ip_uat_processing_image),
        MSG_IP_UAT_PROCESSING_UNKNOWN (LogLevel.DEBUG, R.string.msg_ip_uat_processing_unknown),
        MSG_IP_UAT_REVOKED (LogLevel.DEBUG, R.string.msg_ip_uat_revoked),
        MSG_IP_UAT_CERT_BAD (LogLevel.WARN, R.string.msg_ip_uat_cert_bad),
        MSG_IP_UAT_CERT_OLD (LogLevel.DEBUG, R.string.msg_ip_uat_cert_old),
        MSG_IP_UAT_CERT_NONREVOKE (LogLevel.DEBUG, R.string.msg_ip_uat_cert_nonrevoke),
        MSG_IP_UAT_CERT_NEW (LogLevel.DEBUG, R.string.msg_ip_uat_cert_new),
        MSG_IP_UAT_CERT_ERROR (LogLevel.WARN, R.string.msg_ip_uat_cert_error),
        MSG_IP_UAT_CERTS_UNKNOWN (LogLevel.DEBUG, R.plurals.msg_ip_uat_certs_unknown),
        MSG_IP_UAT_CERT_GOOD_REVOKE (LogLevel.DEBUG, R.string.msg_ip_uat_cert_good_revoke),
        MSG_IP_UAT_CERT_GOOD (LogLevel.DEBUG, R.string.msg_ip_uat_cert_good),

        // import secret
        MSG_IS(LogLevel.START, R.string.msg_is),
        MSG_IS_BAD_TYPE_PUBLIC (LogLevel.WARN, R.string.msg_is_bad_type_public),
        MSG_IS_ERROR_IO_EXC(LogLevel.DEBUG, R.string.msg_is_error_io_exc),
        MSG_IS_MERGE_PUBLIC (LogLevel.DEBUG, R.string.msg_is_merge_public),
        MSG_IS_MERGE_SECRET (LogLevel.DEBUG, R.string.msg_is_merge_secret),
        MSG_IS_MERGE_SPECIAL (LogLevel.DEBUG, R.string.msg_is_merge_special),
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
        MSG_KC_PUBLIC (LogLevel.START, R.string.msg_kc_public),
        MSG_KC_SECRET (LogLevel.START, R.string.msg_kc_secret),
        MSG_KC_ERROR_V3 (LogLevel.ERROR, R.string.msg_kc_error_v3),
        MSG_KC_ERROR_NO_UID (LogLevel.ERROR, R.string.msg_kc_error_no_uid),
        MSG_KC_ERROR_MASTER_ALGO (LogLevel.ERROR, R.string.msg_kc_error_master_algo),
        MSG_KC_ERROR_DUP_KEY (LogLevel.ERROR, R.string.msg_kc_error_dup_key),
        MSG_KC_MASTER (LogLevel.DEBUG, R.string.msg_kc_master),
        MSG_KC_MASTER_BAD_TYPE(LogLevel.WARN, R.string.msg_kc_master_bad_type),
        MSG_KC_MASTER_BAD_LOCAL(LogLevel.WARN, R.string.msg_kc_master_bad_local),
        MSG_KC_MASTER_BAD_ERR(LogLevel.WARN, R.string.msg_kc_master_bad_err),
        MSG_KC_MASTER_BAD_TIME(LogLevel.WARN, R.string.msg_kc_master_bad_time),
        MSG_KC_MASTER_BAD_TYPE_UID(LogLevel.WARN, R.string.msg_kc_master_bad_type_uid),
        MSG_KC_MASTER_BAD(LogLevel.WARN, R.string.msg_kc_master_bad),
        MSG_KC_MASTER_LOCAL(LogLevel.WARN, R.string.msg_kc_master_local),
        MSG_KC_REVOKE_DUP (LogLevel.DEBUG, R.string.msg_kc_revoke_dup),
        MSG_KC_NOTATION_DUP (LogLevel.DEBUG, R.string.msg_kc_notation_dup),
        MSG_KC_NOTATION_EMPTY (LogLevel.DEBUG, R.string.msg_kc_notation_empty),
        MSG_KC_SUB (LogLevel.DEBUG, R.string.msg_kc_sub),
        MSG_KC_SUB_BAD(LogLevel.WARN, R.string.msg_kc_sub_bad),
        MSG_KC_SUB_BAD_ERR(LogLevel.WARN, R.string.msg_kc_sub_bad_err),
        MSG_KC_SUB_BAD_LOCAL(LogLevel.WARN, R.string.msg_kc_sub_bad_local),
        MSG_KC_SUB_BAD_KEYID(LogLevel.WARN, R.string.msg_kc_sub_bad_keyid),
        MSG_KC_SUB_BAD_TIME_EARLY(LogLevel.WARN, R.string.msg_kc_sub_bad_time_early),
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
        MSG_KC_SUB_ALGO_BAD_ENCRYPT (LogLevel.WARN, R.string.msg_kc_sub_algo_bad_encrpyt),
        MSG_KC_SUB_ALGO_BAD_SIGN (LogLevel.WARN, R.string.msg_kc_sub_algo_bad_sign),
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
        MSG_KC_UID_TOO_MANY (LogLevel.DEBUG, R.string.msg_kc_uid_too_many),
        MSG_KC_UID_FOREIGN (LogLevel.DEBUG, R.string.msg_kc_uid_foreign),
        MSG_KC_UID_NO_CERT (LogLevel.DEBUG, R.string.msg_kc_uid_no_cert),
        MSG_KC_UID_REVOKE_DUP (LogLevel.DEBUG, R.string.msg_kc_uid_revoke_dup),
        MSG_KC_UID_REVOKE_OLD (LogLevel.DEBUG, R.string.msg_kc_uid_revoke_old),
        MSG_KC_UID_REMOVE (LogLevel.DEBUG, R.string.msg_kc_uid_remove),
        MSG_KC_UID_WARN_ENCODING (LogLevel.WARN, R.string.msg_kc_uid_warn_encoding),
        MSG_KC_UAT_JPEG (LogLevel.DEBUG, R.string.msg_kc_uat_jpeg),
        MSG_KC_UAT_UNKNOWN (LogLevel.DEBUG, R.string.msg_kc_uat_unknown),
        MSG_KC_UAT_BAD_ERR (LogLevel.WARN, R.string.msg_kc_uat_bad_err),
        MSG_KC_UAT_BAD_LOCAL (LogLevel.WARN, R.string.msg_kc_uat_bad_local),
        MSG_KC_UAT_BAD_TIME (LogLevel.WARN, R.string.msg_kc_uat_bad_time),
        MSG_KC_UAT_BAD_TYPE (LogLevel.WARN, R.string.msg_kc_uat_bad_type),
        MSG_KC_UAT_BAD (LogLevel.WARN, R.string.msg_kc_uat_bad),
        MSG_KC_UAT_CERT_DUP (LogLevel.DEBUG, R.string.msg_kc_uat_cert_dup),
        MSG_KC_UAT_DUP (LogLevel.DEBUG, R.string.msg_kc_uat_dup),
        MSG_KC_UAT_FOREIGN (LogLevel.DEBUG, R.string.msg_kc_uat_foreign),
        MSG_KC_UAT_NO_CERT (LogLevel.DEBUG, R.string.msg_kc_uat_no_cert),
        MSG_KC_UAT_REVOKE_DUP (LogLevel.DEBUG, R.string.msg_kc_uat_revoke_dup),
        MSG_KC_UAT_REVOKE_OLD (LogLevel.DEBUG, R.string.msg_kc_uat_revoke_old),
        MSG_KC_UAT_REMOVE (LogLevel.DEBUG, R.string.msg_kc_uat_remove),
        MSG_KC_UAT_WARN_ENCODING (LogLevel.WARN, R.string.msg_kc_uat_warn_encoding),


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
        MSG_CR_ERROR_KEYSIZE_2048(LogLevel.ERROR, R.string.msg_cr_error_keysize_2048),
        MSG_CR_ERROR_NO_KEYSIZE (LogLevel.ERROR, R.string.msg_cr_error_no_keysize),
        MSG_CR_ERROR_NO_CURVE (LogLevel.ERROR, R.string.msg_cr_error_no_curve),
        MSG_CR_ERROR_UNKNOWN_ALGO (LogLevel.ERROR, R.string.msg_cr_error_unknown_algo),
        MSG_CR_ERROR_INTERNAL_PGP (LogLevel.ERROR, R.string.msg_cr_error_internal_pgp),
        MSG_CR_ERROR_FLAGS_DSA (LogLevel.ERROR, R.string.msg_cr_error_flags_dsa),
        MSG_CR_ERROR_FLAGS_ELGAMAL (LogLevel.ERROR, R.string.msg_cr_error_flags_elgamal),
        MSG_CR_ERROR_FLAGS_ECDSA (LogLevel.ERROR, R.string.msg_cr_error_flags_ecdsa),
        MSG_CR_ERROR_FLAGS_EDDSA (LogLevel.ERROR, R.string.msg_cr_error_flags_eddsa),
        MSG_CR_ERROR_FLAGS_ECDH (LogLevel.ERROR, R.string.msg_cr_error_flags_ecdh),

        // secret key modify
        MSG_MF (LogLevel.START, R.string.msg_mr),
        MSG_MF_DIVERT (LogLevel.DEBUG, R.string.msg_mf_divert),
        MSG_MF_ERROR_ALL_KEYS_STRIPPED (LogLevel.ERROR, R.string.msg_mf_error_all_keys_stripped),
        MSG_MF_ERROR_DIVERT_NEWSUB (LogLevel.ERROR, R.string.msg_mf_error_divert_newsub),
        MSG_MF_ERROR_DIVERT_SERIAL (LogLevel.ERROR, R.string.msg_mf_error_divert_serial),
        MSG_MF_ERROR_ENCODE (LogLevel.ERROR, R.string.msg_mf_error_encode),
        MSG_MF_ERROR_FINGERPRINT (LogLevel.ERROR, R.string.msg_mf_error_fingerprint),
        MSG_MF_ERROR_KEYID (LogLevel.ERROR, R.string.msg_mf_error_keyid),
        MSG_MF_ERROR_INTEGRITY (LogLevel.ERROR, R.string.msg_mf_error_integrity),
        MSG_MF_ERROR_MASTER_NONE(LogLevel.ERROR, R.string.msg_mf_error_master_none),
        MSG_MF_ERROR_NO_CERTIFY (LogLevel.ERROR, R.string.msg_cr_error_no_certify),
        MSG_MF_ERROR_NOEXIST_PRIMARY (LogLevel.ERROR, R.string.msg_mf_error_noexist_primary),
        MSG_MF_ERROR_NOEXIST_REVOKE (LogLevel.ERROR, R.string.msg_mf_error_noexist_revoke),
        MSG_MF_ERROR_NOOP (LogLevel.ERROR, R.string.msg_mf_error_noop),
        MSG_MF_ERROR_NULL_EXPIRY (LogLevel.ERROR, R.string.msg_mf_error_null_expiry),
        MSG_MF_ERROR_PASSPHRASE_MASTER(LogLevel.ERROR, R.string.msg_mf_error_passphrase_master),
        MSG_MF_ERROR_PASSPHRASES_UNCHANGED(LogLevel.ERROR, R.string.msg_mf_error_passphrases_unchanged),
        MSG_MF_ERROR_PAST_EXPIRY(LogLevel.ERROR, R.string.msg_mf_error_past_expiry),
        MSG_MF_ERROR_PGP (LogLevel.ERROR, R.string.msg_mf_error_pgp),
        MSG_MF_ERROR_RESTRICTED(LogLevel.ERROR, R.string.msg_mf_error_restricted),
        MSG_MF_ERROR_REVOKED_PRIMARY (LogLevel.ERROR, R.string.msg_mf_error_revoked_primary),
        MSG_MF_ERROR_SIG (LogLevel.ERROR, R.string.msg_mf_error_sig),
        MSG_MF_ERROR_SUB_STRIPPED(LogLevel.ERROR, R.string.msg_mf_error_sub_stripped),
        MSG_MF_ERROR_SUBKEY_MISSING(LogLevel.ERROR, R.string.msg_mf_error_subkey_missing),
        MSG_MF_ERROR_CONFLICTING_NFC_COMMANDS(LogLevel.ERROR, R.string.msg_mf_error_conflicting_nfc_commands),
        MSG_MF_ERROR_DUPLICATE_KEYTOCARD_FOR_SLOT(LogLevel.ERROR, R.string.msg_mf_error_duplicate_keytocard_for_slot),
        MSG_MF_ERROR_INVALID_FLAGS_FOR_KEYTOCARD(LogLevel.ERROR, R.string.msg_mf_error_invalid_flags_for_keytocard),
        MSG_MF_ERROR_BAD_SECURITY_TOKEN_ALGO(LogLevel.ERROR, R.string.edit_key_error_bad_security_token_algo),
        MSG_MF_ERROR_BAD_SECURITY_TOKEN_RSA_KEY_SIZE(LogLevel.ERROR, R.string.edit_key_error_bad_security_token_size),
        MSG_MF_ERROR_BAD_SECURITY_TOKEN_CURVE(LogLevel.ERROR, R.string.edit_key_error_bad_security_token_curve),
        MSG_MF_ERROR_BAD_SECURITY_TOKEN_STRIPPED(LogLevel.ERROR, R.string.edit_key_error_bad_security_token_stripped),
        MSG_MF_MASTER (LogLevel.DEBUG, R.string.msg_mf_master),
        MSG_MF_PASSPHRASE (LogLevel.INFO, R.string.msg_mf_passphrase),
        MSG_MF_PIN (LogLevel.INFO, R.string.msg_mf_pin),
        MSG_MF_ADMIN_PIN (LogLevel.INFO, R.string.msg_mf_admin_pin),
        MSG_MF_PASSPHRASE_KEY (LogLevel.DEBUG, R.string.msg_mf_passphrase_key),
        MSG_MF_PASSPHRASE_EMPTY_RETRY (LogLevel.DEBUG, R.string.msg_mf_passphrase_empty_retry),
        MSG_MF_PASSPHRASE_FAIL (LogLevel.WARN, R.string.msg_mf_passphrase_fail),
        MSG_MF_PRIMARY_REPLACE_OLD (LogLevel.DEBUG, R.string.msg_mf_primary_replace_old),
        MSG_MF_PRIMARY_NEW (LogLevel.DEBUG, R.string.msg_mf_primary_new),
        MSG_MF_RESTRICTED_MODE (LogLevel.INFO, R.string.msg_mf_restricted_mode),
        MSG_MF_REQUIRE_DIVERT (LogLevel.OK, R.string.msg_mf_require_divert),
        MSG_MF_REQUIRE_PASSPHRASE (LogLevel.OK, R.string.msg_mf_require_passphrase),
        MSG_MF_SUBKEY_CHANGE (LogLevel.INFO, R.string.msg_mf_subkey_change),
        MSG_MF_SUBKEY_NEW_ID (LogLevel.DEBUG, R.string.msg_mf_subkey_new_id),
        MSG_MF_SUBKEY_NEW (LogLevel.INFO, R.string.msg_mf_subkey_new),
        MSG_MF_SUBKEY_REVOKE (LogLevel.INFO, R.string.msg_mf_subkey_revoke),
        MSG_MF_SUBKEY_STRIP (LogLevel.INFO, R.string.msg_mf_subkey_strip),
        MSG_MF_KEYTOCARD_START (LogLevel.INFO, R.string.msg_mf_keytocard_start),
        MSG_MF_KEYTOCARD_FINISH (LogLevel.OK, R.string.msg_mf_keytocard_finish),
        MSG_MF_SUCCESS (LogLevel.OK, R.string.msg_mf_success),
        MSG_MF_UID_ADD (LogLevel.INFO, R.string.msg_mf_uid_add),
        MSG_MF_UID_PRIMARY (LogLevel.INFO, R.string.msg_mf_uid_primary),
        MSG_MF_UID_REVOKE (LogLevel.INFO, R.string.msg_mf_uid_revoke),
        MSG_MF_UID_ERROR_EMPTY (LogLevel.ERROR, R.string.msg_mf_uid_error_empty),
        MSG_MF_UAT_ERROR_EMPTY (LogLevel.ERROR, R.string.msg_mf_uat_error_empty),
        MSG_MF_UAT_ADD_IMAGE (LogLevel.INFO, R.string.msg_mf_uat_add_image),
        MSG_MF_UAT_ADD_UNKNOWN (LogLevel.INFO, R.string.msg_mf_uat_add_unknown),
        MSG_MF_UNLOCK_ERROR (LogLevel.ERROR, R.string.msg_mf_unlock_error),
        MSG_MF_UNLOCK (LogLevel.DEBUG, R.string.msg_mf_unlock),

        // edit key (higher level operation than modify)
        MSG_ED (LogLevel.START, R.string.msg_ed),
        MSG_ED_CACHING_NEW (LogLevel.DEBUG, R.string.msg_ed_caching_new),
        MSG_ED_ERROR_NO_PARCEL (LogLevel.ERROR, R.string.msg_ed_error_no_parcel),
        MSG_ED_ERROR_KEY_NOT_FOUND (LogLevel.ERROR, R.string.msg_ed_error_key_not_found),
        MSG_ED_ERROR_EXTRACTING_PUBLIC_UPLOAD (LogLevel.ERROR,
                R.string.msg_ed_error_extract_public_upload),
        MSG_ED_FETCHING (LogLevel.DEBUG, R.string.msg_ed_fetching),
        MSG_ED_SUCCESS (LogLevel.OK, R.string.msg_ed_success),

        // promote key
        MSG_PR (LogLevel.START, R.string.msg_pr),
        MSG_PR_ALL (LogLevel.DEBUG, R.string.msg_pr_all),
        MSG_PR_ERROR_KEY_NOT_FOUND (LogLevel.ERROR, R.string.msg_pr_error_key_not_found),
        MSG_PR_FETCHING (LogLevel.DEBUG, R.string.msg_pr_fetching),
        MSG_PR_SUBKEY_MATCH (LogLevel.DEBUG, R.string.msg_pr_subkey_match),
        MSG_PR_SUBKEY_NOMATCH (LogLevel.WARN, R.string.msg_pr_subkey_nomatch),
        MSG_PR_SUCCESS (LogLevel.OK, R.string.msg_pr_success),

        // messages used in UI code
        MSG_EK_ERROR_DUMMY (LogLevel.ERROR, R.string.msg_ek_error_dummy),
        MSG_EK_ERROR_NOT_FOUND (LogLevel.ERROR, R.string.msg_ek_error_not_found),

        // decryptverify
        MSG_DC_ASKIP_BAD_FLAGS (LogLevel.DEBUG, R.string.msg_dc_askip_bad_flags),
        MSG_DC_ASKIP_UNAVAILABLE (LogLevel.DEBUG, R.string.msg_dc_askip_unavailable),
        MSG_DC_ASKIP_NO_KEY (LogLevel.DEBUG, R.string.msg_dc_askip_no_key),
        MSG_DC_ASKIP_NOT_ALLOWED (LogLevel.DEBUG, R.string.msg_dc_askip_not_allowed),
        MSG_DC_ASYM (LogLevel.DEBUG, R.string.msg_dc_asym),
        MSG_DC_CHARSET (LogLevel.DEBUG, R.string.msg_dc_charset),
        MSG_DC_BACKUP_VERSION (LogLevel.DEBUG, R.string.msg_dc_backup_version),
        MSG_DC_PASSPHRASE_FORMAT (LogLevel.DEBUG, R.string.msg_dc_passphrase_format),
        MSG_DC_PASSPHRASE_BEGIN (LogLevel.DEBUG, R.string.msg_dc_passphrase_begin),
        MSG_DC_CLEAR_DATA (LogLevel.DEBUG, R.string.msg_dc_clear_data),
        MSG_DC_CLEAR_DECOMPRESS (LogLevel.DEBUG, R.string.msg_dc_clear_decompress),
        MSG_DC_CLEAR_INTENDED_RECIPIENT (LogLevel.DEBUG, R.string.msg_dc_clear_intended_recipient),
        MSG_DC_CLEAR_META_FILE (LogLevel.DEBUG, R.string.msg_dc_clear_meta_file),
        MSG_DC_CLEAR_META_MIME (LogLevel.DEBUG, R.string.msg_dc_clear_meta_mime),
        MSG_DC_CLEAR_META_SIZE (LogLevel.DEBUG, R.string.msg_dc_clear_meta_size),
        MSG_DC_CLEAR_META_SIZE_UNKNOWN (LogLevel.DEBUG, R.string.msg_dc_clear_meta_size_unknown),
        MSG_DC_CLEAR_META_TIME (LogLevel.DEBUG, R.string.msg_dc_clear_meta_time),
        MSG_DC_CLEAR (LogLevel.DEBUG, R.string.msg_dc_clear),
        MSG_DC_CLEAR_SIGNATURE_BAD (LogLevel.WARN, R.string.msg_dc_clear_signature_bad),
        MSG_DC_CLEAR_SIGNATURE_CHECK (LogLevel.DEBUG, R.string.msg_dc_clear_signature_check),
        MSG_DC_CLEAR_SIGNATURE_OK (LogLevel.OK, R.string.msg_dc_clear_signature_ok),
        MSG_DC_CLEAR_SIGNATURE (LogLevel.DEBUG, R.string.msg_dc_clear_signature),
        MSG_DC_ERROR_BAD_PASSPHRASE (LogLevel.ERROR, R.string.msg_dc_error_bad_passphrase),
        MSG_DC_ERROR_SYM_PASSPHRASE (LogLevel.ERROR, R.string.msg_dc_error_sym_passphrase),
        MSG_DC_ERROR_CORRUPT_DATA (LogLevel.ERROR, R.string.msg_dc_error_corrupt_data),
        MSG_DC_ERROR_EXTRACT_KEY (LogLevel.ERROR, R.string.msg_dc_error_extract_key),
        MSG_DC_ERROR_INTEGRITY_CHECK (LogLevel.ERROR, R.string.msg_dc_error_integrity_check),
        MSG_DC_ERROR_INVALID_DATA (LogLevel.ERROR, R.string.msg_dc_error_invalid_data),
        MSG_DC_ERROR_IO (LogLevel.ERROR, R.string.msg_dc_error_io),
        MSG_DC_ERROR_INPUT (LogLevel.ERROR, R.string.msg_dc_error_input),
        MSG_DC_ERROR_INPUT_DENIED (LogLevel.ERROR, R.string.msg_dc_error_input_denied),
        MSG_DC_ERROR_NO_DATA (LogLevel.ERROR, R.string.msg_dc_error_no_data),
        MSG_DC_ERROR_NO_KEY (LogLevel.ERROR, R.string.msg_dc_error_no_key),
        MSG_DC_ERROR_NO_SIGNATURE (LogLevel.ERROR, R.string.msg_dc_error_no_signature),
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
        MSG_DC_INSECURE_ENCRYPTION_KEY (LogLevel.WARN, R.string.msg_dc_insecure_encryption_key),
        MSG_DC_INSECURE_SYMMETRIC_ENCRYPTION_ALGO(LogLevel.WARN, R.string.msg_dc_insecure_symmetric_encryption_algo),
        MSG_DC_INSECURE_HASH_ALGO(LogLevel.ERROR, R.string.msg_dc_insecure_hash_algo),
        MSG_DC_INSECURE_MDC_MISSING(LogLevel.ERROR, R.string.msg_dc_insecure_mdc_missing),
        MSG_DC_INSECURE_KEY(LogLevel.ERROR, R.string.msg_dc_insecure_key),

        // verify signed literal data
        MSG_VL (LogLevel.INFO, R.string.msg_vl),
        MSG_VL_ERROR_MISSING_SIGLIST (LogLevel.ERROR, R.string.msg_vl_error_no_siglist),
        MSG_VL_ERROR_MISSING_LITERAL (LogLevel.ERROR, R.string.msg_vl_error_missing_literal),
        MSG_VL_ERROR_MISSING_KEY (LogLevel.ERROR, R.string.msg_vl_error_wrong_key),
        MSG_VL_ERROR_NO_SIGNATURE (LogLevel.ERROR, R.string.msg_vl_error_no_signature),
        MSG_VL_CLEAR_SIGNATURE_CHECK (LogLevel.DEBUG, R.string.msg_vl_clear_signature_check),
        MSG_VL_ERROR_INTEGRITY_CHECK (LogLevel.ERROR, R.string.msg_vl_error_integrity_check),
        MSG_VL_OK (LogLevel.OK, R.string.msg_vl_ok),

        // signencrypt
        MSG_SE (LogLevel.START, R.string.msg_se),
        MSG_SE_ERROR_NO_INPUT (LogLevel.DEBUG, R.string.msg_se_error_no_input),
        MSG_SE_ERROR_TOO_MANY_INPUTS (LogLevel.ERROR, R.string.msg_se_error_too_many_inputs),
        MSG_SE_SUCCESS (LogLevel.OK, R.string.msg_se_success),

        // pgpsignencrypt
        MSG_PSE_INPUT_BYTES (LogLevel.INFO, R.string.msg_se_input_bytes),
        MSG_PSE_INPUT_URI (LogLevel.INFO, R.string.msg_se_input_uri),
        MSG_PSE_ERROR_INPUT_URI_NOT_FOUND (LogLevel.ERROR, R.string.msg_se_error_input_uri_not_found),
        MSG_PSE_ERROR_OUTPUT_URI_NOT_FOUND (LogLevel.ERROR, R.string.msg_se_error_output_uri_not_found),
        MSG_PSE_ASYMMETRIC (LogLevel.INFO, R.string.msg_pse_asymmetric),
        MSG_PSE_COMPRESSING (LogLevel.DEBUG, R.string.msg_pse_compressing),
        MSG_PSE_ENCRYPTING (LogLevel.DEBUG, R.string.msg_pse_encrypting),
        MSG_PSE_ERROR_BAD_PASSPHRASE (LogLevel.ERROR, R.string.msg_pse_error_bad_passphrase),
        MSG_PSE_ERROR_IO (LogLevel.ERROR, R.string.msg_pse_error_io),
        MSG_PSE_ERROR_SIGN_KEY(LogLevel.ERROR, R.string.msg_pse_error_sign_key),
        MSG_PSE_ERROR_KEY_SIGN (LogLevel.ERROR, R.string.msg_pse_error_key_sign),
        MSG_PSE_ERROR_NFC (LogLevel.ERROR, R.string.msg_pse_error_nfc),
        MSG_PSE_ERROR_PGP (LogLevel.ERROR, R.string.msg_pse_error_pgp),
        MSG_PSE_ERROR_SIG (LogLevel.ERROR, R.string.msg_pse_error_sig),
        MSG_PSE_ERROR_UNLOCK (LogLevel.ERROR, R.string.msg_pse_error_unlock),
        MSG_PSE_ERROR_KEY_NOT_ALLOWED(LogLevel.ERROR, R.string.msg_pse_error_key_not_allowed),
        MSG_PSE_ERROR_REVOKED_OR_EXPIRED (LogLevel.ERROR, R.string.msg_pse_error_revoked_or_expired),
        MSG_PSE_KEY_OK (LogLevel.OK, R.string.msg_pse_key_ok),
        MSG_PSE_KEY_UNKNOWN (LogLevel.DEBUG, R.string.msg_pse_key_unknown),
        MSG_PSE_KEY_WARN (LogLevel.WARN, R.string.msg_pse_key_warn),
        MSG_PSE_OK (LogLevel.OK, R.string.msg_pse_ok),
        MSG_PSE_PENDING_NFC (LogLevel.INFO, R.string.msg_pse_pending_nfc),
        MSG_PSE_PENDING_PASSPHRASE (LogLevel.INFO, R.string.msg_pse_pending_passphrase),
        MSG_PSE (LogLevel.DEBUG, R.string.msg_pse),
        MSG_PSE_SIGNING (LogLevel.DEBUG, R.string.msg_pse_signing),
        MSG_PSE_SIGNING_CLEARTEXT (LogLevel.DEBUG, R.string.msg_pse_signing_cleartext),
        MSG_PSE_SIGNING_DETACHED (LogLevel.DEBUG, R.string.msg_pse_signing_detached),
        MSG_PSE_SIGCRYPTING (LogLevel.DEBUG, R.string.msg_pse_sigcrypting),
        MSG_PSE_SYMMETRIC (LogLevel.INFO, R.string.msg_pse_symmetric),

        MSG_AUTH (LogLevel.DEBUG, R.string.msg_auth),
        MSG_AUTH_ERROR_KEY_AUTH (LogLevel.ERROR, R.string.msg_auth_error_key_auth),
        MSG_AUTH_ERROR_KEY_NOT_ALLOWED(LogLevel.ERROR, R.string.msg_auth_error_key_not_allowed),
        MSG_AUTH_ERROR_REVOKED_OR_EXPIRED (LogLevel.ERROR, R.string.msg_auth_error_revoked_or_expired),
        MSG_AUTH_ERROR_UNLOCK (LogLevel.ERROR, R.string.msg_auth_error_unlock),
        MSG_AUTH_PENDING_NFC (LogLevel.INFO, R.string.msg_auth_pending_nfc),
        MSG_AUTH_PENDING_PASSPHRASE (LogLevel.INFO, R.string.msg_auth_pending_passphrase),
        MSG_AUTH_ERROR_BAD_PASSPHRASE (LogLevel.ERROR, R.string.msg_auth_error_bad_passphrase),
        MSG_AUTH_ERROR_NFC (LogLevel.ERROR, R.string.msg_auth_error_nfc),
        MSG_AUTH_ERROR_SIG (LogLevel.ERROR, R.string.msg_auth_error_sig),
        MSG_AUTH_OK (LogLevel.OK, R.string.msg_auth_ok),


        MSG_CRT_CERTIFYING (LogLevel.DEBUG, R.string.msg_crt_certifying),
        MSG_CRT_CERTIFY_UIDS (LogLevel.DEBUG, R.plurals.msg_crt_certify_uids),
        MSG_CRT_CERTIFY_UATS (LogLevel.DEBUG, R.plurals.msg_crt_certify_uats),
        MSG_CRT_ERROR_SELF (LogLevel.ERROR, R.string.msg_crt_error_self),
        MSG_CRT_ERROR_MASTER_NOT_FOUND (LogLevel.ERROR, R.string.msg_crt_error_master_not_found),
        MSG_CRT_ERROR_NOTHING (LogLevel.ERROR, R.string.msg_crt_error_nothing),
        MSG_CRT_ERROR_UNLOCK (LogLevel.ERROR, R.string.msg_crt_error_unlock),
        MSG_CRT (LogLevel.START, R.string.msg_crt),
        MSG_CRT_MASTER_FETCH (LogLevel.DEBUG, R.string.msg_crt_master_fetch),
        MSG_CRT_NFC_RETURN (LogLevel.OK, R.string.msg_crt_nfc_return),
        MSG_CRT_SAVE (LogLevel.DEBUG, R.string.msg_crt_save),
        MSG_CRT_SAVING (LogLevel.DEBUG, R.string.msg_crt_saving),
        MSG_CRT_SUCCESS (LogLevel.OK, R.string.msg_crt_success),
        MSG_CRT_UNLOCK (LogLevel.DEBUG, R.string.msg_crt_unlock),
        MSG_CRT_WARN_NOT_FOUND (LogLevel.WARN, R.string.msg_crt_warn_not_found),
        MSG_CRT_WARN_CERT_FAILED (LogLevel.WARN, R.string.msg_crt_warn_cert_failed),
        MSG_CRT_WARN_SAVE_FAILED (LogLevel.WARN, R.string.msg_crt_warn_save_failed),
        MSG_CRT_WARN_UPLOAD_FAILED (LogLevel.WARN, R.string.msg_crt_warn_upload_failed),

        MSG_IMPORT (LogLevel.START, R.plurals.msg_import),

        MSG_IMPORT_FETCH_ERROR (LogLevel.ERROR, R.string.msg_import_fetch_error),
        MSG_IMPORT_FETCH_ERROR_DECODE (LogLevel.ERROR, R.string.msg_import_fetch_error_decode),
        MSG_IMPORT_FETCH_ERROR_NOT_FOUND (LogLevel.ERROR, R.string.msg_import_fetch_error_not_found),
        MSG_IMPORT_FETCH_ERROR_KEYSERVER(LogLevel.ERROR, R.string.msg_import_fetch_error_keyserver),
        MSG_IMPORT_FETCH_ERROR_KEYSERVER_SECRET (LogLevel.ERROR, R.string.msg_import_fetch_error_keyserver_secret),
        MSG_IMPORT_FETCH_KEYBASE (LogLevel.INFO, R.string.msg_import_fetch_keybase),
        MSG_IMPORT_FETCH_FACEBOOK (LogLevel.INFO, R.string.msg_import_fetch_facebook),
        MSG_IMPORT_FETCH_KEYSERVER (LogLevel.INFO, R.string.msg_import_fetch_keyserver),
        MSG_IMPORT_FETCH_KEYSERVER_OK (LogLevel.DEBUG, R.string.msg_import_fetch_keyserver_ok),
        MSG_IMPORT_KEYSERVER (LogLevel.DEBUG, R.string.msg_import_keyserver),
        MSG_IMPORT_MERGE (LogLevel.DEBUG, R.string.msg_import_merge),
        MSG_IMPORT_MERGE_ERROR (LogLevel.ERROR, R.string.msg_import_merge_error),
        MSG_IMPORT_ERROR (LogLevel.ERROR, R.string.msg_import_error),
        MSG_IMPORT_ERROR_IO (LogLevel.ERROR, R.string.msg_import_error_io),
        MSG_IMPORT_PARTIAL (LogLevel.ERROR, R.string.msg_import_partial),
        MSG_IMPORT_SUCCESS (LogLevel.OK, R.string.msg_import_success),

        MSG_BACKUP(LogLevel.START, R.plurals.msg_backup),
        MSG_BACKUP_PUBLIC(LogLevel.DEBUG, R.string.msg_backup_public),
        MSG_BACKUP_SECRET(LogLevel.DEBUG, R.string.msg_backup_secret),
        MSG_BACKUP_ALL(LogLevel.START, R.string.msg_backup_all),
        MSG_BACKUP_ERROR_URI_OPEN(LogLevel.ERROR, R.string.msg_backup_error_uri_open),
        MSG_BACKUP_ERROR_DB(LogLevel.ERROR, R.string.msg_backup_error_db),
        MSG_BACKUP_ERROR_IO(LogLevel.ERROR, R.string.msg_backup_error_io),
        MSG_BACKUP_SUCCESS(LogLevel.OK, R.string.msg_backup_success),

        MSG_UPLOAD(LogLevel.START, R.string.msg_upload),
        MSG_UPLOAD_KEY(LogLevel.INFO, R.string.msg_upload_key),
        MSG_UPLOAD_PROXY_DIRECT(LogLevel.DEBUG, R.string.msg_upload_proxy_direct),
        MSG_UPLOAD_PROXY_TOR(LogLevel.DEBUG, R.string.msg_upload_proxy_tor),
        MSG_UPLOAD_PROXY(LogLevel.DEBUG, R.string.msg_upload_proxy),
        MSG_UPLOAD_SERVER(LogLevel.DEBUG, R.string.msg_upload_server),
        MSG_UPLOAD_SUCCESS(LogLevel.OK, R.string.msg_upload_success),
        MSG_UPLOAD_ERROR_NOT_FOUND(LogLevel.ERROR, R.string.msg_upload_error_not_found),
        MSG_UPLOAD_ERROR_IO(LogLevel.ERROR, R.string.msg_upload_error_key),
        MSG_UPLOAD_ERROR_UPLOAD(LogLevel.ERROR, R.string.msg_upload_error_upload),

        MSG_CRT_UPLOAD_SUCCESS (LogLevel.OK, R.string.msg_crt_upload_success),

        MSG_WRONG_QR_CODE (LogLevel.ERROR, R.string.import_qr_code_wrong),
        MSG_WRONG_QR_CODE_FP(LogLevel.ERROR, R.string.import_qr_code_fp),

        MSG_NO_VALID_ENC (LogLevel.ERROR, R.string.error_invalid_data),

        // get key
        MSG_GET_SUCCESS (LogLevel.OK, R.string.msg_get_success),
        MSG_GET_NO_VALID_KEYS (LogLevel.ERROR, R.string.msg_get_no_valid_keys),
        MSG_GET_QUERY_TOO_SHORT (LogLevel.ERROR, R.string.msg_get_query_too_short),
        MSG_GET_TOO_MANY_RESPONSES (LogLevel.ERROR, R.string.msg_get_too_many_responses),
        MSG_GET_QUERY_TOO_SHORT_OR_TOO_MANY_RESPONSES (LogLevel.ERROR, R.string.msg_get_query_too_short_or_too_many_responses),
        MSG_GET_QUERY_FAILED (LogLevel.ERROR, R.string.msg_download_query_failed),
        MSG_GET_QUERY_NOT_IMPLEMENTED (LogLevel.ERROR, R.string.msg_get_query_not_implemented),
        MSG_GET_FILE_NOT_FOUND (LogLevel.ERROR, R.string.msg_get_file_not_found),
        MSG_GET_NO_ENABLED_SOURCE (LogLevel.ERROR, R.string.msg_get_no_enabled_source),

        MSG_DEL_ERROR_EMPTY (LogLevel.ERROR, R.string.msg_del_error_empty),
        MSG_DEL_ERROR_MULTI_SECRET (LogLevel.ERROR, R.string.msg_del_error_multi_secret),
        MSG_DEL (LogLevel.START, R.plurals.msg_del),
        MSG_DEL_KEY (LogLevel.INFO, R.string.msg_del_key),
        MSG_DEL_KEY_FAIL (LogLevel.WARN, R.string.msg_del_key_fail),
        MSG_DEL_OK (LogLevel.OK, R.plurals.msg_del_ok),
        MSG_DEL_FAIL (LogLevel.WARN, R.plurals.msg_del_fail),

        MSG_REVOKE_ERROR_EMPTY (LogLevel.ERROR, R.string.msg_revoke_error_empty),
        MSG_REVOKE_ERROR_NOT_FOUND (LogLevel.ERROR, R.string.msg_revoke_error_not_found),
        MSG_REVOKE (LogLevel.DEBUG, R.string.msg_revoke_key),
        MSG_REVOKE_ERROR_KEY_FAIL (LogLevel.ERROR, R.string.msg_revoke_key_fail),
        MSG_REVOKE_OK (LogLevel.OK, R.string.msg_revoke_ok),

        // keybase verification
        MSG_KEYBASE_VERIFICATION(LogLevel.START, R.string.msg_keybase_verification),

        MSG_KEYBASE_ERROR_NO_PROVER(LogLevel.ERROR, R.string.msg_keybase_error_no_prover),
        MSG_KEYBASE_ERROR_FETCH_PROOF(LogLevel.ERROR, R.string.msg_keybase_error_fetching_evidence),
        MSG_KEYBASE_ERROR_FINGERPRINT_MISMATCH(LogLevel.ERROR,
                R.string.msg_keybase_error_key_mismatch),
        MSG_KEYBASE_ERROR_DNS_FAIL(LogLevel.ERROR, R.string.msg_keybase_error_dns_fail),
        MSG_KEYBASE_ERROR_SPECIFIC(LogLevel.ERROR, R.string.msg_keybase_error_specific),
        MSG_KEYBASE_ERROR_PAYLOAD_MISMATCH(LogLevel.ERROR,
                R.string.msg_keybase_error_msg_payload_mismatch),

        // InputData Operation
        MSG_DATA (LogLevel.START, R.string.msg_data),
        MSG_DATA_OPENPGP (LogLevel.DEBUG, R.string.msg_data_openpgp),
        MSG_DATA_ERROR_IO (LogLevel.ERROR, R.string.msg_data_error_io),
        MSG_DATA_DETACHED (LogLevel.INFO, R.string.msg_data_detached),
        MSG_DATA_DETACHED_CLEAR (LogLevel.WARN, R.string.msg_data_detached_clear),
        MSG_DATA_DETACHED_SIG (LogLevel.DEBUG, R.string.msg_data_detached_sig),
        MSG_DATA_DETACHED_RAW (LogLevel.DEBUG, R.string.msg_data_detached_raw),
        MSG_DATA_DETACHED_NESTED(LogLevel.WARN, R.string.msg_data_detached_nested),
        MSG_DATA_DETACHED_TRAILING (LogLevel.WARN, R.string.msg_data_detached_trailing),
        MSG_DATA_DETACHED_UNSUPPORTED (LogLevel.WARN, R.string.msg_data_detached_unsupported),
        MSG_DATA_MIME_BAD(LogLevel.INFO, R.string.msg_data_mime_bad),
        MSG_DATA_MIME_FROM_EXTENSION (LogLevel.DEBUG, R.string.msg_data_mime_from_extension),
        MSG_DATA_MIME_FILENAME (LogLevel.DEBUG, R.string.msg_data_mime_filename),
        MSG_DATA_MIME_LENGTH (LogLevel.DEBUG, R.string.msg_data_mime_length),
        MSG_DATA_MIME_CHARSET (LogLevel.DEBUG, R.string.msg_data_mime_charset),
        MSG_DATA_MIME_CHARSET_FAULTY (LogLevel.WARN, R.string.msg_data_mime_charset_faulty),
        MSG_DATA_MIME_CHARSET_GUESS (LogLevel.DEBUG, R.string.msg_data_mime_charset_guess),
        MSG_DATA_MIME_CHARSET_UNKNOWN (LogLevel.DEBUG, R.string.msg_data_mime_charset_unknown),
        MSG_DATA_MIME (LogLevel.DEBUG, R.string.msg_data_mime),
        MSG_DATA_MIME_OK (LogLevel.INFO, R.string.msg_data_mime_ok),
        MSG_DATA_MIME_NONE (LogLevel.DEBUG, R.string.msg_data_mime_none),
        MSG_DATA_MIME_PART (LogLevel.DEBUG, R.string.msg_data_mime_part),
        MSG_DATA_MIME_TYPE (LogLevel.DEBUG, R.string.msg_data_mime_type),
        MSG_DATA_OK (LogLevel.OK, R.string.msg_data_ok),
        MSG_DATA_SKIP_MIME (LogLevel.DEBUG, R.string.msg_data_skip_mime),

        MSG_LV (LogLevel.START, R.string.msg_lv),
        MSG_LV_MATCH (LogLevel.DEBUG, R.string.msg_lv_match),
        MSG_LV_MATCH_ERROR (LogLevel.ERROR, R.string.msg_lv_match_error),
        MSG_LV_FP_OK (LogLevel.DEBUG, R.string.msg_lv_fp_ok),
        MSG_LV_FP_ERROR (LogLevel.ERROR, R.string.msg_lv_fp_error),

        MSG_LV_ERROR_TWITTER_AUTH (LogLevel.ERROR, R.string.msg_lv_error_twitter_auth),
        MSG_LV_ERROR_TWITTER_HANDLE (LogLevel.ERROR, R.string.msg_lv_error_twitter_handle),
        MSG_LV_ERROR_TWITTER_RESPONSE (LogLevel.ERROR, R.string.msg_lv_error_twitter_response),
        MSG_LV_ERROR_GITHUB_HANDLE (LogLevel.ERROR, R.string.msg_lv_error_github_handle),
        MSG_LV_ERROR_GITHUB_NOT_FOUND (LogLevel.ERROR, R.string.msg_lv_error_github_not_found),

        MSG_LV_FETCH (LogLevel.DEBUG, R.string.msg_lv_fetch),
        MSG_LV_FETCH_REDIR (LogLevel.DEBUG, R.string.msg_lv_fetch_redir),
        MSG_LV_FETCH_OK (LogLevel.DEBUG, R.string.msg_lv_fetch_ok),
        MSG_LV_FETCH_ERROR (LogLevel.ERROR, R.string.msg_lv_fetch_error),
        MSG_LV_FETCH_ERROR_URL (LogLevel.ERROR, R.string.msg_lv_fetch_error_url),
        MSG_LV_FETCH_ERROR_IO (LogLevel.ERROR, R.string.msg_lv_fetch_error_io),
        MSG_LV_FETCH_ERROR_FORMAT(LogLevel.ERROR, R.string.msg_lv_fetch_error_format),
        MSG_LV_FETCH_ERROR_NOTHING (LogLevel.ERROR, R.string.msg_lv_fetch_error_nothing),

        MSG_BENCH (LogLevel.START, R.string.msg_bench),
        MSG_BENCH_ENC_TIME (LogLevel.DEBUG, R.string.msg_bench_enc_time),
        MSG_BENCH_ENC_TIME_AVG (LogLevel.INFO, R.string.msg_bench_enc_time_avg),
        MSG_BENCH_DEC_TIME (LogLevel.DEBUG, R.string.msg_bench_dec_time),
        MSG_BENCH_DEC_TIME_AVG (LogLevel.INFO, R.string.msg_bench_enc_time_avg),
        MSG_BENCH_S2K_FOR_IT (LogLevel.DEBUG, R.string.msg_bench_s2k_for_it),
        MSG_BENCH_S2K_100MS_ITS (LogLevel.INFO, R.string.msg_bench_s2k_100ms_its),
        MSG_BENCH_SUCCESS (LogLevel.OK, R.string.msg_bench_success),

        MSG_RET_CURI_ERROR_IO (LogLevel.ERROR, R.string.msg_ret_curi_error_io),
        MSG_RET_CURI_ERROR_NO_MATCH (LogLevel.ERROR, R.string.msg_ret_curi_error_no_match),
        MSG_RET_CURI_ERROR_NOT_FOUND (LogLevel.ERROR, R.string.msg_ret_curi_error_not_found),
        MSG_RET_CURI_FOUND (LogLevel.DEBUG, R.string.msg_ret_curi_found),
        MSG_RET_CURI_MISMATCH (LogLevel.ERROR, R.string.msg_ret_curi_mismatch),
        MSG_RET_CURI_OK (LogLevel.OK, R.string.msg_ret_curi_ok),
        MSG_RET_CURI_OPEN (LogLevel.DEBUG, R.string.msg_ret_curi_open),
        MSG_RET_CURI_START (LogLevel.START, R.string.msg_ret_curi_start),
        MSG_RET_KS_ERROR_NOT_FOUND (LogLevel.ERROR, R.string.msg_ret_ks_error_not_found),
        MSG_RET_KS_ERROR (LogLevel.ERROR, R.string.msg_ret_ks_error),
        MSG_RET_KS_FP_MATCH (LogLevel.DEBUG, R.string.msg_ret_ks_fp_match),
        MSG_RET_KS_FP_MISMATCH (LogLevel.ERROR, R.string.msg_ret_ks_fp_mismatch),
        MSG_RET_KS_OK (LogLevel.OK, R.string.msg_ret_ks_ok),
        MSG_RET_KS_START (LogLevel.START, R.string.msg_ret_ks_start),
        MSG_RET_LOCAL_SEARCH(LogLevel.DEBUG, R.string.msg_ret_local_search),
        MSG_RET_LOCAL_FP_MATCH (LogLevel.DEBUG, R.string.msg_ret_local_fp_match),
        MSG_RET_LOCAL_FP_MISMATCH (LogLevel.ERROR, R.string.msg_ret_local_fp_mismatch),
        MSG_RET_LOCAL_NOT_FOUND (LogLevel.DEBUG, R.string.msg_ret_local_not_found),
        MSG_RET_LOCAL_NONE_FOUND (LogLevel.ERROR, R.string.msg_ret_local_none_found),
        MSG_RET_LOCAL_OK (LogLevel.OK, R.string.msg_ret_local_ok),
        MSG_RET_LOCAL_SECRET (LogLevel.INFO, R.string.msg_ret_local_secret),
        MSG_RET_LOCAL_START (LogLevel.START, R.string.msg_ret_local_start),
        MSG_RET_URI_ERROR_NO_MATCH(LogLevel.ERROR, R.string.msg_ret_uri_error_no_match),
        MSG_RET_URI_ERROR_FETCH (LogLevel.ERROR, R.string.msg_ret_uri_error_fetch),
        MSG_RET_URI_ERROR_PARSE (LogLevel.ERROR, R.string.msg_ret_uri_error_parse),
        MSG_RET_URI_FETCHING (LogLevel.DEBUG, R.string.msg_ret_uri_fetching),
        MSG_RET_URI_OK (LogLevel.OK, R.string.msg_ret_uri_ok),
        MSG_RET_URI_START (LogLevel.START, R.string.msg_ret_uri_start),
        MSG_RET_URI_NULL (LogLevel.ERROR, R.string.msg_ret_uri_null),
        MSG_RET_URI_TEST (LogLevel.DEBUG, R.string.msg_ret_uri_test),

        MSG_TRUST (LogLevel.START, R.string.msg_trust),
        MSG_TRUST_OK (LogLevel.OK, R.string.msg_trust_ok),
        MSG_TRUST_KEY (LogLevel.INFO, R.string.msg_trust_key),
        MSG_TRUST_INITIALIZE (LogLevel.INFO, R.string.msg_trust_initialize),
        MSG_TRUST_COUNT_NONE (LogLevel.DEBUG, R.string.msg_trust_count_none),
        MSG_TRUST_COUNT (LogLevel.DEBUG, R.plurals.msg_trust_count);

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
    public enum LogLevel {
        DEBUG,
        INFO,
        WARN,
        ERROR, // should occur once at the end of a failed operation
        START, // should occur once at the start of each independent operation
        OK, // should occur once at the end of a successful operation
        CANCELLED, // should occur once at the end of a cancelled operation
    }
    // for print of debug log. keep those in sync with above!
    static final String[] LOG_LEVEL_NAME = new String[] {
            "[DEBUG]", "[INFO]", "[WARN]", "[ERROR]", "[START]", "[OK]", "[CANCEL]"
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mResult);
        // cache log and write UUID to dest
        logCache.cacheAndWriteToParcel(mLog, dest);
    }

    public static class OperationLog implements Iterable<LogEntryParcel> {

        private final List<LogEntryParcel> mParcels = new ArrayList<>();

        /// Simple convenience method
        public void add(LogType type, int indent, Object... parameters) {
            mParcels.add(new OperationResult.LogEntryParcel(type, indent, parameters));
        }

        public void add(LogType type, int indent) {
            mParcels.add(new OperationResult.LogEntryParcel(type, indent, (Object[]) null));
        }

        public void add(OperationResult subResult, int indent) {
            OperationLog subLog = subResult.getLog();
            mParcels.add(new SubLogEntryParcel(subResult, subLog.getFirst().mType, indent, subLog.getFirst().mParameters));
        }

        public void addByMerge(OperationResult subResult, int indent) {
            OperationLog subLog = subResult.getLog();
            for (LogEntryParcel entry : subLog) {
                mParcels.add(new LogEntryParcel(entry, indent));
            }
        }

        public SubLogEntryParcel getSubResultIfSingle() {
            if (mParcels.size() != 1) {
                return null;
            }
            LogEntryParcel first = getFirst();
            if (first instanceof SubLogEntryParcel) {
                return (SubLogEntryParcel) first;
            }
            return null;
        }

        public void clear() {
            mParcels.clear();
        }

        public boolean containsType(LogType type) {
            for(LogEntryParcel entry : new IterableIterator<>(mParcels.iterator())) {
                if (entry.mType == type) {
                    return true;
                }
            }
            return false;
        }

        public boolean containsWarnings() {
            for(LogEntryParcel entry : new IterableIterator<>(mParcels.iterator())) {
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

        public LogEntryParcel getFirst() {
            if (mParcels.isEmpty()) {
                return null;
            }
            return mParcels.get(0);
        }

        public LogEntryParcel getLast() {
            if (mParcels.isEmpty()) {
                return null;
            }
            LogEntryParcel last = mParcels.get(mParcels.size() -1);
            if (last instanceof SubLogEntryParcel) {
                return ((SubLogEntryParcel) last).getSubResult().getLog().getLast();
            }
            return last;
        }

        @Override
        public Iterator<LogEntryParcel> iterator() {
            return mParcels.iterator();
        }

        /**
         * returns an indented String of an entire OperationLog
         * @param indent padding to add at the start of all log entries, made for use with SubLogs
         * @return printable, indented version of passed operationLog
         */
        public String getPrintableOperationLog(Resources resources, int indent) {
            StringBuilder log = new StringBuilder();
            for (LogEntryParcel entry : this) {
                log.append(entry.getPrintableLogEntry(resources, indent)).append("\n");
            }
            if (log.length() >= 1) {
                return log.toString().substring(0, log.length() - 1); // get rid of extra new line
            } else {
                return log.toString();
            }

        }

    }

}

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


import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedKeyRing;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.LogDisplayActivity;
import org.sufficientlysecure.keychain.ui.LogDisplayFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Showable;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

public class ImportKeyResult extends InputPendingResult {

    public final int mNewKeys, mUpdatedKeys, mMissingKeys, mBadKeys, mSecret;
    public final long[] mImportedMasterKeyIds;

    // NOT PARCELED
    public ArrayList<CanonicalizedKeyRing> mCanonicalizedKeyRings;

    // At least one new key
    public static final int RESULT_OK_NEWKEYS = 8;
    // At least one updated key
    public static final int RESULT_OK_UPDATED = 16;
    // At least one key failed (might still be an overall success)
    public static final int RESULT_WITH_ERRORS = 32;

    // No keys to import...
    public static final int RESULT_FAIL_NOTHING = 64 + 1;

    public boolean isOkBoth() {
        return (mResult & (RESULT_OK_NEWKEYS | RESULT_OK_UPDATED))
                == (RESULT_OK_NEWKEYS | RESULT_OK_UPDATED);
    }

    public boolean isOkNew() {
        return (mResult & RESULT_OK_NEWKEYS) == RESULT_OK_NEWKEYS;
    }

    public boolean isOkUpdated() {
        return (mResult & RESULT_OK_UPDATED) == RESULT_OK_UPDATED;
    }

    public boolean isOkWithErrors() {
        return (mResult & RESULT_WITH_ERRORS) == RESULT_WITH_ERRORS;
    }

    public boolean isFailNothing() {
        return (mResult & RESULT_FAIL_NOTHING) == RESULT_FAIL_NOTHING;
    }

    public boolean isFailMissing() {
        return isFailNothing() && mMissingKeys > 0;
    }

    public long[] getImportedMasterKeyIds() {
        return mImportedMasterKeyIds;
    }

    public ImportKeyResult(Parcel source) {
        super(source);
        mNewKeys = source.readInt();
        mUpdatedKeys = source.readInt();
        mMissingKeys = source.readInt();
        mBadKeys = source.readInt();
        mSecret = source.readInt();
        mImportedMasterKeyIds = source.createLongArray();
    }

    public ImportKeyResult(int result, OperationLog log) {
        this(result, log, 0, 0, 0, 0, 0, new long[]{});
    }

    public ImportKeyResult(int result, OperationLog log,
                           int newKeys, int updatedKeys, int missingKeys, int badKeys, int secret,
                           long[] importedMasterKeyIds) {
        super(result, log);
        mNewKeys = newKeys;
        mUpdatedKeys = updatedKeys;
        mMissingKeys = missingKeys;
        mBadKeys = badKeys;
        mSecret = secret;
        mImportedMasterKeyIds = importedMasterKeyIds;
    }

    public ImportKeyResult(OperationLog log, RequiredInputParcel requiredInputParcel,
                           CryptoInputParcel cryptoInputParcel) {
        super(log, requiredInputParcel, cryptoInputParcel);
        // just assign default values, we won't use them anyway
        mNewKeys = 0;
        mUpdatedKeys = 0;
        mMissingKeys = 0;
        mBadKeys = 0;
        mSecret = 0;
        mImportedMasterKeyIds = new long[]{};
    }

    public void setCanonicalizedKeyRings(ArrayList<CanonicalizedKeyRing> canonicalizedKeyRings) {
        this.mCanonicalizedKeyRings = canonicalizedKeyRings;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mNewKeys);
        dest.writeInt(mUpdatedKeys);
        dest.writeInt(mMissingKeys);
        dest.writeInt(mBadKeys);
        dest.writeInt(mSecret);
        dest.writeLongArray(mImportedMasterKeyIds);
    }

    public static final Creator<ImportKeyResult> CREATOR = new Creator<ImportKeyResult>() {
        public ImportKeyResult createFromParcel(final Parcel source) {
            return new ImportKeyResult(source);
        }

        public ImportKeyResult[] newArray(final int size) {
            return new ImportKeyResult[size];
        }
    };

    public Showable createNotify(final Activity activity) {
        int resultType = getResult();

        String str;
        int duration;
        Style style;

        // Not an overall failure
        if ((resultType & OperationResult.RESULT_ERROR) == 0) {
            String withWarnings;

            duration = Notify.LENGTH_LONG;
            style = Style.OK;
            withWarnings = "";

            // Any warnings?
            if ((resultType & ImportKeyResult.RESULT_WARNINGS) > 0) {
                duration = 0;
                style = Style.WARN;
                withWarnings += activity.getString(R.string.with_warnings);
            }
            if ((resultType & ImportKeyResult.RESULT_CANCELLED) > 0) {
                duration = 0;
                style = Style.WARN;
                withWarnings += activity.getString(R.string.with_cancelled);
            }

            // New and updated keys
            if (isOkBoth()) {
                str = activity.getResources().getQuantityString(
                        R.plurals.import_keys_added_and_updated_1, mNewKeys, mNewKeys);
                str += " " + activity.getResources().getQuantityString(
                        R.plurals.import_keys_added_and_updated_2, mUpdatedKeys, mUpdatedKeys, withWarnings);
            } else if (isOkUpdated()) {
                str = activity.getResources().getQuantityString(
                        R.plurals.import_keys_updated, mUpdatedKeys, mUpdatedKeys, withWarnings);
            } else if (isOkNew()) {
                str = activity.getResources().getQuantityString(
                        R.plurals.import_keys_added, mNewKeys, mNewKeys, withWarnings);
            } else {
                duration = 0;
                style = Style.ERROR;
                str = "internal error";
            }
            if (isOkWithErrors()) {
                // definitely switch to warning-style message in this case!
                duration = 0;
                style = Style.WARN;
                str += " " + activity.getResources().getQuantityString(
                        R.plurals.import_keys_with_errors, mBadKeys, mBadKeys);
            }

        } else {
            if (isFailMissing()) {
                duration = 0;
                style = Style.WARN;
                str = activity.getResources().getString(R.string.import_warn_missing);
            } else if (isFailNothing()) {
                duration = 0;
                style = Style.ERROR;
                str = activity.getString((resultType & ImportKeyResult.RESULT_CANCELLED) > 0
                        ? R.string.import_error_nothing_cancelled
                        : R.string.import_error_nothing);
            } else {
                duration = 0;
                style = Style.ERROR;
                str = activity.getResources().getQuantityString(R.plurals.import_error, mBadKeys, mBadKeys);
            }
        }

        return Notify.create(activity, str, duration, style, new ActionListener() {
            @Override
            public void onAction() {
                Intent intent = new Intent(
                        activity, LogDisplayActivity.class);
                intent.putExtra(LogDisplayFragment.EXTRA_RESULT, ImportKeyResult.this);
                activity.startActivity(intent);
            }
        }, R.string.snackbar_details);
    }

}

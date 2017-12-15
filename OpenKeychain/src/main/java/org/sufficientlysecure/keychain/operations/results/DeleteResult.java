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
import android.os.Parcel;
import android.support.annotation.Nullable;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.LogDisplayActivity;
import org.sufficientlysecure.keychain.ui.LogDisplayFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Showable;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

public class DeleteResult extends InputPendingResult {

    final public int mOk, mFail;

    public DeleteResult(int result, OperationLog log, int ok, int fail) {
        super(result, log);
        mOk = ok;
        mFail = fail;
    }

    /**
     * used when more input is required
     * @param log operation log upto point of required input, if any
     * @param requiredInput represents input required
     */
    public DeleteResult(@Nullable OperationLog log, RequiredInputParcel requiredInput,
                        CryptoInputParcel cryptoInputParcel) {
        super(log, requiredInput, cryptoInputParcel);
        // values are not to be used
        mOk = -1;
        mFail = -1;
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public DeleteResult(Parcel source) {
        super(source);
        mOk = source.readInt();
        mFail = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mOk);
        dest.writeInt(mFail);
    }

    public static Creator<DeleteResult> CREATOR = new Creator<DeleteResult>() {
        public DeleteResult createFromParcel(final Parcel source) {
            return new DeleteResult(source);
        }

        public DeleteResult[] newArray(final int size) {
            return new DeleteResult[size];
        }
    };

    public Showable createNotify(final Activity activity) {

        int resultType = getResult();

        String str;
        int duration;
        Style style;

        // Not an overall failure
        if ((resultType & OperationResult.RESULT_ERROR) == 0) {
            String untilCancelled;

            duration = Notify.LENGTH_LONG;
            style = Style.OK;
            untilCancelled = "";

            // Any warnings?
            if ((resultType & ImportKeyResult.RESULT_CANCELLED) > 0) {
                duration = 0;
                style = Style.WARN;
                untilCancelled += activity.getString(R.string.with_cancelled);
            }

            // New and updated keys
            if (mOk > 0 && mFail > 0) {
                style = Style.WARN;
                duration = 0;
                str = activity.getResources().getQuantityString(
                        R.plurals.delete_ok_but_fail_1, mOk, mOk);
                str += " " + activity.getResources().getQuantityString(
                        R.plurals.delete_ok_but_fail_2, mFail, mFail, untilCancelled);
            } else if (mOk > 0) {
                str = activity.getResources().getQuantityString(
                        R.plurals.delete_ok, mOk, mOk, untilCancelled);
            } else if ((resultType & ImportKeyResult.RESULT_CANCELLED) > 0) {
                str = activity.getString(R.string.delete_cancelled);
            } else {
                duration = 0;
                style = Style.ERROR;
                str = "internal error";
            }

        } else {
            duration = 0;
            style = Style.ERROR;
            if (mLog.getLast().mType == LogType.MSG_DEL_ERROR_MULTI_SECRET) {
                str = activity.getString(R.string.secret_cannot_multiple);
            }
            else if (mFail == 0) {
                str = activity.getString(R.string.delete_nothing);
            } else {
                str = activity.getResources().getQuantityString(
                        R.plurals.delete_fail, mFail, mFail);
            }
        }

        return Notify.create(activity, str, duration, style, new ActionListener() {
            @Override
            public void onAction() {
                Intent intent = new Intent(
                        activity, LogDisplayActivity.class);
                intent.putExtra(LogDisplayFragment.EXTRA_RESULT, DeleteResult.this);
                activity.startActivity(intent);
            }
        }, R.string.snackbar_details);

    }

}

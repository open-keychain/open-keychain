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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.LogDisplayActivity;
import org.sufficientlysecure.keychain.ui.LogDisplayFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Showable;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

public class CertifyResult extends InputPendingResult {
    int mCertifyOk, mCertifyError, mUploadOk, mUploadError;

    public CertifyResult(int result, OperationLog log) {
        super(result, log);
    }

    public CertifyResult(OperationLog log, RequiredInputParcel requiredInput,
                         CryptoInputParcel cryptoInputParcel) {
        super(log, requiredInput, cryptoInputParcel);
    }

    public CertifyResult(int result, OperationLog log, int certifyOk, int certifyError, int uploadOk, int uploadError) {
        super(result, log);
        mCertifyOk = certifyOk;
        mCertifyError = certifyError;
        mUploadOk = uploadOk;
        mUploadError = uploadError;
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public CertifyResult(Parcel source) {
        super(source);
        mCertifyOk = source.readInt();
        mCertifyError = source.readInt();
        mUploadOk = source.readInt();
        mUploadError = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mCertifyOk);
        dest.writeInt(mCertifyError);
        dest.writeInt(mUploadOk);
        dest.writeInt(mUploadError);
    }

    public static Creator<CertifyResult> CREATOR = new Creator<CertifyResult>() {
        public CertifyResult createFromParcel(final Parcel source) {
            return new CertifyResult(source);
        }

        public CertifyResult[] newArray(final int size) {
            return new CertifyResult[size];
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
            str = activity.getResources().getQuantityString(
                    R.plurals.certify_keys_ok, mCertifyOk, mCertifyOk, withWarnings);
            if (mCertifyError > 0) {
                // definitely switch to warning-style message in this case!
                duration = 0;
                style = Style.ERROR;
                str += " " + activity.getResources().getQuantityString(
                        R.plurals.certify_keys_with_errors, mCertifyError, mCertifyError);
            }

        } else {
            duration = 0;
            style = Style.ERROR;
            str = activity.getResources().getQuantityString(R.plurals.certify_error,
                    mCertifyError, mCertifyError);
        }

        return Notify.create(activity, str, duration, style, new ActionListener() {
            @Override
            public void onAction() {
                Intent intent = new Intent(
                        activity, LogDisplayActivity.class);
                intent.putExtra(LogDisplayFragment.EXTRA_RESULT, CertifyResult.this);
                activity.startActivity(intent);
            }
        }, R.string.snackbar_details);

    }

}

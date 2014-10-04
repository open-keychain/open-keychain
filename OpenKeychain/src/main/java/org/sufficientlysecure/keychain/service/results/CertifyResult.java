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
import com.github.johnpersano.supertoasts.SuperToast.Duration;
import com.github.johnpersano.supertoasts.util.OnClickWrapper;
import com.github.johnpersano.supertoasts.util.Style;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.LogDisplayActivity;
import org.sufficientlysecure.keychain.ui.LogDisplayFragment;

public class CertifyResult extends OperationResult {

    int mCertifyOk, mCertifyError;

    public CertifyResult(int result, OperationLog log) {
        super(result, log);
    }

    public CertifyResult(int result, OperationLog log, int certifyOk, int certifyError) {
        this(result, log);
        mCertifyOk = certifyOk;
        mCertifyError = certifyError;
    }

    /** Construct from a parcel - trivial because we have no extra data. */
    public CertifyResult(Parcel source) {
        super(source);
        mCertifyOk = source.readInt();
        mCertifyError = source.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeInt(mCertifyOk);
        dest.writeInt(mCertifyError);
    }

    public static Creator<CertifyResult> CREATOR = new Creator<CertifyResult>() {
        public CertifyResult createFromParcel(final Parcel source) {
            return new CertifyResult(source);
        }

        public CertifyResult[] newArray(final int size) {
            return new CertifyResult[size];
        }
    };

    public SuperCardToast createNotify(final Activity activity) {

        int resultType = getResult();

        String str;
        int duration, color;

        // Not an overall failure
        if ((resultType & OperationResult.RESULT_ERROR) == 0) {
            String withWarnings;

            duration = Duration.EXTRA_LONG;
            color = Style.GREEN;
            withWarnings = "";

            // Any warnings?
            if ((resultType & ImportKeyResult.RESULT_WARNINGS) > 0) {
                duration = 0;
                color = Style.ORANGE;
                withWarnings += activity.getString(R.string.with_warnings);
            }
            if ((resultType & ImportKeyResult.RESULT_CANCELLED) > 0) {
                duration = 0;
                color = Style.ORANGE;
                withWarnings += activity.getString(R.string.with_cancelled);
            }

            // New and updated keys
            str = activity.getResources().getQuantityString(
                    R.plurals.certify_keys_ok, mCertifyOk, mCertifyOk, withWarnings);
            if (mCertifyError > 0) {
                // definitely switch to warning-style message in this case!
                duration = 0;
                color = Style.RED;
                str += " " + activity.getResources().getQuantityString(
                        R.plurals.certify_keys_with_errors, mCertifyError, mCertifyError);
            }

        } else {
            duration = 0;
            color = Style.RED;
            str = activity.getResources().getQuantityString(R.plurals.certify_error,
                    mCertifyError, mCertifyError);
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
                            intent.putExtra(LogDisplayFragment.EXTRA_RESULT, CertifyResult.this);
                            activity.startActivity(intent);
                        }
                    }
            ));
        }

        return toast;

    }

}

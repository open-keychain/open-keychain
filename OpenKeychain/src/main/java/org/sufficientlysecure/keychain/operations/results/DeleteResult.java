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

package org.sufficientlysecure.keychain.operations.results;

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

public class DeleteResult extends OperationResult {

    final public int mOk, mFail;

    public DeleteResult(int result, OperationLog log, int ok, int fail) {
        super(result, log);
        mOk = ok;
        mFail = fail;
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

    public SuperCardToast createNotify(final Activity activity) {

        int resultType = getResult();

        String str;
        int duration, color;

        // Not an overall failure
        if ((resultType & OperationResult.RESULT_ERROR) == 0) {
            String untilCancelled;

            duration = Duration.EXTRA_LONG;
            color = Style.GREEN;
            untilCancelled = "";

            // Any warnings?
            if ((resultType & ImportKeyResult.RESULT_CANCELLED) > 0) {
                duration = 0;
                color = Style.ORANGE;
                untilCancelled += activity.getString(R.string.with_cancelled);
            }

            // New and updated keys
            if (mOk > 0 && mFail > 0) {
                color = Style.ORANGE;
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
                color = Style.RED;
                str = "internal error";
            }

        } else {
            duration = 0;
            color = Style.RED;
            if (mFail == 0) {
                str = activity.getString(R.string.delete_nothing);
            } else {
                str = activity.getResources().getQuantityString(R.plurals.delete_fail, mFail);
            }
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
                            intent.putExtra(LogDisplayFragment.EXTRA_RESULT, DeleteResult.this);
                            activity.startActivity(intent);
                        }
                    }
            ));
        }

        return toast;

    }

}

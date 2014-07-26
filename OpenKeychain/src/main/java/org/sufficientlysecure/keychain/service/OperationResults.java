package org.sufficientlysecure.keychain.service;

import android.app.Activity;
import android.content.Intent;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;

import com.github.johnpersano.supertoasts.SuperCardToast;
import com.github.johnpersano.supertoasts.SuperToast;
import com.github.johnpersano.supertoasts.util.OnClickWrapper;
import com.github.johnpersano.supertoasts.util.Style;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.ui.LogDisplayActivity;
import org.sufficientlysecure.keychain.ui.LogDisplayFragment;

public abstract class OperationResults {

    public static class ImportKeyResult extends OperationResultParcel {

        public final int mNewKeys, mUpdatedKeys, mBadKeys;

        // At least one new key
        public static final int RESULT_OK_NEWKEYS = 2;
        // At least one updated key
        public static final int RESULT_OK_UPDATED = 4;
        // At least one key failed (might still be an overall success)
        public static final int RESULT_WITH_ERRORS = 8;
        // There are warnings in the log
        public static final int RESULT_WITH_WARNINGS = 16;

        // No keys to import...
        public static final int RESULT_FAIL_NOTHING = 32 +1;

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
        public boolean isFailNothing() {
            return (mResult & RESULT_FAIL_NOTHING) == RESULT_FAIL_NOTHING;
        }

        public ImportKeyResult(Parcel source) {
            super(source);
            mNewKeys = source.readInt();
            mUpdatedKeys = source.readInt();
            mBadKeys = source.readInt();
        }

        public ImportKeyResult(int result, OperationLog log,
                               int newKeys, int updatedKeys, int badKeys) {
            super(result, log);
            mNewKeys = newKeys;
            mUpdatedKeys = updatedKeys;
            mBadKeys = badKeys;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mNewKeys);
            dest.writeInt(mUpdatedKeys);
            dest.writeInt(mBadKeys);
        }

        public static Creator<ImportKeyResult> CREATOR = new Creator<ImportKeyResult>() {
            public ImportKeyResult createFromParcel(final Parcel source) {
                return new ImportKeyResult(source);
            }

            public ImportKeyResult[] newArray(final int size) {
                return new ImportKeyResult[size];
            }
        };

        public SuperCardToast createNotify(final Activity activity) {

            int resultType = getResult();

            String str;
            int duration, color;

            // Not an overall failure
            if ((resultType & OperationResultParcel.RESULT_ERROR) == 0) {
                String withWarnings;

                // Any warnings?
                if ((resultType & ImportKeyResult.RESULT_WITH_WARNINGS) > 0) {
                    duration = 0;
                    color = Style.ORANGE;
                    withWarnings = activity.getResources().getString(R.string.import_with_warnings);
                } else {
                    duration = SuperToast.Duration.LONG;
                    color = Style.GREEN;
                    withWarnings = "";
                }

                // New and updated keys
                if (this.isOkBoth()) {
                    str = activity.getResources().getQuantityString(
                            R.plurals.import_keys_added_and_updated_1, mNewKeys, mNewKeys);
                    str += activity.getResources().getQuantityString(
                            R.plurals.import_keys_added_and_updated_2, mUpdatedKeys, mUpdatedKeys, withWarnings);
                } else if (isOkUpdated()) {
                    str = activity.getResources().getQuantityString(
                            R.plurals.import_keys_updated, mUpdatedKeys, mUpdatedKeys, withWarnings);
                } else if (isOkNew()) {
                    str = activity.getResources().getQuantityString(
                            R.plurals.import_keys_added, mNewKeys, mNewKeys, withWarnings);
                } else {
                    duration = 0;
                    color = Style.RED;
                    str = "internal error";
                }

            } else {
                duration = 0;
                color = Style.RED;
                if (isFailNothing()) {
                    str = activity.getString(R.string.import_error_nothing);
                } else {
                    str = activity.getString(R.string.import_error);
                }
            }

            // TODO: externalize into Notify class?
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
                        activity.getResources().getString(R.string.import_view_log));
                toast.setButtonTextColor(activity.getResources().getColor(R.color.black));
                toast.setTextColor(activity.getResources().getColor(R.color.black));
                toast.setOnClickWrapper(new OnClickWrapper("supercardtoast",
                        new SuperToast.OnClickListener() {
                            @Override
                            public void onClick(View view, Parcelable token) {
                                Intent intent = new Intent(
                                        activity, LogDisplayActivity.class);
                                intent.putExtra(LogDisplayFragment.EXTRA_RESULT, ImportKeyResult.this);
                                activity.startActivity(intent);
                            }
                        }
                ));
            }

            return toast;

        }

    }

    public static class EditKeyResult extends OperationResultParcel {

        public EditKeyResult(Parcel source) {
            super(source);
        }

        public static Creator<EditKeyResult> CREATOR = new Creator<EditKeyResult>() {
            public EditKeyResult createFromParcel(final Parcel source) {
                return new EditKeyResult(source);
            }

            public EditKeyResult[] newArray(final int size) {
                return new EditKeyResult[size];
            }
        };

    }


    public static class SaveKeyringResult extends OperationResultParcel {

        public SaveKeyringResult(int result, OperationLog log) {
            super(result, log);
        }

        // Some old key was updated
        public static final int UPDATED = 2;

        // Public key was saved
        public static final int SAVED_PUBLIC = 8;
        // Secret key was saved (not exclusive with public!)
        public static final int SAVED_SECRET = 16;

        public boolean updated() {
            return (mResult & UPDATED) == UPDATED;
        }

    }

}

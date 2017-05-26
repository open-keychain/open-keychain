package org.sufficientlysecure.keychain.ui.keyview.presenter;


import android.content.Intent;
import android.support.v4.app.DialogFragment;


public interface ViewKeyMvpView {
    void startActivityAndShowResultSnackbar(Intent intent);
    void showDialogFragment(DialogFragment dialogFragment, final String tag);
    void setContentShown(boolean show, boolean animate);
}

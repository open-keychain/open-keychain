package org.sufficientlysecure.keychain.ui.keyview.presenter;


import android.content.Intent;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.View;


public interface ViewKeyMvpView {
    void switchToFragment(Fragment frag, String backStackName);

    void startActivity(Intent intent);
    void startActivityAndShowResultSnackbar(Intent intent);
    void showDialogFragment(DialogFragment dialogFragment, final String tag);
    void setContentShown(boolean show, boolean animate);

    void showContextMenu(int position, View anchor);
}

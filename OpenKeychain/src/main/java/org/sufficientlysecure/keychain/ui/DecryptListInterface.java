package org.sufficientlysecure.keychain.ui;

import java.util.ArrayList;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import org.sufficientlysecure.keychain.operations.results.InputDataResult;


public interface DecryptListInterface {

    // Lifecycle handlers
    void attachView(DecryptListView decryptListFragmentView);
    void initialize(ArrayList<Uri> inputUris, boolean canDelete, Bundle savedInstanceState);
    void detachView();

    // Android passthrough of some events
    void onSaveInstanceState(Bundle outState);
    boolean onActivityResult(int requestCode, int resultCode, Intent data);
    void onRequestPermissionsResult(@NonNull String[] permissions, @NonNull int[] grantResults);

    // Handlers for a bunch of UI events
    void onUiFileSelected(Uri uri);
    void onUiLongClickFile(int itemPosition, int fileIdx);
    void onUiClickFileContextOpen(int itemPosition, int fileIdx);
    void onUiClickFileContextShare(int itemPosition, int fileIdx);
    void onUiClickFileContextSave(int itemPosition, int fileIdx);
    void onUiClickFile(int itemPosition, int fileIdx);
    void onUiClickSignature(int itemPosition);
    void onUiContextViewLog(int itemPosition);
    void onUiContextDelete(int itemPosition);
    void onUiClickRetry(int itemPosition);

    /** Interface used by a DecryptListInterface to communicate with the View. */
    interface DecryptListView {

        // Adds another decrypt item to the view.
        void addItem();

        // Methods to update the view of available items. Indexed in the order they were added by addItem!
        void setInputDataResult(int pos, InputDataResult result);
        void setProgress(int pos, int progress, int max, String msg);
        void setCancelled(int pos, boolean isCancelled);
        void setProcessingKeyLookup(int pos, boolean processingKeyLookup);
        void resetItemData(int pos);

        // Methods to display modal dialogues on top of data
        void saveDocumentDialog(String filename, String mimeType);
        void displayFileContextMenu(int pos, int index);

        // Setting whether the "delete" option should be shown in file context menu
        void setShowDeleteFile(boolean canDeleteFiles);

        // Android passthrough: asks to issue a request for the passed permissions
        void requestPermissions(String[] strings);

        // simple access to an icon cache, this will supply the view with drawable for the views
        boolean iconCacheContainsKey(Uri cacheUri);
        void iconCachePut(Uri cacheUri, Drawable icon);

        // Android passthrough: access the underlying fragment and activity
        FragmentActivity getActivity();
        Fragment getFragment();

    }
}

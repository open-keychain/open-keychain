package org.sufficientlysecure.keychain.ui;


import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.LabeledIntent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.InputDataResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.InputDataParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableHashMap;
import org.sufficientlysecure.keychain.util.Preferences;


public class DecryptListPresenter
        implements DecryptListInterface, CryptoOperationHelper.Callback<InputDataParcel,InputDataResult> {

    private ArrayList<Uri> mInputUris;
    private HashMap<Uri, InputDataResult> mInputDataResults;
    private ArrayList<Uri> mPendingInputUris;
    private ArrayList<Uri> mCancelledInputUris;

    private Uri mCurrentInputUri;
    private boolean mCanDelete;
    private Uri mCurrentSaveFileUri;
    private CryptoOperationHelper<InputDataParcel, InputDataResult> mCryptoHelper;

    public void attachView(DecryptListView decryptListFragmentView) {
        mCryptoHelper = new CryptoOperationHelper<>(0, decryptListFragmentView.getFragment(), this, null);
        mDecryptListView = decryptListFragmentView;
    }

    public void detachView() {
        mDecryptListView = null;
        mCryptoHelper.cancelAllOperations();
        mCryptoHelper = null;
    }

    DecryptListView mDecryptListView;

    public static final String ARG_OUTPUT_URIS = "output_uris";
    public static final String ARG_CANCELLED_URIS = "cancelled_uris";
    public static final String ARG_RESULTS = "results";
    public static final String ARG_CAN_DELETE = "can_delete";

    public void initialize(ArrayList<Uri> inputUris, boolean canDelete, Bundle savedInstanceState) {

        mInputUris = inputUris;
        mPendingInputUris = new ArrayList<>();

        if (savedInstanceState != null) {
            ParcelableHashMap<Uri, InputDataResult> parceledResultMap = savedInstanceState.getParcelable(ARG_RESULTS);
            if (parceledResultMap != null) {
                mInputDataResults = parceledResultMap.getMap();
            }
            mCancelledInputUris = savedInstanceState.getParcelableArrayList(ARG_CANCELLED_URIS);
            mCanDelete = savedInstanceState.getBoolean(ARG_CAN_DELETE);
        } else {
            mCancelledInputUris = new ArrayList<>();
            mCanDelete = canDelete;
        }

        if (mInputDataResults == null) {
            mInputDataResults = new HashMap<>(mInputUris.size());
        }

        for (final Uri uri : inputUris) {
            mDecryptListView.addItem();

            boolean uriIsCancelled = mCancelledInputUris.contains(uri);
            if (uriIsCancelled) {
                mDecryptListView.setCancelled(mInputUris.indexOf(uri), true);
                continue;
            }

            boolean uriHasResult = mInputDataResults.containsKey(uri);
            if (uriHasResult) {
                processResult(uri);
                continue;
            }

            // can't actually happen at the moment, but we *might* want to restore this field in the future? not sure.
            boolean uriIsCurrent = uri.equals(mCurrentInputUri);
            if (uriIsCurrent) {
                continue;
            }

            mPendingInputUris.add(uri);
        }

        mDecryptListView.setShowDeleteFile(mCanDelete);

        cryptoOperation();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        HashMap<Uri,InputDataResult> results = new HashMap<>(mInputUris.size());
        for (Uri uri : mInputUris) {
            if (mPendingInputUris.contains(uri)) {
                continue;
            }
            InputDataResult result = mInputDataResults.get(uri);
            if (result != null) {
                results.put(uri, result);
            }
        }

        outState.putParcelable(ARG_RESULTS, new ParcelableHashMap<>(results));
        outState.putParcelable(ARG_OUTPUT_URIS, new ParcelableHashMap<>(mInputDataResults));
        outState.putParcelableArrayList(ARG_CANCELLED_URIS, mCancelledInputUris);
        outState.putBoolean(ARG_CAN_DELETE, mCanDelete);

    }

    @Override
    public void onUiFileSelected(Uri uri) {
        saveFile(uri);
        mCurrentInputUri = null;
    }

    @TargetApi(VERSION_CODES.KITKAT)
    private void saveFileDialog(InputDataResult result, int index) {

        Activity activity = mDecryptListView.getActivity();
        if (activity == null) {
            return;
        }

        OpenPgpMetadata metadata = result.mMetadata.get(index);
        mCurrentSaveFileUri = result.getOutputUris().get(index);

        String filename = metadata.getFilename();
        if (TextUtils.isEmpty(filename)) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(metadata.getMimeType());
            filename = "decrypted" + (ext != null ? "."+ext : "");
        }

        mDecryptListView.saveDocumentDialog(filename, metadata.getMimeType());
    }

    private void saveFile(Uri saveUri) {
        if (mCurrentSaveFileUri == null) {
            return;
        }

        Uri decryptedFileUri = mCurrentSaveFileUri;
        mCurrentInputUri = null;

        Activity activity = mDecryptListView.getActivity();
        if (activity == null) {
            return;
        }

        try {
            FileHelper.copyUriData(activity, decryptedFileUri, saveUri);
            Notify.create(activity, R.string.file_saved, Style.OK).show();
        } catch (IOException e) {
            Log.e(Constants.TAG, "error saving file", e);
            Notify.create(activity, R.string.error_saving_file, Style.ERROR).show();
        }
    }

    private void cryptoOperation() {
        mCryptoHelper.cryptoOperation();
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        return mCryptoHelper.handleActivityResult(requestCode, resultCode, data);
    }

    @Override
    public InputDataParcel createOperationInput() {

        Activity activity = mDecryptListView.getActivity();
        if (activity == null) {
            return null;
        }

        if (mCurrentInputUri == null) {
            if (mPendingInputUris.isEmpty()) {
                // nothing left to do
                return null;
            }

            mCurrentInputUri = mPendingInputUris.remove(0);
        }

        Log.d(Constants.TAG, "mCurrentInputUri=" + mCurrentInputUri);

        if ( ! checkAndRequestReadPermission(activity, mCurrentInputUri)) {
            return null;
        }

        PgpDecryptVerifyInputParcel decryptInput = new PgpDecryptVerifyInputParcel()
                .setAllowSymmetricDecryption(true);
        return new InputDataParcel(mCurrentInputUri, decryptInput);

    }

    @Override
    public void onCryptoOperationSuccess(InputDataResult result) {
        if (mCurrentInputUri == null) {
            return;
        }

        Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        boolean isSingleInput = mInputDataResults.isEmpty() && mPendingInputUris.isEmpty();
        if ( !Constants.DEBUG && isSingleInput) {

            Activity activity = mDecryptListView.getActivity();

            // there is always at least one mMetadata object, so we know this is >= 1 already
            boolean isSingleMetadata = result.mMetadata.size() == 1;
            OpenPgpMetadata metadata = result.mMetadata.get(0);
            boolean isText = "text/plain".equals(metadata.getMimeType());
            boolean isOverSized = metadata.getOriginalSize() > Constants.TEXT_LENGTH_LIMIT;

            if (isSingleMetadata && isText && !isOverSized) {
                Intent displayTextIntent = new Intent(activity, DisplayTextActivity.class)
                        .setDataAndType(result.mOutputUris.get(0), "text/plain")
                        .putExtra(DisplayTextActivity.EXTRA_RESULT, result.mDecryptVerifyResult)
                        .putExtra(DisplayTextActivity.EXTRA_METADATA, metadata);
                activity.startActivity(displayTextIntent);
                activity.finish();
                return;
            }

        }

        mInputDataResults.put(uri, result);
        processResult(uri);

        cryptoOperation();
    }

    @Override
    public void onCryptoOperationCancelled() {
        if (mCurrentInputUri == null) {
            return;
        }

        final Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        mCancelledInputUris.add(uri);
        mDecryptListView.setCancelled(mInputUris.indexOf(uri), true);

        cryptoOperation();

    }

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        if (mCurrentInputUri == null) {
            return true;
        }

        mDecryptListView.setProgress(mInputUris.indexOf(mCurrentInputUri), progress, max, msg);
        return true;
    }

    @Override
    public void onCryptoOperationError(InputDataResult result) {
        if (mCurrentInputUri == null) {
            return;
        }

        final Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        Activity activity = mDecryptListView.getActivity();
        if (activity != null && "com.fsck.k9.attachmentprovider".equals(uri.getHost())) {
            Toast.makeText(activity, R.string.error_reading_k9, Toast.LENGTH_LONG).show();
        }

        mDecryptListView.setInputDataResult(mInputUris.indexOf(uri), result);

        cryptoOperation();
    }

    private void processResult(final Uri uri) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                InputDataResult result = mInputDataResults.get(uri);

                Context context = mDecryptListView.getActivity();
                if (context == null) {
                    return null;
                }

                for (int i = 0; i < result.getOutputUris().size(); i++) {

                    Uri outputUri = result.getOutputUris().get(i);
                    if (mDecryptListView.iconCacheContainsKey(outputUri)) {
                        continue;
                    }

                    OpenPgpMetadata metadata = result.mMetadata.get(i);
                    String type = metadata.getMimeType();

                    Drawable icon = null;

                    if (ClipDescription.compareMimeTypes(type, "text/plain")) {
                        // noinspection deprecation, this should be called from Context, but not available in minSdk
                        icon = context.getResources().getDrawable(R.drawable.ic_chat_black_24dp);
                    } else if (ClipDescription.compareMimeTypes(type, "application/octet-stream")) {
                        // icons for this are just confusing
                        // noinspection deprecation, this should be called from Context, but not available in minSdk
                        icon = context.getResources().getDrawable(R.drawable.ic_doc_generic_am);
                    } else if (ClipDescription.compareMimeTypes(type, Constants.MIME_TYPE_KEYS)) {
                        // noinspection deprecation, this should be called from Context, but not available in minSdk
                        icon = context.getResources().getDrawable(R.drawable.ic_key_plus_grey600_24dp);
                    } else if (ClipDescription.compareMimeTypes(type, "image/*")) {
                        int px = FormattingUtils.dpToPx(context, 32);
                        Bitmap bitmap = FileHelper.getThumbnail(context, outputUri, new Point(px, px));
                        icon = new BitmapDrawable(context.getResources(), bitmap);
                    }

                    if (icon == null) {
                        final Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(outputUri, type);

                        final List<ResolveInfo> matches =
                                context.getPackageManager().queryIntentActivities(intent, 0);
                        // noinspection LoopStatementThatDoesntLoop
                        for (ResolveInfo match : matches) {
                            icon = match.loadIcon(mDecryptListView.getActivity().getPackageManager());
                            break;
                        }
                    }

                    if (icon != null) {
                        mDecryptListView.iconCachePut(outputUri, icon);
                    }

                }

                return null;

            }

            @Override
            protected void onPostExecute(Void v) {
                InputDataResult result = mInputDataResults.get(uri);
                mDecryptListView.setInputDataResult(mInputUris.indexOf(uri), result);
            }
        }.execute();

    }

    private void retryUri(Uri uri) {

        // never interrupt running operations!
        if (mCurrentInputUri != null) {
            return;
        }

        // un-cancel this one
        mCancelledInputUris.remove(uri);
        mInputDataResults.remove(uri);
        mPendingInputUris.add(uri);
        mDecryptListView.resetItemData(mInputUris.indexOf(uri));

        // check if there are any pending input uris
        cryptoOperation();

    }

    private void displayWithViewIntent(int itemPosition, int index, boolean share, boolean forceChooser) {
        Activity activity = mDecryptListView.getActivity();
        if (activity == null) {
            return;
        }

        Uri inputUri = mInputUris.get(itemPosition);
        InputDataResult result = mInputDataResults.get(inputUri);

        Uri outputUri = result.getOutputUris().get(index);
        OpenPgpMetadata metadata = result.mMetadata.get(index);

        // text/plain is a special case where we extract the uri content into
        // the EXTRA_TEXT extra ourselves, and display a chooser which includes
        // OpenKeychain's internal viewer
        if ("text/plain".equals(metadata.getMimeType())) {

            if (share) {
                try {
                    String plaintext = FileHelper.readTextFromUri(activity, outputUri, null);

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, plaintext);

                    Intent chooserIntent = Intent.createChooser(intent, activity.getString(R.string.intent_share));
                    activity.startActivity(chooserIntent);

                } catch (IOException e) {
                    Notify.create(activity, R.string.error_preparing_data, Style.ERROR).show();
                }

                return;
            }

            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(outputUri, "text/plain");

            if (forceChooser) {

                LabeledIntent internalIntent = new LabeledIntent(
                        new Intent(intent)
                                .setClass(activity, DisplayTextActivity.class)
                                .putExtra(DisplayTextActivity.EXTRA_RESULT, result.mDecryptVerifyResult)
                                .putExtra(DisplayTextActivity.EXTRA_METADATA, metadata),
                        BuildConfig.APPLICATION_ID, R.string.view_internal, R.mipmap.ic_launcher);

                Intent chooserIntent = Intent.createChooser(intent, activity.getString(R.string.intent_show));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                        new Parcelable[] { internalIntent });

                activity.startActivity(chooserIntent);

            } else {

                intent.setClass(activity, DisplayTextActivity.class);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(DisplayTextActivity.EXTRA_RESULT, result.mDecryptVerifyResult);
                intent.putExtra(DisplayTextActivity.EXTRA_METADATA, metadata);
                activity.startActivity(intent);

            }

        } else {

            Intent intent;
            if (share) {
                intent = new Intent(Intent.ACTION_SEND);
                intent.setType(metadata.getMimeType());
                intent.putExtra(Intent.EXTRA_STREAM, outputUri);
            } else {
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(outputUri, metadata.getMimeType());

                if (!forceChooser && Constants.MIME_TYPE_KEYS.equals(metadata.getMimeType())) {
                    // bind Intent to this OpenKeychain, don't allow other apps to intercept here!
                    intent.setPackage(activity.getPackageName());
                }
            }

            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooserIntent = Intent.createChooser(intent, activity.getString(R.string.intent_show));
            chooserIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(chooserIntent);
        }

    }

    /**
     * Request READ_EXTERNAL_STORAGE permission on Android >= 6.0 to read content from "file" Uris.
     *
     * This method returns true on Android < 6, or if permission is already granted. It
     * requests the permission and returns false otherwise, taking over responsibility
     * for mCurrentInputUri.
     *
     * see https://commonsware.com/blog/2015/10/07/runtime-permissions-files-action-send.html
     */
    private boolean checkAndRequestReadPermission(Activity activity, final Uri uri) {
        if ( ! "file".equals(uri.getScheme())) {
            return true;
        }

        if (Build.VERSION.SDK_INT < VERSION_CODES.M) {
            return true;
        }

        // Additional check due to https://commonsware.com/blog/2015/11/09/you-cannot-hold-nonexistent-permissions.html
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        mDecryptListView.requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE });

        return false;

    }

    private void lookupUnknownKey(final Uri inputUri, long unknownKeyId) {

        final ArrayList<ParcelableKeyRing> keyList;
        final String keyserver;

        // search config
        {
            Preferences prefs = Preferences.getPreferences(mDecryptListView.getActivity());
            Preferences.CloudSearchPrefs cloudPrefs =
                    new Preferences.CloudSearchPrefs(true, true, prefs.getPreferredKeyserver());
            keyserver = cloudPrefs.keyserver;
        }

        {
            ParcelableKeyRing keyEntry = new ParcelableKeyRing(null,
                    KeyFormattingUtils.convertKeyIdToHex(unknownKeyId), null);
            ArrayList<ParcelableKeyRing> selectedEntries = new ArrayList<>();
            selectedEntries.add(keyEntry);

            keyList = selectedEntries;
        }

        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> callback
                = new CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult>() {

            @Override
            public ImportKeyringParcel createOperationInput() {
                return new ImportKeyringParcel(keyList, keyserver);
            }

            @Override
            public void onCryptoOperationSuccess(ImportKeyResult result) {
                retryUri(inputUri);
            }

            @Override
            public void onCryptoOperationCancelled() {
                mDecryptListView.setProcessingKeyLookup(mInputUris.indexOf(inputUri), false);
            }

            @Override
            public void onCryptoOperationError(ImportKeyResult result) {
                result.createNotify(mDecryptListView.getActivity()).show();
                mDecryptListView.setProcessingKeyLookup(mInputUris.indexOf(inputUri), false);
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        mDecryptListView.setProcessingKeyLookup(mInputUris.indexOf(inputUri), true);

        CryptoOperationHelper importOpHelper = new CryptoOperationHelper<>(
                2, mDecryptListView.getActivity(), callback, null);
        importOpHelper.cryptoOperation();

    }


    @Override
    public void onRequestPermissionsResult(
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        boolean permissionWasGranted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (permissionWasGranted) {

            // permission granted -> retry all cancelled file uris
            Iterator<Uri> it = mCancelledInputUris.iterator();
            while (it.hasNext()) {
                Uri uri = it.next();
                if ( ! "file".equals(uri.getScheme())) {
                    continue;
                }
                it.remove();
                mPendingInputUris.add(uri);
                mDecryptListView.setCancelled(mInputUris.indexOf(uri), false);
            }

        } else {

            // permission denied -> cancel current, and all pending file uris
            mCancelledInputUris.add(mCurrentInputUri);
            mDecryptListView.setCancelled(mInputUris.indexOf(mCurrentInputUri), true);

            mCurrentInputUri = null;
            Iterator<Uri> it = mPendingInputUris.iterator();
            while (it.hasNext()) {
                Uri uri = it.next();
                if ( ! "file".equals(uri.getScheme())) {
                    continue;
                }
                it.remove();
                mCancelledInputUris.add(uri);
                mDecryptListView.setCancelled(mInputUris.indexOf(uri), true);
            }

        }

        // hand control flow back
        cryptoOperation();

    }

    @Override
    public void onUiClickFile(int itemPosition, int fileIdx) {
        displayWithViewIntent(itemPosition, fileIdx, false, false);
    }

    @Override
    public void onUiLongClickFile(int itemPosition, int fileIdx) {
        mDecryptListView.displayFileContextMenu(itemPosition, fileIdx);
    }

    @Override
    public void onUiClickFileContextOpen(int itemPosition, int fileIdx) {
        displayWithViewIntent(itemPosition, fileIdx, false, true);
    }

    @Override
    public void onUiClickFileContextShare(int itemPosition, int fileIdx) {
        displayWithViewIntent(itemPosition, fileIdx, true, true);
    }

    @Override
    public void onUiClickFileContextSave(int itemPosition, int fileIdx) {
        Uri inputUri = mInputUris.get(itemPosition);
        InputDataResult result = mInputDataResults.get(inputUri);
        saveFileDialog(result, fileIdx);
    }

    @Override
    public void onUiClickSignature(int itemPosition) {
        Uri inputUri = mInputUris.get(itemPosition);
        InputDataResult result = mInputDataResults.get(inputUri);
        OpenPgpSignatureResult sigResult = result.mDecryptVerifyResult.getSignatureResult();

        final long keyId = sigResult.getKeyId();
        if (sigResult.getResult() != OpenPgpSignatureResult.RESULT_KEY_MISSING) {
            startViewKey(keyId);
        } else {
            lookupUnknownKey(inputUri, keyId);
        }

    }

    private void startViewKey(long keyId) {
        Activity activity = mDecryptListView.getActivity();

        Intent intent = new Intent(activity, ViewKeyActivity.class);
        intent.setData(KeyRings.buildUnifiedKeyRingUri(keyId));
        activity.startActivity(intent);
    }

    @Override
    public void onUiContextViewLog(int itemPosition) {

        Activity activity = mDecryptListView.getActivity();
        if (activity == null) {
            return;
        }

        Uri inputUri = mInputUris.get(itemPosition);
        InputDataResult result = mInputDataResults.get(inputUri);

        Intent intent = new Intent(activity, LogDisplayActivity.class);
        intent.putExtra(LogDisplayFragment.EXTRA_RESULT, result);
        activity.startActivity(intent);

    }

    @Override
    public void onUiContextDelete(int itemPosition) {

        Activity activity = mDecryptListView.getActivity();
        Uri uri = mInputUris.get(itemPosition);

        // we can only ever delete a file once, if we got this far either it's gone or it will never work
        mCanDelete = false;
        mDecryptListView.setShowDeleteFile(false);

        try {
            int deleted = FileHelper.deleteFileSecurely(activity, uri);
            if (deleted > 0) {
                Notify.create(activity, R.string.file_delete_ok, Style.OK).show();
            } else {
                Notify.create(activity, R.string.file_delete_none, Style.WARN).show();
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "exception deleting file", e);
            Notify.create(activity, R.string.file_delete_exception, Style.ERROR).show();
        }

    }

    @Override
    public void onUiClickRetry(int itemPosition) {
        Uri uri = mInputUris.get(itemPosition);
        retryUri(uri);
    }

}

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
import android.content.ContentResolver;
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
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewAnimator;

import com.cocosw.bottomsheet.BottomSheet;

import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.keyimport.HkpKeyserverAddress;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.InputDataResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.service.InputDataParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.base.QueueingCryptoOperationFragment;
// this import NEEDS to be above the ViewModel AND SubViewHolder one, or it won't compile! (as of 16.09.15)
import org.sufficientlysecure.keychain.ui.keyview.ViewKeyActivity;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.StatusHolder;
import org.sufficientlysecure.keychain.ui.DecryptListFragment.ViewHolder.SubViewHolder;
import org.sufficientlysecure.keychain.ui.DecryptListFragment.DecryptFilesAdapter.ViewModel;
import org.sufficientlysecure.keychain.ui.adapter.SpacesItemDecoration;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.ParcelableHashMap;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


/**
 * Displays a list of decrypted inputs.
 * <p/>
 * This class has a complex control flow to manage its input URIs. Each URI
 * which is in mInputUris is also in exactly one of mPendingInputUris,
 * mCancelledInputUris, mCurrentInputUri, or a key in mInputDataResults.
 * <p/>
 * Processing of URIs happens using a looping approach:
 * - There is always exactly one method running which works on mCurrentInputUri
 * - Processing starts in cryptoOperation(), which pops a new mCurrentInputUri
 * from the list of mPendingInputUris.
 * - Once a mCurrentInputUri is finished processing, it should be set to null and
 * control handed back to cryptoOperation()
 * - Control flow can move through asynchronous calls, and resume in callbacks
 * like onActivityResult() or onPermissionRequestResult().
 */
public class DecryptListFragment
        extends QueueingCryptoOperationFragment<InputDataParcel, InputDataResult>
        implements OnMenuItemClickListener {

    public static final String ARG_INPUT_URIS = "input_uris";
    public static final String ARG_OUTPUT_URIS = "output_uris";
    public static final String ARG_CANCELLED_URIS = "cancelled_uris";
    public static final String ARG_RESULTS = "results";
    public static final String ARG_CAN_DELETE = "can_delete";
    public static final String ARG_IS_AUTOCRYPT_SETUP = "is_autocrypt_setup";

    private static final int REQUEST_CODE_OUTPUT = 0x00007007;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 12;

    private ArrayList<Uri> mInputUris;
    private HashMap<Uri, InputDataResult> mInputDataResults;
    private ArrayList<Uri> mPendingInputUris;
    private ArrayList<Uri> mCancelledInputUris;

    private Uri mCurrentInputUri;
    private boolean mCanDelete;
    private boolean mIsAutocryptSetup;

    private DecryptFilesAdapter mAdapter;
    private Uri mCurrentSaveFileUri;

    /**
     * Creates new instance of this fragment
     */
    public static DecryptListFragment newInstance(@NonNull ArrayList<Uri> uris, boolean canDelete, boolean isAutocryptSetup) {
        DecryptListFragment frag = new DecryptListFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_INPUT_URIS, uris);
        args.putBoolean(ARG_CAN_DELETE, canDelete);
        args.putBoolean(ARG_IS_AUTOCRYPT_SETUP, isAutocryptSetup);
        frag.setArguments(args);

        return frag;
    }

    public DecryptListFragment() {
        super(null);
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_files_list_fragment, container, false);

        RecyclerView vFilesList = view.findViewById(R.id.decrypted_files_list);

        vFilesList.addItemDecoration(new SpacesItemDecoration(
                FormattingUtils.dpToPx(getActivity(), 4)));
        vFilesList.setHasFixedSize(true);
        vFilesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        vFilesList.setItemAnimator(new DefaultItemAnimator() {
            @Override
            public boolean canReuseUpdatedViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                return true;
            }
        });

        mAdapter = new DecryptFilesAdapter();
        vFilesList.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(ARG_INPUT_URIS, mInputUris);

        HashMap<Uri, InputDataResult> results = new HashMap<>(mInputUris.size());
        for (Uri uri : mInputUris) {
            if (mPendingInputUris.contains(uri)) {
                continue;
            }
            InputDataResult result = mAdapter.getItemResult(uri);
            if (result != null) {
                results.put(uri, result);
            }
        }

        outState.putParcelable(ARG_RESULTS, new ParcelableHashMap<>(results));
        outState.putParcelable(ARG_OUTPUT_URIS, new ParcelableHashMap<>(mInputDataResults));
        outState.putParcelableArrayList(ARG_CANCELLED_URIS, mCancelledInputUris);
        outState.putBoolean(ARG_CAN_DELETE, mCanDelete);
        outState.putBoolean(ARG_IS_AUTOCRYPT_SETUP, mIsAutocryptSetup);

        // this does not save mCurrentInputUri - if anything is being
        // processed at fragment recreation time, the operation in
        // progress will be lost!
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();

        ArrayList<Uri> inputUris = getArguments().getParcelableArrayList(ARG_INPUT_URIS);
        ArrayList<Uri> cancelledUris = args.getParcelableArrayList(ARG_CANCELLED_URIS);
        ParcelableHashMap<Uri, InputDataResult> results = args.getParcelable(ARG_RESULTS);

        mCanDelete = args.getBoolean(ARG_CAN_DELETE, false);
        mIsAutocryptSetup = args.getBoolean(ARG_IS_AUTOCRYPT_SETUP, false);

        displayInputUris(inputUris, cancelledUris,
                results != null ? results.getMap() : null
        );
    }

    private void displayInputUris(
            ArrayList<Uri> inputUris,
            ArrayList<Uri> cancelledUris,
            HashMap<Uri, InputDataResult> results) {

        mInputUris = inputUris;
        mCurrentInputUri = null;
        mInputDataResults = results != null ? results : new HashMap<Uri, InputDataResult>(inputUris.size());
        mCancelledInputUris = cancelledUris != null ? cancelledUris : new ArrayList<Uri>();

        mPendingInputUris = new ArrayList<>();

        for (final Uri uri : inputUris) {
            mAdapter.add(uri);

            boolean uriIsCancelled = mCancelledInputUris.contains(uri);
            if (uriIsCancelled) {
                mAdapter.setCancelled(uri, true);
                continue;
            }

            boolean uriHasResult = results != null && results.containsKey(uri);
            if (uriHasResult) {
                processResult(uri);
                continue;
            }

            mPendingInputUris.add(uri);
        }

        // check if there are any pending input uris
        cryptoOperation();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OUTPUT: {
                // This happens after output file was selected, so start our operation
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Uri saveUri = data.getData();
                    saveFile(saveUri);
                    mCurrentInputUri = null;
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @TargetApi(VERSION_CODES.KITKAT)
    private void saveFileDialog(InputDataResult result, int index) {

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        OpenPgpMetadata metadata = result.mMetadata.get(index);
        mCurrentSaveFileUri = result.getOutputUris().get(index);

        String filename = metadata.getFilename();
        if (TextUtils.isEmpty(filename)) {
            String ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(metadata.getMimeType());
            filename = "decrypted" + (ext != null ? "." + ext : "");
        }

        // requires >=kitkat
        FileHelper.saveDocument(this, filename, metadata.getMimeType(), REQUEST_CODE_OUTPUT);
    }

    private void saveFile(Uri saveUri) {
        if (mCurrentSaveFileUri == null) {
            return;
        }

        Uri decryptedFileUri = mCurrentSaveFileUri;
        mCurrentInputUri = null;

        hideKeyboard();

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        try {
            FileHelper.copyUriData(activity, decryptedFileUri, saveUri);
            Notify.create(activity, R.string.file_saved, Style.OK).show();
        } catch (IOException e) {
            Timber.e(e, "error saving file");
            Notify.create(activity, R.string.error_saving_file, Style.ERROR).show();
        }
    }

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        mAdapter.setProgress(mCurrentInputUri, progress, max, msg);
        return true;
    }

    @Override
    public void onQueuedOperationError(InputDataResult result) {
        final Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        Activity activity = getActivity();
        if (activity != null && "com.fsck.k9.attachmentprovider".equals(uri.getHost())) {
            Toast.makeText(getActivity(), R.string.error_reading_k9, Toast.LENGTH_LONG).show();
        }

        mAdapter.addResult(uri, result);

        cryptoOperation();
    }

    @Override
    public void onQueuedOperationSuccess(InputDataResult result) {
        Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        Activity activity = getActivity();

        boolean isSingleInput = mInputDataResults.isEmpty() && mPendingInputUris.isEmpty();
        if (isSingleInput) {

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
        super.onCryptoOperationCancelled();

        final Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        mCancelledInputUris.add(uri);
        mAdapter.setCancelled(uri, true);

        cryptoOperation();

    }

    HashMap<Uri, Drawable> mIconCache = new HashMap<>();

    private void processResult(final Uri uri) {

        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {

                InputDataResult result = mInputDataResults.get(uri);

                Context context = getActivity();
                if (context == null) {
                    return null;
                }

                for (int i = 0; i < result.getOutputUris().size(); i++) {

                    Uri outputUri = result.getOutputUris().get(i);
                    if (mIconCache.containsKey(outputUri)) {
                        continue;
                    }

                    OpenPgpMetadata metadata = result.mMetadata.get(i);
                    String type = metadata.getMimeType();

                    Drawable icon = null;

                    if (ClipDescription.compareMimeTypes(type, "text/plain")) {
                        // noinspection deprecation, this should be called from Context, but not available in minSdk
                        icon = getResources().getDrawable(R.drawable.ic_chat_black_24dp);
                    } else if (ClipDescription.compareMimeTypes(type, "application/octet-stream")) {
                        // icons for this are just confusing
                        // noinspection deprecation, this should be called from Context, but not available in minSdk
                        icon = getResources().getDrawable(R.drawable.ic_doc_generic_am);
                    } else if (ClipDescription.compareMimeTypes(type, Constants.MIME_TYPE_KEYS)) {
                        // noinspection deprecation, this should be called from Context, but not available in minSdk
                        icon = getResources().getDrawable(R.drawable.ic_key_plus_grey600_24dp);
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
                            icon = match.loadIcon(getActivity().getPackageManager());
                            break;
                        }
                    }

                    if (icon != null) {
                        mIconCache.put(outputUri, icon);
                    }

                }

                return null;

            }

            @Override
            protected void onPostExecute(Void v) {
                InputDataResult result = mInputDataResults.get(uri);
                mAdapter.addResult(uri, result);
            }
        }.execute();

    }

    public void retryUri(Uri uri) {

        // never interrupt running operations!
        if (mCurrentInputUri != null) {
            return;
        }

        // un-cancel this one
        mCancelledInputUris.remove(uri);
        mInputDataResults.remove(uri);
        mPendingInputUris.add(uri);
        mAdapter.resetItemData(uri);

        // check if there are any pending input uris
        cryptoOperation();
    }

    public void displayBottomSheet(final InputDataResult result, final int index) {

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        new BottomSheet.Builder(activity).sheet(R.menu.decrypt_bottom_sheet).listener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.decrypt_open:
                        displayWithViewIntent(result, index, false, true);
                        break;
                    case R.id.decrypt_share:
                        displayWithViewIntent(result, index, true, true);
                        break;
                    case R.id.decrypt_save:
                        // only inside the menu xml for Android >= 4.4
                        saveFileDialog(result, index);
                        break;
                }
                return false;
            }
        }).grid().show();

    }

    public void displayWithViewIntent(InputDataResult result, int index, boolean share, boolean forceChooser) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

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

                    Intent chooserIntent = Intent.createChooser(intent, getString(R.string.intent_share));
                    startActivity(chooserIntent);

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

                Intent chooserIntent = Intent.createChooser(intent, getString(R.string.intent_show));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                        new Parcelable[]{internalIntent});

                startActivity(chooserIntent);

            } else {

                intent.setClass(activity, DisplayTextActivity.class);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.putExtra(DisplayTextActivity.EXTRA_RESULT, result.mDecryptVerifyResult);
                intent.putExtra(DisplayTextActivity.EXTRA_METADATA, metadata);
                startActivity(intent);

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
                    intent.setPackage(getActivity().getPackageName());
                }
            }

            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooserIntent = Intent.createChooser(intent, getString(R.string.intent_show));
            chooserIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (!share && ClipDescription.compareMimeTypes(metadata.getMimeType(), "text/*")) {
                LabeledIntent internalIntent = new LabeledIntent(
                        new Intent(intent)
                                .setClass(activity, DisplayTextActivity.class)
                                .putExtra(DisplayTextActivity.EXTRA_RESULT, result.mDecryptVerifyResult)
                                .putExtra(DisplayTextActivity.EXTRA_METADATA, metadata),
                        BuildConfig.APPLICATION_ID, R.string.view_internal, R.mipmap.ic_launcher);
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                        new Parcelable[]{internalIntent});
            }

            startActivity(chooserIntent);
        }

    }

    @Override
    public InputDataParcel createOperationInput() {

        Activity activity = getActivity();
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

        Timber.d("mCurrentInputUri=" + mCurrentInputUri);

        if (!checkAndRequestReadPermission(activity, mCurrentInputUri)) {
            return null;
        }

        PgpDecryptVerifyInputParcel.Builder decryptInput = PgpDecryptVerifyInputParcel.builder()
                .setAllowSymmetricDecryption(true)
                .setAutocryptSetup(mIsAutocryptSetup);
        return InputDataParcel.createInputDataParcel(mCurrentInputUri, decryptInput.build());

    }



    /**
     * Request READ_EXTERNAL_STORAGE permission on Android >= 6.0 to read content from "file" Uris.
     * <p/>
     * This method returns true on Android < 6, or if permission is already granted. It
     * requests the permission and returns false otherwise, taking over responsibility
     * for mCurrentInputUri.
     * <p/>
     * see https://commonsware.com/blog/2015/10/07/runtime-permissions-files-action-send.html
     */
    private boolean checkAndRequestReadPermission(Activity activity, final Uri uri) {
        if (!ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return true;
        }

        // Additional check due to https://commonsware.com/blog/2015/11/09/you-cannot-hold-nonexistent-permissions.html
        if (Build.VERSION.SDK_INT < VERSION_CODES.M) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        requestPermissions(
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);

        return false;

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode != REQUEST_PERMISSION_READ_EXTERNAL_STORAGE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        boolean permissionWasGranted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (permissionWasGranted) {

            // permission granted -> retry all cancelled file uris
            Iterator<Uri> it = mCancelledInputUris.iterator();
            while (it.hasNext()) {
                Uri uri = it.next();
                if (!ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                    continue;
                }
                it.remove();
                mPendingInputUris.add(uri);
                mAdapter.setCancelled(uri, false);
            }

        } else {

            // permission denied -> cancel current, and all pending file uris
            mCancelledInputUris.add(mCurrentInputUri);
            mAdapter.setCancelled(mCurrentInputUri, true);

            mCurrentInputUri = null;
            Iterator<Uri> it = mPendingInputUris.iterator();
            while (it.hasNext()) {
                Uri uri = it.next();
                if (!ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
                    continue;
                }
                it.remove();
                mCancelledInputUris.add(uri);
                mAdapter.setCancelled(uri, true);
            }

        }

        // hand control flow back
        cryptoOperation();

    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (mAdapter.mMenuClickedModel == null || !mAdapter.mMenuClickedModel.hasResult()) {
            return false;
        }

        // don't process menu items until all items are done!
        if (!mPendingInputUris.isEmpty()) {
            return true;
        }

        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        ViewModel model = mAdapter.mMenuClickedModel;
        switch (menuItem.getItemId()) {
            case R.id.view_log:
                Intent intent = new Intent(activity, LogDisplayActivity.class);
                intent.putExtra(LogDisplayFragment.EXTRA_RESULT, model.mResult);
                activity.startActivity(intent);
                return true;
            case R.id.decrypt_delete:
                deleteFile(activity, model.mInputUri);
                return true;
        }
        return false;
    }

    private void lookupUnknownKey(final Uri inputUri, long unknownKeyId) {

        final ArrayList<ParcelableKeyRing> keyList;
        final HkpKeyserverAddress keyserver;

        // search config
        keyserver = Preferences.getPreferences(getActivity()).getPreferredKeyserver();

        {
            ParcelableKeyRing keyEntry = ParcelableKeyRing.createFromReference(null,
                    KeyFormattingUtils.convertKeyIdToHex(unknownKeyId), null, null);
            ArrayList<ParcelableKeyRing> selectedEntries = new ArrayList<>();
            selectedEntries.add(keyEntry);

            keyList = selectedEntries;
        }

        CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult> callback
                = new CryptoOperationHelper.Callback<ImportKeyringParcel, ImportKeyResult>() {

            @Override
            public ImportKeyringParcel createOperationInput() {
                return ImportKeyringParcel.createImportKeyringParcel(keyList, keyserver);
            }

            @Override
            public void onCryptoOperationSuccess(ImportKeyResult result) {
                retryUri(inputUri);
            }

            @Override
            public void onCryptoOperationCancelled() {
                mAdapter.setProcessingKeyLookup(inputUri, false);
            }

            @Override
            public void onCryptoOperationError(ImportKeyResult result) {
                result.createNotify(getActivity()).show();
                mAdapter.setProcessingKeyLookup(inputUri, false);
            }

            @Override
            public boolean onCryptoSetProgress(String msg, int progress, int max) {
                return false;
            }
        };

        mAdapter.setProcessingKeyLookup(inputUri, true);

        CryptoOperationHelper importOpHelper = new CryptoOperationHelper<>(2, this, callback, null);
        importOpHelper.cryptoOperation();

    }


    private void deleteFile(Activity activity, Uri uri) {

        // we can only ever delete a file once, if we got this far either it's gone or it will never work
        mCanDelete = false;

        try {
            int deleted = FileHelper.deleteFileSecurely(activity, uri);
            if (deleted > 0) {
                Notify.create(activity, R.string.file_delete_ok, Style.OK).show();
            } else {
                Notify.create(activity, R.string.file_delete_none, Style.WARN).show();
            }
        } catch (Exception e) {
            Timber.e(e, "exception deleting file");
            Notify.create(activity, R.string.file_delete_exception, Style.ERROR).show();
        }

    }

    public class DecryptFilesAdapter extends RecyclerView.Adapter<ViewHolder> {
        private ArrayList<ViewModel> mDataset;
        private ViewModel mMenuClickedModel;

        public class ViewModel {
            Uri mInputUri;
            InputDataResult mResult;

            int mProgress, mMax;
            String mProgressMsg;
            OnClickListener mCancelled;
            boolean mProcessingKeyLookup;

            ViewModel(Uri uri) {
                mInputUri = uri;
                mProgress = 0;
                mMax = 100;
                mCancelled = null;
            }

            void setResult(InputDataResult result) {
                mResult = result;
            }

            boolean hasResult() {
                return mResult != null;
            }

            void setCancelled(OnClickListener retryListener) {
                mCancelled = retryListener;
            }

            void setProgress(int progress, int max, String msg) {
                if (msg != null) {
                    mProgressMsg = msg;
                }
                mProgress = progress;
                mMax = max;
            }

            void setProcessingKeyLookup(boolean processingKeyLookup) {
                mProcessingKeyLookup = processingKeyLookup;
            }

            // Depends on inputUri only
            @Override
            public boolean equals(Object o) {
                if (this == o) {
                    return true;
                }
                if (o == null || getClass() != o.getClass()) {
                    return false;
                }
                ViewModel viewModel = (ViewModel) o;
                if (mInputUri == null) {
                    return viewModel.mInputUri == null;
                }
                return mInputUri.equals(viewModel.mInputUri);
            }

            // Depends on inputUri only
            @Override
            public int hashCode() {
                return mResult != null ? mResult.hashCode() : 0;
            }

            @Override
            public String toString() {
                return mResult.toString();
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public DecryptFilesAdapter() {
            mDataset = new ArrayList<>();
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //inflate your layout and pass it to view holder
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.decrypt_list_entry, parent, false);
            return new ViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final ViewModel model = mDataset.get(position);

            if (model.mCancelled != null) {
                bindItemCancelled(holder, model);
                return;
            }

            if (!model.hasResult()) {
                bindItemProgress(holder, model);
                return;
            }

            if (model.mResult.success()) {
                bindItemSuccess(holder, model);
            } else {
                bindItemFailure(holder, model);
            }

        }

        private void bindItemCancelled(ViewHolder holder, ViewModel model) {
            holder.vAnimator.setDisplayedChild(3);

            holder.vCancelledRetry.setOnClickListener(model.mCancelled);
        }

        private void bindItemProgress(ViewHolder holder, ViewModel model) {
            holder.vAnimator.setDisplayedChild(0);

            holder.vProgress.setProgress(model.mProgress);
            holder.vProgress.setMax(model.mMax);
            if (model.mProgressMsg != null) {
                holder.vProgressMsg.setText(model.mProgressMsg);
            }
        }

        private void bindItemSuccess(ViewHolder holder, final ViewModel model) {
            holder.vAnimator.setDisplayedChild(1);

            KeyFormattingUtils.setStatus(getResources(), holder,
                    model.mResult.mDecryptVerifyResult, model.mProcessingKeyLookup);

            int numFiles = model.mResult.getOutputUris().size();
            holder.resizeFileList(numFiles, LayoutInflater.from(getActivity()));
            for (int i = 0; i < numFiles; i++) {

                Uri outputUri = model.mResult.getOutputUris().get(i);
                OpenPgpMetadata metadata = model.mResult.mMetadata.get(i);
                SubViewHolder fileHolder = holder.mFileHolderList.get(i);

                String filename;
                if (metadata == null) {
                    filename = getString(R.string.filename_unknown);
                } else if (!TextUtils.isEmpty(metadata.getFilename())) {
                    filename = metadata.getFilename();
                } else if (ClipDescription.compareMimeTypes(metadata.getMimeType(), Constants.MIME_TYPE_KEYS)) {
                    filename = getString(R.string.filename_keys);
                } else if (ClipDescription.compareMimeTypes(metadata.getMimeType(), "text/plain")) {
                    filename = getString(R.string.filename_unknown_text);
                } else {
                    filename = getString(R.string.filename_unknown);
                }
                fileHolder.vFilename.setText(filename);

                long size = metadata == null ? 0 : metadata.getOriginalSize();
                if (size == -1 || size == 0) {
                    fileHolder.vFilesize.setText("");
                } else {
                    fileHolder.vFilesize.setText(FileHelper.readableFileSize(size));
                }

                if (mIconCache.containsKey(outputUri)) {
                    fileHolder.vThumbnail.setImageDrawable(mIconCache.get(outputUri));
                } else {
                    fileHolder.vThumbnail.setImageResource(R.drawable.ic_doc_generic_am);
                }

                // save index closure-style :)
                final int idx = i;

                fileHolder.vFile.setOnLongClickListener(new OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View view) {
                        if (model.mResult.success()) {
                            displayBottomSheet(model.mResult, idx);
                            return true;
                        }
                        return false;
                    }
                });

                fileHolder.vFile.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (model.mResult.success()) {
                            displayWithViewIntent(model.mResult, idx, false, false);
                        }
                    }
                });

            }

            OpenPgpSignatureResult sigResult = model.mResult.mDecryptVerifyResult.getSignatureResult();
            if (sigResult != null) {
                final long keyId = sigResult.getKeyId();
                if (sigResult.getResult() != OpenPgpSignatureResult.RESULT_KEY_MISSING) {
                    holder.vSignatureLayout.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Activity activity = getActivity();
                            if (activity == null) {
                                return;
                            }
                            Intent intent = new Intent(activity, ViewKeyActivity.class);
                            intent.setData(KeyRings.buildUnifiedKeyRingUri(keyId));
                            activity.startActivity(intent);
                        }
                    });
                } else {
                    holder.vSignatureLayout.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            lookupUnknownKey(model.mInputUri, keyId);
                        }
                    });
                }
            }

            holder.vContextMenu.setTag(model);
            holder.vContextMenu.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Activity activity = getActivity();
                    if (activity == null) {
                        return;
                    }
                    mMenuClickedModel = model;
                    PopupMenu menu = new PopupMenu(activity, view);
                    menu.inflate(R.menu.decrypt_item_context_menu);
                    menu.setOnMenuItemClickListener(DecryptListFragment.this);
                    menu.setOnDismissListener(new OnDismissListener() {
                        @Override
                        public void onDismiss(PopupMenu popupMenu) {
                            mMenuClickedModel = null;
                        }
                    });
                    menu.getMenu().findItem(R.id.decrypt_delete).setEnabled(mCanDelete);
                    menu.show();
                }
            });
        }

        private void bindItemFailure(ViewHolder holder, final ViewModel model) {
            holder.vAnimator.setDisplayedChild(2);

            holder.vErrorMsg.setText(model.mResult.getLog().getLast().mType.getMsgId());

            holder.vErrorViewLog.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Activity activity = getActivity();
                    if (activity == null) {
                        return;
                    }
                    Intent intent = new Intent(activity, LogDisplayActivity.class);
                    intent.putExtra(LogDisplayFragment.EXTRA_RESULT, model.mResult);
                    activity.startActivity(intent);
                }
            });

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }

        public InputDataResult getItemResult(Uri uri) {
            ViewModel model = new ViewModel(uri);
            int pos = mDataset.indexOf(model);
            if (pos == -1) {
                return null;
            }
            model = mDataset.get(pos);

            return model.mResult;
        }

        public void add(Uri uri) {
            ViewModel newModel = new ViewModel(uri);
            mDataset.add(newModel);
            notifyItemInserted(mDataset.size());
        }

        public void setProgress(Uri uri, int progress, int max, String msg) {
            ViewModel newModel = new ViewModel(uri);
            int pos = mDataset.indexOf(newModel);
            mDataset.get(pos).setProgress(progress, max, msg);
            notifyItemChanged(pos);
        }

        public void setCancelled(final Uri uri, boolean isCancelled) {
            ViewModel newModel = new ViewModel(uri);
            int pos = mDataset.indexOf(newModel);
            if (isCancelled) {
                mDataset.get(pos).setCancelled(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        retryUri(uri);
                    }
                });
            } else {
                mDataset.get(pos).setCancelled(null);
            }
            notifyItemChanged(pos);
        }

        public void setProcessingKeyLookup(Uri uri, boolean processingKeyLookup) {
            ViewModel newModel = new ViewModel(uri);
            int pos = mDataset.indexOf(newModel);
            mDataset.get(pos).setProcessingKeyLookup(processingKeyLookup);
            notifyItemChanged(pos);
        }

        public void addResult(Uri uri, InputDataResult result) {
            ViewModel model = new ViewModel(uri);
            int pos = mDataset.indexOf(model);
            model = mDataset.get(pos);
            model.setResult(result);
            notifyItemChanged(pos);
        }

        public void resetItemData(Uri uri) {
            ViewModel model = new ViewModel(uri);
            int pos = mDataset.indexOf(model);
            model = mDataset.get(pos);
            model.setResult(null);
            model.setCancelled(null);
            model.setProcessingKeyLookup(false);
            notifyItemChanged(pos);
        }

    }


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder implements StatusHolder {
        public ViewAnimator vAnimator;

        public ProgressBar vProgress;
        public TextView vProgressMsg;

        public ImageView vEncStatusIcon;
        public TextView vEncStatusText;

        public ImageView vSigStatusIcon;
        public TextView vSigStatusText;
        public View vSignatureLayout;
        public TextView vSignatureName;
        public TextView vSignatureMail;
        public ViewAnimator vSignatureAction;
        public View vContextMenu;

        public TextView vErrorMsg;
        public ImageView vErrorViewLog;

        public ImageView vCancelledRetry;

        public LinearLayout vFileList;

        public static class SubViewHolder {
            public View vFile;
            public TextView vFilename;
            public TextView vFilesize;
            public ImageView vThumbnail;

            public SubViewHolder(View itemView) {
                vFile = itemView.findViewById(R.id.file);
                vFilename = itemView.findViewById(R.id.filename);
                vFilesize = itemView.findViewById(R.id.filesize);
                vThumbnail = itemView.findViewById(R.id.thumbnail);
            }
        }

        public ArrayList<SubViewHolder> mFileHolderList = new ArrayList<>();
        private int mCurrentFileListSize = 0;

        public ViewHolder(View itemView) {
            super(itemView);

            vAnimator = itemView.findViewById(R.id.view_animator);

            vProgress = itemView.findViewById(R.id.progress);
            vProgressMsg = itemView.findViewById(R.id.progress_msg);

            vEncStatusIcon = itemView.findViewById(R.id.result_encryption_icon);
            vEncStatusText = itemView.findViewById(R.id.result_encryption_text);

            vSigStatusIcon = itemView.findViewById(R.id.result_signature_icon);
            vSigStatusText = itemView.findViewById(R.id.result_signature_text);
            vSignatureLayout = itemView.findViewById(R.id.result_signature_layout);
            vSignatureName = itemView.findViewById(R.id.result_signature_name);
            vSignatureMail = itemView.findViewById(R.id.result_signature_email);
            vSignatureAction = itemView.findViewById(R.id.result_signature_action);

            vFileList = itemView.findViewById(R.id.file_list);
            for (int i = 0; i < vFileList.getChildCount(); i++) {
                mFileHolderList.add(new SubViewHolder(vFileList.getChildAt(i)));
                mCurrentFileListSize += 1;
            }

            vContextMenu = itemView.findViewById(R.id.context_menu);

            vErrorMsg = itemView.findViewById(R.id.result_error_msg);
            vErrorViewLog = itemView.findViewById(R.id.result_error_log);

            vCancelledRetry = itemView.findViewById(R.id.cancel_retry);

        }

        public void resizeFileList(int size, LayoutInflater inflater) {
            int childCount = vFileList.getChildCount();
            // if we require more children, create them
            while (childCount < size) {
                View v = inflater.inflate(R.layout.decrypt_list_file_item, null);
                vFileList.addView(v);
                mFileHolderList.add(new SubViewHolder(v));
                childCount += 1;
            }

            while (size < mCurrentFileListSize) {
                mCurrentFileListSize -= 1;
                vFileList.getChildAt(mCurrentFileListSize).setVisibility(View.GONE);
            }
            while (size > mCurrentFileListSize) {
                vFileList.getChildAt(mCurrentFileListSize).setVisibility(View.VISIBLE);
                mCurrentFileListSize += 1;
            }

        }

        @Override
        public ImageView getEncryptionStatusIcon() {
            return vEncStatusIcon;
        }

        @Override
        public TextView getEncryptionStatusText() {
            return vEncStatusText;
        }

        @Override
        public ImageView getSignatureStatusIcon() {
            return vSigStatusIcon;
        }

        @Override
        public TextView getSignatureStatusText() {
            return vSigStatusText;
        }

        @Override
        public View getSignatureLayout() {
            return vSignatureLayout;
        }

        @Override
        public ViewAnimator getSignatureAction() {
            return vSignatureAction;
        }

        @Override
        public TextView getSignatureUserName() {
            return vSignatureName;
        }

        @Override
        public TextView getSignatureUserEmail() {
            return vSignatureMail;
        }

        @Override
        public boolean hasEncrypt() {
            return true;
        }
    }

}

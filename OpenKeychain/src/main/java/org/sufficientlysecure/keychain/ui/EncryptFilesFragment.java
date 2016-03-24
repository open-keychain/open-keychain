/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
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


import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.rdrei.android.dirchooser.DirectoryChooserActivity;
import net.rdrei.android.dirchooser.DirectoryChooserConfig;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.adapter.EncryptFilesAdapter;
import org.sufficientlysecure.keychain.ui.adapter.SpacesItemDecoration;
import org.sufficientlysecure.keychain.ui.base.CachingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.dialog.DeleteFileDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.EncryptFilesOptionsDialog;
import org.sufficientlysecure.keychain.ui.dialog.EncryptFilesOptionsDialog.FileOptionsDialogListener;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.ActionListener;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class EncryptFilesFragment
        extends CachingCryptoOperationFragment<SignEncryptParcel, SignEncryptResult>
        implements EncryptFilesAdapter.FilesListListener {

    public static final String ARG_DELETE_AFTER_ENCRYPT = "delete_after_encrypt";
    public static final String ARG_ENCRYPT_FILENAMES = "encrypt_filenames";
    public static final String ARG_USE_COMPRESSION = "use_compression";
    public static final String ARG_USE_ASCII_ARMOR = "use_ascii_armor";
    public static final String ARG_URIS = "uris";
    public static final String ARG_OPENED_FILE = "opened_file";

    public static final int REQUEST_CODE_INPUT = 0x00007003;
    private static final int REQUEST_CODE_OUTPUT = 0x00007007;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 12;

    private static final String TAG_DIALOG_OPTIONS = "dialogOptions";

    private boolean mUseArmor;
    private boolean mUseCompression;
    private boolean mDeleteAfterEncrypt;
    private boolean mEncryptFilenames;
    private boolean mHiddenRecipients = false;
    private ArrayList<Uri> mPendingInputUris;

    private AfterEncryptAction mAfterEncryptAction;


    private enum AfterEncryptAction {
        SAVE, SHARE, COPY;
    }

    private ArrayList<Uri> mOutputUris;

    private RecyclerView mSelectedFiles;

    private ArrayList<ViewModel> mDataset;
    private EncryptFilesAdapter mEncryptFilesAdapter;
    private int mOpenedFile;


    /**
     * Creates new instance of this fragment
     */
    public static EncryptFilesFragment newInstance(ArrayList<Uri> uris) {
        EncryptFilesFragment frag = new EncryptFilesFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_URIS, uris);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof EncryptActivity)) {
            throw new AssertionError(activity + " must inherit from EncryptionActivity");
        }
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_files_fragment, container, false);
        mSelectedFiles = (RecyclerView) view.findViewById(R.id.selected_files_list);

        mSelectedFiles.addItemDecoration(new SpacesItemDecoration(
                FormattingUtils.dpToPx(getActivity(), 4)));
        mSelectedFiles.setHasFixedSize(true);
        mSelectedFiles.setLayoutManager(new LinearLayoutManager(getActivity()));
        mSelectedFiles.setItemAnimator(new DefaultItemAnimator());

        mEncryptFilesAdapter = new EncryptFilesAdapter(mDataset, this,
                onFileClickListener,
                onFooterClickListener);
        mSelectedFiles.setAdapter(mEncryptFilesAdapter);

        if (savedInstanceState != null) {
            EncryptFilesOptionsDialog filesOptionsDialog =
                    (EncryptFilesOptionsDialog) getFragmentManager().findFragmentByTag(TAG_DIALOG_OPTIONS);
            if (filesOptionsDialog != null) {
                filesOptionsDialog.setListener(fileOptionsListener);
            }
        }

        mPendingInputUris = new ArrayList<>();

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(ARG_DELETE_AFTER_ENCRYPT, mDeleteAfterEncrypt);
        outState.putBoolean(ARG_USE_ASCII_ARMOR, mUseArmor);
        outState.putBoolean(ARG_USE_COMPRESSION, mUseCompression);
        outState.putBoolean(ARG_ENCRYPT_FILENAMES, mEncryptFilenames);

        outState.putParcelableArrayList(ARG_URIS, mDataset);
        outState.putInt(ARG_OPENED_FILE, mOpenedFile);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Preferences prefs = Preferences.getPreferences(getActivity());

        Bundle args = savedInstanceState == null ? getArguments() : savedInstanceState;
        mDeleteAfterEncrypt = args.getBoolean(ARG_DELETE_AFTER_ENCRYPT, false);

        if (args.containsKey(ARG_USE_ASCII_ARMOR)) {
            mUseArmor = args.getBoolean(ARG_USE_ASCII_ARMOR, false);
        } else {
            mUseArmor = prefs.getUseArmor();
        }

        if (args.containsKey(ARG_USE_COMPRESSION)) {
            mUseCompression = args.getBoolean(ARG_USE_COMPRESSION, true);
        } else {
            mUseCompression = prefs.getFilesUseCompression();
        }

        if (args.containsKey(ARG_ENCRYPT_FILENAMES)) {
            mEncryptFilenames = args.getBoolean(ARG_ENCRYPT_FILENAMES, true);
        } else {
            mEncryptFilenames = prefs.getEncryptFilenames();
        }

        if(args.containsKey(ARG_URIS)){
            mDataset = args.getParcelableArrayList(ARG_URIS);
        } else{
            mDataset = new ArrayList<>();
        }
        if(args.containsKey(ARG_OPENED_FILE)){
            mOpenedFile = args.getInt(ARG_OPENED_FILE);
        }

        setHasOptionsMenu(true);
    }

    public FileOptionsDialogListener fileOptionsListener = new FileOptionsDialogListener() {
        @Override
        public boolean onFileOptionsUpdated(EncryptFilesOptionsDialog.FileOptions options) {
            ViewModel model = mDataset.get(mOpenedFile);
            model.filenameOut = options.curFilename;
            model.defaultFilenameOut = false;
            mEncryptFilesAdapter.notifyDataSetChanged();
            return true;
        }
    };

    public View.OnClickListener onFileClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int index = (int) v.getTag();
            mOpenedFile = index;
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag(TAG_DIALOG_OPTIONS);
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            EncryptFilesOptionsDialog.FileOptions options;
            options = new EncryptFilesOptionsDialog.FileOptions(
                    mDataset.get(index).filename, mDataset.get(index).filenameOut
            );

            EncryptFilesOptionsDialog dialog = EncryptFilesOptionsDialog.newInstance(options);
            dialog.setListener(fileOptionsListener);
            dialog.show(ft, TAG_DIALOG_OPTIONS);
        }
    };

    public View.OnClickListener onFooterClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            FileHelper.openDocument(EncryptFilesFragment.this, mDataset.size() == 0 ?
                            null : mDataset.get(mDataset.size() - 1).inputUri,
                    "*/*", true, REQUEST_CODE_INPUT);
        }
    };

    public void addInputUri(Intent data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            mPendingInputUris.add(data.getData());
        } else {
            if (data.getClipData() != null && data.getClipData().getItemCount() > 0) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    if (uri != null) {
                        mPendingInputUris.add(uri);
                    }
                }
            } else {
                // fallback, try old method to get single uri
                mPendingInputUris.add(data.getData());
            }
        }

        // process pending uris
        processPendingInputUris();
    }

    private void processPendingInputUris() {
        Iterator<Uri> it = mPendingInputUris.iterator();
        while (it.hasNext()) {
            Uri inputUri = it.next();

            if (!checkAndRequestReadPermission(inputUri)) {
                // break out, don't process other uris and don't remove this one from queue
                break;
            }

            ViewModel newModel = new ViewModel(getActivity(), inputUri);
            try {
                if (mDataset.contains(newModel)) {
                    throw new IOException("Already added!");
                }
                mDataset.add(newModel);
            } catch (IOException e) {
                String fileName = FileHelper.getFilename(getActivity(), inputUri);
                Notify.create(getActivity(),
                        getActivity().getString(R.string.error_file_added_already, fileName),
                        Notify.Style.ERROR).show(this);
            }

            mEncryptFilesAdapter.notifyDataSetChanged();
            // remove from pending input uris
            it.remove();
        }

        mSelectedFiles.requestFocus();
    }

    /**
     * Request READ_EXTERNAL_STORAGE permission on Android >= 6.0 to read content from "file" Uris.
     * <p/>
     * This method returns true on Android < 6, or if permission is already granted. It
     * requests the permission and returns false otherwise.
     * <p/>
     * see https://commonsware.com/blog/2015/10/07/runtime-permissions-files-action-send.html
     */
    private boolean checkAndRequestReadPermission(final Uri uri) {
        if (!ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return true;
        }

        // Additional check due to https://commonsware.com/blog/2015/11/09/you-cannot-hold-nonexistent-permissions.html
        if (Build.VERSION.SDK_INT < VERSION_CODES.M) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
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
            // permission granted -> restart processing uris
            processPendingInputUris();
        } else {
            Toast.makeText(getActivity(), R.string.error_denied_storage_permission, Toast.LENGTH_LONG).show();
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
        }
    }

    private void showOutputFileDialog() {
        final Intent chooserIntent = new Intent(getActivity(), DirectoryChooserActivity.class);

        final DirectoryChooserConfig config = DirectoryChooserConfig.builder()
                .newDirectoryName("Output")
                .allowNewDirectoryNameModification(true)
                .build();

        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_CONFIG, config);
        startActivityForResult(chooserIntent, REQUEST_CODE_OUTPUT);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.encrypt_file_fragment, menu);

        menu.findItem(R.id.check_delete_after_encrypt).setChecked(mDeleteAfterEncrypt);
        menu.findItem(R.id.check_use_armor).setChecked(mUseArmor);
        menu.findItem(R.id.check_enable_compression).setChecked(mUseCompression);
        menu.findItem(R.id.check_encrypt_filenames).setChecked(mEncryptFilenames);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.encrypt_save: {
                hideKeyboard();
                mAfterEncryptAction = AfterEncryptAction.SAVE;
                cryptoOperation(new CryptoInputParcel(new Date()));
                break;
            }
            case R.id.encrypt_share: {
                hideKeyboard();
                mAfterEncryptAction = AfterEncryptAction.SHARE;
                cryptoOperation(new CryptoInputParcel(new Date()));
                break;
            }
            case R.id.encrypt_copy: {
                hideKeyboard();
                mAfterEncryptAction = AfterEncryptAction.COPY;
                cryptoOperation(new CryptoInputParcel(new Date()));
                break;
            }
            case R.id.check_use_armor: {
                toggleUseArmor(item, !item.isChecked());
                break;
            }
            case R.id.check_delete_after_encrypt: {
                item.setChecked(!item.isChecked());
                mDeleteAfterEncrypt = item.isChecked();
                break;
            }
            case R.id.check_enable_compression: {
                toggleEnableCompression(item, !item.isChecked());
                break;
            }
            case R.id.check_encrypt_filenames: {
                toggleEncryptFilenamesCheck(item, !item.isChecked());
                break;
            }
//            case R.id.check_hidden_recipients: {
//                mHiddenRecipients = item.isChecked();
//                notifyUpdate();
//                break;
//            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }
        return true;
    }

    public void toggleUseArmor(MenuItem item, final boolean useArmor) {
        mUseArmor = useArmor;
        item.setChecked(useArmor);

        Notify.create(getActivity(), useArmor
                        ? R.string.snack_armor_on
                        : R.string.snack_armor_off,
                Notify.LENGTH_LONG, Style.OK, new ActionListener() {
                    @Override
                    public void onAction() {
                        Preferences.getPreferences(getActivity()).setUseArmor(useArmor);
                        Notify.create(getActivity(), useArmor
                                        ? R.string.snack_armor_on
                                        : R.string.snack_armor_off,
                                Notify.LENGTH_SHORT, Style.OK, null, R.string.btn_saved)
                                .show(EncryptFilesFragment.this, false);
                    }
                }, R.string.btn_save_default).show(this);

        mEncryptFilesAdapter.notifyDataSetChanged();
    }

    public void toggleEnableCompression(MenuItem item, final boolean compress) {
        mUseCompression = compress;
        item.setChecked(compress);

        Notify.create(getActivity(), compress
                        ? R.string.snack_compression_on
                        : R.string.snack_compression_off,
                Notify.LENGTH_LONG, Style.OK, new ActionListener() {
                    @Override
                    public void onAction() {
                        Preferences.getPreferences(getActivity()).setFilesUseCompression(compress);
                        Notify.create(getActivity(), compress
                                        ? R.string.snack_compression_on
                                        : R.string.snack_compression_off,
                                Notify.LENGTH_SHORT, Style.OK, null, R.string.btn_saved)
                                .show(EncryptFilesFragment.this, false);
                    }
                }, R.string.btn_save_default).show(this);
    }

    public void toggleEncryptFilenamesCheck(MenuItem item, final boolean encryptFilenames) {
        mEncryptFilenames = encryptFilenames;
        item.setChecked(encryptFilenames);

        Notify.create(getActivity(), encryptFilenames
                        ? R.string.snack_encrypt_filenames_on
                        : R.string.snack_encrypt_filenames_off,
                Notify.LENGTH_LONG, Style.OK, new ActionListener() {
                    @Override
                    public void onAction() {
                        Preferences.getPreferences(getActivity()).setEncryptFilenames(encryptFilenames);
                        Notify.create(getActivity(), encryptFilenames
                                        ? R.string.snack_encrypt_filenames_on
                                        : R.string.snack_encrypt_filenames_off,
                                Notify.LENGTH_SHORT, Style.OK, null, R.string.btn_saved)
                                .show(EncryptFilesFragment.this, false);
                    }
                }, R.string.btn_save_default).show(this);

        mEncryptFilesAdapter.notifyDataSetChanged();
    }

    @Override
    public void onQueuedOperationSuccess(final SignEncryptResult result) {
        super.onQueuedOperationSuccess(result);

        hideKeyboard();

        // protected by Queueing*Fragment
        FragmentActivity activity = getActivity();

        if (mDeleteAfterEncrypt) {
            // TODO make behavior coherent here
            DeleteFileDialogFragment deleteFileDialog =
                    DeleteFileDialogFragment.newInstance(getUris());
            deleteFileDialog.setOnDeletedListener(new DeleteFileDialogFragment.OnDeletedListener() {

                @Override
                public void onDeleted() {
                    if (mAfterEncryptAction == AfterEncryptAction.SHARE) {
                        // Share encrypted message/file
                        startActivity(Intent.createChooser(createSendIntent(), getString(R.string.title_share_file)));
                    } else {
                        Activity activity = getActivity();
                        if (activity == null) {
                            // it's gone, there's nothing we can do here
                            return;
                        }
                        // Save encrypted file
                        result.createNotify(activity).show();
                    }
                }

            });
            deleteFileDialog.show(activity.getSupportFragmentManager(), "deleteDialog");
        } else {

            switch (mAfterEncryptAction) {

                case SHARE:
                    // Share encrypted message/file
                    startActivity(Intent.createChooser(createSendIntent(), getString(R.string.title_share_file)));
                    break;

                case COPY:

                    ClipboardManager clipMan = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    if (clipMan == null) {
                        Notify.create(activity, R.string.error_clipboard_copy, Style.ERROR).show();
                        break;
                    }
                    ClipData clip = new ClipData(getString(R.string.label_clip_title),
                            // make available as application/pgp-encrypted
                            new String[]{"text/plain"},
                            new ClipData.Item(mOutputUris.get(0))
                    );
                    clipMan.setPrimaryClip(clip);
                    result.createNotify(activity).show();
                    break;

                case SAVE:
                    // Encrypted file was saved already, just show notification
                    result.createNotify(activity).show();
                    break;
            }
        }

    }

    // prepares mOutputUris, either directly and returns false, or indirectly
    // which returns true and will call cryptoOperation after mOutputUris has
    // been set at a later point.
    private boolean prepareOutputStreams() {

        switch (mAfterEncryptAction) {
            default:
            case SHARE:
                mOutputUris = new ArrayList<>();
                int filenameCounter = 1;
                for (ViewModel model : mDataset) {
                    String targetName;
                    if (mEncryptFilenames) {
                        if (model.defaultFilenameOut) {
                            targetName = String.valueOf(filenameCounter) + (mUseArmor
                                    ? Constants.FILE_EXTENSION_ASC : Constants.FILE_EXTENSION_PGP_MAIN);
                        } else {
                            targetName = model.filenameOut + (mUseArmor
                                    ? Constants.FILE_EXTENSION_ASC : Constants.FILE_EXTENSION_PGP_MAIN);
                        }
                    } else {
                        targetName = model.filenameOut + (mUseArmor
                                ? Constants.FILE_EXTENSION_ASC : Constants.FILE_EXTENSION_PGP_MAIN);
                    }

                    mOutputUris.add(TemporaryFileProvider.createFile(getActivity(), targetName));
                    filenameCounter++;
                }
                return false;

            case SAVE:
                showOutputFileDialog();
                return true;

            case COPY:
                // nothing to do here, but make sure
                if (mDataset.size() > 1) {
                    Notify.create(getActivity(), R.string.error_multi_clipboard, Notify.Style.ERROR).show(this);
                    return true;
                }
                mOutputUris = new ArrayList<>();
                String targetName = (mEncryptFilenames
                        ? String.valueOf(1) : FileHelper.getFilename(getActivity(),
                        mDataset.get(0).inputUri)) + Constants.FILE_EXTENSION_ASC;
                mOutputUris.add(TemporaryFileProvider.createFile(getActivity(), targetName, "text/plain"));
                return false;
        }

    }

    public SignEncryptParcel createOperationInput() {

        SignEncryptParcel actionsParcel = getCachedActionsParcel();

        // we have three cases here: nothing cached, cached except output, fully cached
        if (actionsParcel == null) {

            // clear output uris for now, they will be created by prepareOutputStreams later
            mOutputUris = null;

            actionsParcel = createIncompleteCryptoInput();
            // this is null if invalid, just return in that case
            if (actionsParcel == null) {
                return null;
            }

            cacheActionsParcel(actionsParcel);

        }

        // if it's incomplete, prepare output streams
        if (actionsParcel.isIncomplete()) {
            // if this is still null, prepare output streams again
            if (mOutputUris == null) {
                // this may interrupt the flow, and call us again from onActivityResult
                if (prepareOutputStreams()) {
                    return null;
                }
            }

            actionsParcel.addOutputUris(mOutputUris);
            cacheActionsParcel(actionsParcel);

        }

        return actionsParcel;

    }

    protected SignEncryptParcel createIncompleteCryptoInput() {

        if (mDataset.size() == 0) {
            Notify.create(getActivity(), R.string.error_no_file_selected, Notify.Style.ERROR).show(this);
            return null;
        }

        // fill values for this action
        SignEncryptParcel data = new SignEncryptParcel();


        data.addInputUris(getUris());

        if (mUseCompression) {
            data.setCompressionAlgorithm(
                    PgpSecurityConstants.OpenKeychainCompressionAlgorithmTags.USE_DEFAULT);
        } else {
            data.setCompressionAlgorithm(
                    PgpSecurityConstants.OpenKeychainCompressionAlgorithmTags.UNCOMPRESSED);
        }
        data.setHiddenRecipients(mHiddenRecipients);
        data.setEnableAsciiArmorOutput(mAfterEncryptAction == AfterEncryptAction.COPY || mUseArmor);
        data.setSymmetricEncryptionAlgorithm(
                PgpSecurityConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_DEFAULT);
        data.setSignatureHashAlgorithm(
                PgpSecurityConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_DEFAULT);

        EncryptActivity encryptActivity = (EncryptActivity) getActivity();
        EncryptModeFragment modeFragment = encryptActivity.getModeFragment();

        if (modeFragment.isAsymmetric()) {
            long[] encryptionKeyIds = modeFragment.getAsymmetricEncryptionKeyIds();
            long signingKeyId = modeFragment.getAsymmetricSigningKeyId();

            boolean gotEncryptionKeys = (encryptionKeyIds != null && encryptionKeyIds.length > 0);

            if (!gotEncryptionKeys && signingKeyId != 0) {
                Notify.create(getActivity(), R.string.error_detached_signature, Notify.Style.ERROR).show(this);
                return null;
            }
            if (!gotEncryptionKeys) {
                Notify.create(getActivity(), R.string.select_encryption_key, Notify.Style.ERROR).show(this);
                return null;
            }

            data.setEncryptionMasterKeyIds(encryptionKeyIds);
            data.setSignatureMasterKeyId(signingKeyId);
        } else {
            Passphrase passphrase = modeFragment.getSymmetricPassphrase();
            if (passphrase == null) {
                Notify.create(getActivity(), R.string.passphrases_do_not_match, Notify.Style.ERROR)
                        .show(this);
                return null;
            }
            if (passphrase.isEmpty()) {
                Notify.create(getActivity(), R.string.passphrase_must_not_be_empty, Notify.Style.ERROR)
                        .show(this);
                return null;
            }
            data.setSymmetricPassphrase(passphrase);
        }

        return data;
    }

    private Intent createSendIntent() {
        Intent sendIntent;
        // file
        if (mOutputUris.size() == 1) {
            sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.putExtra(Intent.EXTRA_STREAM, mOutputUris.get(0));
        } else {
            sendIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            sendIntent.putExtra(Intent.EXTRA_STREAM, mOutputUris);
        }
        sendIntent.setType(Constants.MIME_TYPE_ENCRYPTED_ALTERNATE);

        EncryptActivity modeInterface = (EncryptActivity) getActivity();
        EncryptModeFragment modeFragment = modeInterface.getModeFragment();
        if (!modeFragment.isAsymmetric()) {
            return sendIntent;
        }

        String[] encryptionUserIds = modeFragment.getAsymmetricEncryptionUserIds();
        if (encryptionUserIds == null) {
            return sendIntent;
        }

        Set<String> users = new HashSet<>();
        for (String user : encryptionUserIds) {
            KeyRing.UserId userId = KeyRing.splitUserId(user);
            if (userId.email != null) {
                users.add(userId.email);
            }
        }
        // pass trough email addresses as extra for email applications
        sendIntent.putExtra(Intent.EXTRA_EMAIL, users.toArray(new String[users.size()]));

        return sendIntent;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_INPUT: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    addInputUri(data);
                }
                return;
            }
            case REQUEST_CODE_OUTPUT: {
                // This happens after output dir was selected, so start our operation
                if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                    String dir = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                    mOutputUris = new ArrayList<>();
                    int filenameCounter = 1;

                    for (ViewModel model : mDataset) {
                        String targetName;
                        if (mEncryptFilenames) {
                            if (model.defaultFilenameOut) {
                                targetName = String.valueOf(filenameCounter) + (mUseArmor
                                        ? Constants.FILE_EXTENSION_ASC : Constants.FILE_EXTENSION_PGP_MAIN);
                            } else {
                                targetName = model.filenameOut + (mUseArmor
                                        ? Constants.FILE_EXTENSION_ASC : Constants.FILE_EXTENSION_PGP_MAIN);
                            }
                        } else {
                            targetName = model.filenameOut + (mUseArmor
                                    ? Constants.FILE_EXTENSION_ASC : Constants.FILE_EXTENSION_PGP_MAIN);
                        }

                        mOutputUris.add(Uri.fromFile(new File(dir + "/" + targetName)));
                        filenameCounter++;
                    }

                    // make sure this is correct at this point
                    mAfterEncryptAction = AfterEncryptAction.SAVE;
                    cryptoOperation(new CryptoInputParcel(new Date()));
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }

    private ArrayList<Uri> getUris(){
        ArrayList<Uri> uris = new ArrayList<>();
        for(ViewModel m : mDataset){
            uris.add(m.inputUri);
        }
        return  uris;
    }

    @Override
    public boolean useArmor() {
        return mUseArmor;
    }

    @Override
    public boolean encryptFilenames() {
        return mEncryptFilenames;
    }

    public static class ViewModel implements Parcelable {
        public Uri inputUri;
        public Bitmap thumbnail;
        public String filename;
        public String filenameOut;
        public boolean defaultFilenameOut;
        public long fileSize;


        public ViewModel(Context context, Uri inputUri) {
            this.inputUri = inputUri;
            int px = FormattingUtils.dpToPx(context, 48);
            this.thumbnail = FileHelper.getThumbnail(context, inputUri, new Point(px, px));
            this.filename = FileHelper.getFilename(context, inputUri);
            this.filenameOut = FileHelper.getFilename(context, inputUri);
            this.defaultFilenameOut = true;
            this.fileSize = FileHelper.getFileSize(context, inputUri);
        }

        public ViewModel(Parcel in) {
            thumbnail = in.readParcelable(null);
            String[] stringData = new String[3];
            in.readStringArray(stringData);
            this.inputUri = Uri.parse(stringData[0]);
            this.filename = stringData[1];
            this.filenameOut = stringData[2];
            boolean[] booleanData = new boolean[1];
            in.readBooleanArray(booleanData);
            this.defaultFilenameOut = booleanData[0];
            in.readLong();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(thumbnail, 0);
            dest.writeStringArray(new String[]{
                    this.inputUri.toString(),
                    this.filename,
                    this.filenameOut
            });
            dest.writeBooleanArray(new boolean[]{
                    this.defaultFilenameOut
            });
            dest.writeLong(fileSize);
        }

        public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
            public ViewModel createFromParcel(Parcel in) {
                return new ViewModel(in);
            }

            public ViewModel[] newArray(int size) {
                return new ViewModel[size];
            }
        };

        /**
         * Depends on inputUri only
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ViewModel viewModel = (ViewModel) o;
            return !(inputUri != null ? !inputUri.equals(viewModel.inputUri)
                    : viewModel.inputUri != null);
        }

        /**
         * Depends on inputUri only
         */
        @Override
        public int hashCode() {
            return inputUri != null ? inputUri.hashCode() : 0;
        }

        @Override
        public String toString() {
            return inputUri.toString();
        }
    }

}

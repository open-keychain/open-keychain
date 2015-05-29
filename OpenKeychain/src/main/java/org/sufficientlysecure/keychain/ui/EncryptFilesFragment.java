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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.SignEncryptResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.pgp.PgpConstants;
import org.sufficientlysecure.keychain.pgp.SignEncryptParcel;
import org.sufficientlysecure.keychain.provider.TemporaryStorageProvider;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.adapter.SpacesItemDecoration;
import org.sufficientlysecure.keychain.ui.base.CachingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.dialog.DeleteFileDialogFragment;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ShareHelper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EncryptFilesFragment extends CachingCryptoOperationFragment<SignEncryptParcel> {

    public static final String ARG_DELETE_AFTER_ENCRYPT = "delete_after_encrypt";
    public static final String ARG_ENCRYPT_FILENAMES = "encrypt_filenames";
    public static final String ARG_USE_COMPRESSION = "use_compression";
    public static final String ARG_USE_ASCII_ARMOR = "use_ascii_armor";
    public static final String ARG_URIS = "uris";

    private static final int REQUEST_CODE_INPUT = 0x00007003;
    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    private boolean mUseArmor;
    private boolean mUseCompression;
    private boolean mDeleteAfterEncrypt;
    private boolean mEncryptFilenames;
    private boolean mHiddenRecipients = false;

    private boolean mShareAfterEncrypt;

    private ArrayList<Uri> mOutputUris;

    private RecyclerView mSelectedFiles;

    ArrayList<FilesAdapter.ViewModel> mFilesModels;
    FilesAdapter mFilesAdapter;

    /**
     * Creates new instance of this fragment
     */
    public static EncryptFilesFragment newInstance(ArrayList<Uri> uris, boolean useArmor) {
        EncryptFilesFragment frag = new EncryptFilesFragment();

        Bundle args = new Bundle();
        args.putBoolean(ARG_USE_ASCII_ARMOR, useArmor);
        args.putParcelableArrayList(ARG_URIS, uris);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if ( ! (activity instanceof EncryptActivity) ) {
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

        mFilesModels = new ArrayList<>();
        mFilesAdapter = new FilesAdapter(getActivity(), mFilesModels, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addInputUri();
            }
        });

        Bundle args = savedInstanceState == null ? getArguments() : savedInstanceState;

        ArrayList<Uri> inputUris = args.getParcelableArrayList(ARG_URIS);
        if (inputUris != null) {
            mFilesAdapter.addAll(inputUris);
        }
        mSelectedFiles.setAdapter(mFilesAdapter);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(ARG_DELETE_AFTER_ENCRYPT, mDeleteAfterEncrypt);
        outState.putBoolean(ARG_USE_ASCII_ARMOR, mUseArmor);
        outState.putBoolean(ARG_USE_COMPRESSION, mUseCompression);
        outState.putBoolean(ARG_ENCRYPT_FILENAMES, mEncryptFilenames);

        outState.putParcelableArrayList(ARG_URIS, mFilesAdapter.getAsArrayList());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = savedInstanceState == null ? getArguments() : savedInstanceState;
        mDeleteAfterEncrypt = args.getBoolean(ARG_DELETE_AFTER_ENCRYPT, false);
        mUseArmor = args.getBoolean(ARG_USE_ASCII_ARMOR, false);
        mUseCompression = args.getBoolean(ARG_USE_COMPRESSION, true);
        mEncryptFilenames = args.getBoolean(ARG_ENCRYPT_FILENAMES, true);

        setHasOptionsMenu(true);
    }

    private void addInputUri() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            FileHelper.openDocument(EncryptFilesFragment.this, "*/*", true, REQUEST_CODE_INPUT);
        } else {
            FileHelper.openFile(EncryptFilesFragment.this, mFilesModels.isEmpty() ?
                            null : mFilesModels.get(mFilesModels.size() - 1).inputUri,
                    "*/*", REQUEST_CODE_INPUT);
        }
    }

    private void addInputUri(Uri inputUri) {
        if (inputUri == null) {
            return;
        }

        try {
            mFilesAdapter.add(inputUri);
        } catch (IOException e) {
            Notify.create(getActivity(),
                    getActivity().getString(R.string.error_file_added_already, FileHelper.getFilename(getActivity(), inputUri)),
                    Notify.Style.ERROR).show(this);
            return;
        }
        mSelectedFiles.requestFocus();
    }

    private void showOutputFileDialog() {
        if (mFilesModels.size() > 1 || mFilesModels.isEmpty()) {
            throw new IllegalStateException();
        }
        FilesAdapter.ViewModel model = mFilesModels.get(0);
        String targetName =
                (mEncryptFilenames ? "1" : FileHelper.getFilename(getActivity(), model.inputUri))
                        + (mUseArmor ? Constants.FILE_EXTENSION_ASC : Constants.FILE_EXTENSION_PGP_MAIN);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            File file = new File(model.inputUri.getPath());
            File parentDir = file.exists() ? file.getParentFile() : Constants.Path.APP_DIR;
            File targetFile = new File(parentDir, targetName);
            FileHelper.saveFile(this, getString(R.string.title_encrypt_to_file),
                    getString(R.string.specify_file_to_encrypt_to), targetFile, REQUEST_CODE_OUTPUT);
        } else {
            FileHelper.saveDocument(this, "*/*", targetName, REQUEST_CODE_OUTPUT);
        }
    }

    public void addFile(Intent data) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            addInputUri(data.getData());
        } else {
            if (data.getClipData() != null && data.getClipData().getItemCount() > 0) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri uri = data.getClipData().getItemAt(i).getUri();
                    if (uri != null) {
                        addInputUri(uri);
                    }
                }
            } else {
                // fallback, try old method to get single uri
                addInputUri(data.getData());
            }
        }
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
                cryptoOperation(false);
                break;
            }
            case R.id.encrypt_share: {
                cryptoOperation(true);
                break;
            }
            case R.id.check_use_armor: {
                // we can NOT do this for every item, others might care!
                item.setChecked(!item.isChecked());
                mUseArmor = item.isChecked();
                break;
            }
            case R.id.check_delete_after_encrypt: {
                item.setChecked(!item.isChecked());
                mDeleteAfterEncrypt = item.isChecked();
                break;
            }
            case R.id.check_enable_compression: {
                item.setChecked(!item.isChecked());
                mUseCompression = item.isChecked();
                break;
            }
            case R.id.check_encrypt_filenames: {
                item.setChecked(!item.isChecked());
                mEncryptFilenames = item.isChecked();
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

    public void onEncryptSuccess(final SignEncryptResult result) {
        if (mDeleteAfterEncrypt) {
            DeleteFileDialogFragment deleteFileDialog =
                    DeleteFileDialogFragment.newInstance(mFilesAdapter.getAsArrayList());
            deleteFileDialog.setOnDeletedListener(new DeleteFileDialogFragment.OnDeletedListener() {

                @Override
                public void onDeleted() {
                    if (mShareAfterEncrypt) {
                        // Share encrypted message/file
                        startActivity(sendWithChooserExcludingEncrypt());
                    } else {
                        // Save encrypted file
                        result.createNotify(getActivity()).show();
                    }
                }

            });
            deleteFileDialog.show(getActivity().getSupportFragmentManager(), "deleteDialog");
        } else {
            if (mShareAfterEncrypt) {
                // Share encrypted message/file
                startActivity(sendWithChooserExcludingEncrypt());
            } else {
                // Save encrypted file
                result.createNotify(getActivity()).show();
            }
        }
    }

    // prepares mOutputUris, either directly and returns false, or indirectly
    // which returns true and will call cryptoOperation after mOutputUris has
    // been set at a later point.
    private boolean prepareOutputStreams(boolean share) {

        if (mFilesModels.isEmpty()) {
            Notify.create(getActivity(), R.string.no_file_selected, Notify.Style.ERROR)
                    .show(this);
            return true;
        } else if (mFilesModels.size() > 1 && !mShareAfterEncrypt) {
            Log.e(Constants.TAG, "Aborting: mInputUris.size() > 1 && !mShareAfterEncrypt");
            // This should be impossible...
            return true;
        }

        if (share) {
            mOutputUris = new ArrayList<>();
            int filenameCounter = 1;
            for (FilesAdapter.ViewModel model : mFilesModels) {
                String targetName =
                        (mEncryptFilenames ? String.valueOf(filenameCounter) : FileHelper.getFilename(getActivity(), model.inputUri))
                                + (mUseArmor ? Constants.FILE_EXTENSION_ASC : Constants.FILE_EXTENSION_PGP_MAIN);
                mOutputUris.add(TemporaryStorageProvider.createFile(getActivity(), targetName));
                filenameCounter++;
            }
            return false;
        } else {
            if (mFilesModels.size() > 1) {
                Notify.create(getActivity(), R.string.error_multi_not_supported,
                        Notify.Style.ERROR).show(this);
                return true;
            }
            showOutputFileDialog();
            return true;
        }
    }

    protected SignEncryptParcel createIncompleteEncryptBundle() {

        // fill values for this action
        SignEncryptParcel data = new SignEncryptParcel();

        data.addInputUris(mFilesAdapter.getAsArrayList());

        if (mUseCompression) {
            data.setCompressionId(PgpConstants.sPreferredCompressionAlgorithms.get(0));
        } else {
            data.setCompressionId(CompressionAlgorithmTags.UNCOMPRESSED);
        }
        data.setHiddenRecipients(mHiddenRecipients);
        data.setEnableAsciiArmorOutput(mUseArmor);
        data.setSymmetricEncryptionAlgorithm(PgpConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_PREFERRED);
        data.setSignatureHashAlgorithm(PgpConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_PREFERRED);

        EncryptActivity encryptActivity = (EncryptActivity) getActivity();
        EncryptModeFragment modeFragment = encryptActivity.getModeFragment();

        if (modeFragment.isAsymmetric()) {
            long[] encryptionKeyIds = modeFragment.getAsymmetricEncryptionKeyIds();
            long signingKeyId = modeFragment.getAsymmetricSigningKeyId();

            boolean gotEncryptionKeys = (encryptionKeyIds != null
                    && encryptionKeyIds.length > 0);

            if (!gotEncryptionKeys && signingKeyId == 0) {
                Notify.create(getActivity(), R.string.select_encryption_or_signature_key, Notify.Style.ERROR)
                        .show(this);
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

    /**
     * Create Intent Chooser but exclude OK's EncryptActivity.
     */
    private Intent sendWithChooserExcludingEncrypt() {
        Intent prototype = createSendIntent();
        String title = getString(R.string.title_share_file);

        // we don't want to encrypt the encrypted, no inception ;)
        String[] blacklist = new String[]{
                Constants.PACKAGE_NAME + ".ui.EncryptFileActivity",
                "org.thialfihar.android.apg.ui.EncryptActivity"
        };

        return new ShareHelper(getActivity()).createChooserExcluding(prototype, title, blacklist);
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
        sendIntent.setType(Constants.ENCRYPTED_FILES_MIME);

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

    public void cryptoOperation(boolean share) {
        mShareAfterEncrypt = share;
        cryptoOperation();
    }

    @Override
    protected void cryptoOperation(CryptoInputParcel cryptoInput, SignEncryptParcel actionsParcel) {

        // we have three cases here: nothing cached, cached except output, fully cached
        if (actionsParcel == null) {

            // clear output uris for now, they will be created by prepareOutputStreams later
            mOutputUris = null;

            actionsParcel = createIncompleteEncryptBundle();
            // this is null if invalid, just return in that case
            if (actionsParcel == null) {
                // Notify was created by createEncryptBundle.
                return;
            }

            cacheActionsParcel(actionsParcel);
        }

        // if it's incomplete, prepare output streams
        if (actionsParcel.isIncomplete()) {
            // if this is still null, prepare output streams again
            if (mOutputUris == null) {
                // this may interrupt the flow, and call us again from onActivityResult
                if (prepareOutputStreams(mShareAfterEncrypt)) {
                    return;
                }
            }

            actionsParcel.addOutputUris(mOutputUris);
            cacheActionsParcel(actionsParcel);
        }

        // Send all information needed to service to edit key in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_SIGN_ENCRYPT);

        Bundle data = new Bundle();
        data.putParcelable(KeychainIntentService.SIGN_ENCRYPT_PARCEL, actionsParcel);
        data.putParcelable(KeychainIntentService.EXTRA_CRYPTO_INPUT, cryptoInput);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after encrypting is done in KeychainIntentService
        ServiceProgressHandler serviceHandler = new ServiceProgressHandler(
                getActivity(),
                getString(R.string.progress_encrypting),
                ProgressDialog.STYLE_HORIZONTAL,
                true,
                ProgressDialogFragment.ServiceType.KEYCHAIN_INTENT) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                // handle pending messages
                if (handlePendingMessage(message)) {
                    return;
                }

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    SignEncryptResult result =
                            message.getData().getParcelable(SignEncryptResult.EXTRA_RESULT);
                    if (result.success()) {
                        onEncryptSuccess(result);
                    } else {
                        result.createNotify(getActivity()).show();
                    }
                }
            }
        };
        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(serviceHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        serviceHandler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_INPUT: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    addFile(data);
                }
                return;
            }
            case REQUEST_CODE_OUTPUT: {
                // This happens after output file was selected, so start our operation
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mOutputUris = new ArrayList<>(1);
                    mOutputUris.add(data.getData());
                    cryptoOperation(false);
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }

    public static class FilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private Activity mActivity;
        private List<ViewModel> mDataset;
        private View.OnClickListener mFooterOnClickListener;
        private static final int TYPE_FOOTER = 0;
        private static final int TYPE_ITEM = 1;

        public static class ViewModel {
            Uri inputUri;
            Bitmap thumbnail;
            String filename;
            long fileSize;

            ViewModel(Context context, Uri inputUri) {
                this.inputUri = inputUri;
                int px = FormattingUtils.dpToPx(context, 48);
                this.thumbnail = FileHelper.getThumbnail(context, inputUri, new Point(px, px));
                this.filename = FileHelper.getFilename(context, inputUri);
                this.fileSize = FileHelper.getFileSize(context, inputUri);
            }

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

        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
        class ViewHolder extends RecyclerView.ViewHolder {
            public TextView filename;
            public TextView fileSize;
            public View removeButton;
            public ImageView thumbnail;

            public ViewHolder(View itemView) {
                super(itemView);
                filename = (TextView) itemView.findViewById(R.id.filename);
                fileSize = (TextView) itemView.findViewById(R.id.filesize);
                removeButton = itemView.findViewById(R.id.action_remove_file_from_list);
                thumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            }
        }

        class FooterHolder extends RecyclerView.ViewHolder {
            public Button mAddButton;

            public FooterHolder(View itemView) {
                super(itemView);
                mAddButton = (Button) itemView.findViewById(R.id.file_list_entry_add);
            }
        }

        // Provide a suitable constructor (depends on the kind of dataset)
        public FilesAdapter(Activity activity, List<ViewModel> myDataset, View.OnClickListener onFooterClickListener) {
            mActivity = activity;
            mDataset = myDataset;
            mFooterOnClickListener = onFooterClickListener;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_FOOTER) {
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.file_list_entry_add, parent, false);
                return new FooterHolder(v);
            } else {
                //inflate your layout and pass it to view holder
                View v = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.file_list_entry, parent, false);
                return new ViewHolder(v);
            }
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof FooterHolder) {
                FooterHolder thisHolder = (FooterHolder) holder;
                thisHolder.mAddButton.setOnClickListener(mFooterOnClickListener);
            } else if (holder instanceof ViewHolder) {
                ViewHolder thisHolder = (ViewHolder) holder;
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                final ViewModel model = mDataset.get(position);

                thisHolder.filename.setText(model.filename);
                if (model.fileSize == -1) {
                    thisHolder.fileSize.setText("");
                } else {
                    thisHolder.fileSize.setText(FileHelper.readableFileSize(model.fileSize));
                }
                thisHolder.removeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        remove(model);
                    }
                });
                if (model.thumbnail != null) {
                    thisHolder.thumbnail.setImageBitmap(model.thumbnail);
                } else {
                    thisHolder.thumbnail.setImageResource(R.drawable.ic_doc_generic_am);
                }
            }
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            if (isPositionFooter(position)) {
                return TYPE_FOOTER;
            } else {
                return TYPE_ITEM;
            }
        }

        private boolean isPositionFooter(int position) {
            return position == mDataset.size();
        }

        public void add(Uri inputUri) throws IOException {
            ViewModel newModel = new ViewModel(mActivity, inputUri);
            if (mDataset.contains(newModel)) {
                throw new IOException("Already added!");
            }
            mDataset.add(newModel);
            notifyItemInserted(mDataset.size() - 1);
        }

        public void addAll(ArrayList<Uri> inputUris) {
            if (inputUris != null) {
                int startIndex = mDataset.size();
                for (Uri inputUri : inputUris) {
                    ViewModel newModel = new ViewModel(mActivity, inputUri);
                    if (mDataset.contains(newModel)) {
                        Log.e(Constants.TAG, "Skipped duplicate " + inputUri);
                    } else {
                        mDataset.add(newModel);
                    }
                }
                notifyItemRangeInserted(startIndex, mDataset.size() - startIndex);
            }
        }

        public void remove(ViewModel model) {
            int position = mDataset.indexOf(model);
            mDataset.remove(position);
            notifyItemRemoved(position);
        }

        public ArrayList<Uri> getAsArrayList() {
            ArrayList<Uri> uris = new ArrayList<>();
            for (ViewModel model : mDataset) {
                uris.add(model.inputUri);
            }
            return uris;
        }

    }
}

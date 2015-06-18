/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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


import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.TemporaryStorageProvider;
// this import NEEDS to be above the ViewModel one, or it won't compile! (as of 06/06/15)
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.StatusHolder;
import org.sufficientlysecure.keychain.ui.DecryptFilesListFragment.DecryptFilesAdapter.ViewModel;
import org.sufficientlysecure.keychain.ui.adapter.SpacesItemDecoration;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;

public class DecryptFilesListFragment
        extends CryptoOperationFragment<PgpDecryptVerifyInputParcel,DecryptVerifyResult>
        implements OnMenuItemClickListener {
    public static final String ARG_URIS = "uris";

    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    private ArrayList<Uri> mInputUris;
    private HashMap<Uri, Uri> mOutputUris;
    private ArrayList<Uri> mPendingInputUris;

    private Uri mCurrentInputUri;

    private DecryptFilesAdapter mAdapter;

    /**
     * Creates new instance of this fragment
     */
    public static DecryptFilesListFragment newInstance(ArrayList<Uri> uris) {
        DecryptFilesListFragment frag = new DecryptFilesListFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_URIS, uris);
        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_files_list_fragment, container, false);

        RecyclerView vFilesList = (RecyclerView) view.findViewById(R.id.decrypted_files_list);

        vFilesList.addItemDecoration(new SpacesItemDecoration(
                FormattingUtils.dpToPx(getActivity(), 4)));
        vFilesList.setHasFixedSize(true);
        vFilesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        vFilesList.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new DecryptFilesAdapter(getActivity(), this);
        vFilesList.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(ARG_URIS, mInputUris);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        displayInputUris(getArguments().<Uri>getParcelableArrayList(ARG_URIS));
    }

    private String removeEncryptedAppend(String name) {
        if (name.endsWith(Constants.FILE_EXTENSION_ASC)
                || name.endsWith(Constants.FILE_EXTENSION_PGP_MAIN)
                || name.endsWith(Constants.FILE_EXTENSION_PGP_ALTERNATE)) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private void askForOutputFilename(Uri inputUri, String originalFilename, String mimeType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            File file = new File(inputUri.getPath());
            File parentDir = file.exists() ? file.getParentFile() : Constants.Path.APP_DIR;
            File targetFile = new File(parentDir, originalFilename);
            FileHelper.saveFile(this, getString(R.string.title_decrypt_to_file),
                    getString(R.string.specify_file_to_decrypt_to), targetFile, REQUEST_CODE_OUTPUT);
        } else {
            FileHelper.saveDocument(this, mimeType, originalFilename, REQUEST_CODE_OUTPUT);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OUTPUT: {
                // This happens after output file was selected, so start our operation
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Uri saveUri = data.getData();
                    Uri outputUri = mOutputUris.get(mCurrentInputUri);
                    // TODO save from outputUri to saveUri

                    mCurrentInputUri = null;
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void displayInputUris(ArrayList<Uri> uris) {
        mInputUris = uris;
        mOutputUris = new HashMap<>(uris.size());
        for (Uri uri : uris) {
            mAdapter.add(uri);
            mOutputUris.put(uri, TemporaryStorageProvider.createFile(getActivity()));
        }

        mPendingInputUris = uris;

        cryptoOperation();
    }

    @Override
    protected void cryptoOperation(CryptoInputParcel cryptoInput) {
        super.cryptoOperation(cryptoInput, false);
    }

    @Override
    protected boolean onCryptoSetProgress(String msg, int progress, int max) {
        mAdapter.setProgress(mCurrentInputUri, progress, max, msg);
        return true;
    }

    @Override
    protected void dismissProgress() {
        // progress shown inline, so never mind
    }

    @Override
    protected void onCryptoOperationError(DecryptVerifyResult result) {
        final Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        mAdapter.addResult(uri, result, null, null, null);

        cryptoOperation();
    }

    @Override
    protected void onCryptoOperationSuccess(DecryptVerifyResult result) {
        final Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        Drawable icon = null;
        OnClickListener onFileClick = null, onKeyClick = null;

        if (result.getDecryptMetadata() != null && result.getDecryptMetadata().getMimeType() != null) {
            icon = loadIcon(result.getDecryptMetadata().getMimeType());
        }

        OpenPgpSignatureResult sigResult = result.getSignatureResult();
        if (sigResult != null) {
            final long keyId = sigResult.getKeyId();
            if (sigResult.getStatus() != OpenPgpSignatureResult.SIGNATURE_KEY_MISSING) {
                onKeyClick = new OnClickListener() {
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
                };
            }
        }

        if (result.success() && result.getDecryptMetadata() != null) {
            final OpenPgpMetadata metadata = result.getDecryptMetadata();
            onFileClick = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Activity activity = getActivity();
                    if (activity == null || mCurrentInputUri != null) {
                        return;
                    }

                    Uri outputUri = mOutputUris.get(uri);
                    Intent intent = new Intent();
                    intent.setDataAndType(outputUri, metadata.getMimeType());
                    activity.startActivity(intent);
                }
            };
        }

        mAdapter.addResult(uri, result, icon, onFileClick, onKeyClick);

        cryptoOperation();

    }

    @Override
    protected PgpDecryptVerifyInputParcel createOperationInput() {

        if (mCurrentInputUri == null) {
            if (mPendingInputUris.isEmpty()) {
                // nothing left to do
                return null;
            }

            mCurrentInputUri = mPendingInputUris.remove(0);
        }

        Uri currentOutputUri = mOutputUris.get(mCurrentInputUri);
        Log.d(Constants.TAG, "mInputUri=" + mCurrentInputUri + ", mOutputUri=" + currentOutputUri);

        return new PgpDecryptVerifyInputParcel(mCurrentInputUri, currentOutputUri)
                .setAllowSymmetricDecryption(true);

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (mAdapter.mMenuClickedModel == null || !mAdapter.mMenuClickedModel.hasResult()) {
            return false;
        }
        Activity activity = getActivity();
        if (activity == null) {
            return false;
        }

        ViewModel model = mAdapter.mMenuClickedModel;
        DecryptVerifyResult result = model.mResult;
        switch (menuItem.getItemId()) {
            case R.id.view_log:
                Intent intent = new Intent(activity, LogDisplayActivity.class);
                intent.putExtra(LogDisplayFragment.EXTRA_RESULT, result);
                activity.startActivity(intent);
                return true;
            case R.id.decrypt_save:
                OpenPgpMetadata metadata = result.getDecryptMetadata();
                if (metadata == null) {
                    return true;
                }
                mCurrentInputUri = model.mInputUri;
                askForOutputFilename(model.mInputUri, metadata.getFilename(), metadata.getMimeType());
                return true;
            case R.id.decrypt_delete:
                Notify.create(activity, "decrypt/delete not yet implemented", Style.ERROR).show(this);
                return true;
        }
        return false;
    }

    public static class DecryptFilesAdapter extends RecyclerView.Adapter<ViewHolder> {
        private Context mContext;
        private ArrayList<ViewModel> mDataset;
        private OnMenuItemClickListener mMenuItemClickListener;
        private ViewModel mMenuClickedModel;

        public class ViewModel {
            Context mContext;
            Uri mInputUri;
            DecryptVerifyResult mResult;
            Drawable mIcon;

            OnClickListener mOnFileClickListener;
            OnClickListener mOnKeyClickListener;

            int mProgress, mMax;
            String mProgressMsg;

            ViewModel(Context context, Uri uri) {
                mContext = context;
                mInputUri = uri;
                mProgress = 0;
                mMax = 100;
            }

            void addResult(DecryptVerifyResult result) {
                mResult = result;
            }

            void addIcon(Drawable icon) {
                mIcon = icon;
            }

            void setOnClickListeners(OnClickListener onFileClick, OnClickListener onKeyClick) {
                mOnFileClickListener = onFileClick;
                mOnKeyClickListener = onKeyClick;
            }

            boolean hasResult() {
                return mResult != null;
            }

            void setProgress(int progress, int max, String msg) {
                if (msg != null) {
                    mProgressMsg = msg;
                }
                mProgress = progress;
                mMax = max;
            }

            // Depends on inputUri only
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ViewModel viewModel = (ViewModel) o;
                return !(mResult != null ? !mResult.equals(viewModel.mResult)
                        : viewModel.mResult != null);
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
        public DecryptFilesAdapter(Context context, OnMenuItemClickListener menuItemClickListener) {
            mContext = context;
            mMenuItemClickListener = menuItemClickListener;
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

            if (model.hasResult()) {
                if (holder.vAnimator.getDisplayedChild() != 1) {
                    holder.vAnimator.setDisplayedChild(1);
                }

                KeyFormattingUtils.setStatus(mContext, holder, model.mResult);

                OpenPgpMetadata metadata = model.mResult.getDecryptMetadata();
                holder.vFilename.setText(metadata.getFilename());

                long size = metadata.getOriginalSize();
                if (size == -1 || size == 0) {
                    holder.vFilesize.setText("");
                } else {
                    holder.vFilesize.setText(FileHelper.readableFileSize(size));
                }

                // TODO thumbnail from OpenPgpMetadata
                if (model.mIcon != null) {
                    holder.vThumbnail.setImageDrawable(model.mIcon);
                } else {
                    holder.vThumbnail.setImageResource(R.drawable.ic_doc_generic_am);
                }

                holder.vFile.setOnClickListener(model.mOnFileClickListener);
                holder.vSignatureLayout.setOnClickListener(model.mOnKeyClickListener);

                holder.vContextMenu.setTag(model);
                holder.vContextMenu.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mMenuClickedModel = model;
                        PopupMenu menu = new PopupMenu(mContext, view);
                        menu.inflate(R.menu.decrypt_item_context_menu);
                        menu.setOnMenuItemClickListener(mMenuItemClickListener);
                        menu.setOnDismissListener(new OnDismissListener() {
                            @Override
                            public void onDismiss(PopupMenu popupMenu) {
                                mMenuClickedModel = null;
                            }
                        });
                        menu.show();
                    }
                });

            } else {
                if (holder.vAnimator.getDisplayedChild() != 0) {
                    holder.vAnimator.setDisplayedChild(0);
                }

                holder.vProgress.setProgress(model.mProgress);
                holder.vProgress.setMax(model.mMax);
                holder.vProgressMsg.setText(model.mProgressMsg);
            }

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }

        public void add(Uri uri) {
            ViewModel newModel = new ViewModel(mContext, uri);
            mDataset.add(newModel);
            notifyItemInserted(mDataset.size());
        }

        public void setProgress(Uri uri, int progress, int max, String msg) {
            ViewModel newModel = new ViewModel(mContext, uri);
            int pos = mDataset.indexOf(newModel);
            mDataset.get(pos).setProgress(progress, max, msg);
            notifyItemChanged(pos);
        }

        public void addResult(Uri uri, DecryptVerifyResult result, Drawable icon,
                OnClickListener onFileClick, OnClickListener onKeyClick) {

            ViewModel model = new ViewModel(mContext, uri);
            int pos = mDataset.indexOf(model);
            model = mDataset.get(pos);

            model.addResult(result);
            if (icon != null) {
                model.addIcon(icon);
            }
            model.setOnClickListeners(onFileClick, onKeyClick);

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

        public View vFile;
        public TextView vFilename;
        public TextView vFilesize;
        public ImageView vThumbnail;

        public ImageView vEncStatusIcon;
        public TextView vEncStatusText;

        public ImageView vSigStatusIcon;
        public TextView vSigStatusText;
        public View vSignatureLayout;
        public TextView vSignatureName;
        public TextView vSignatureMail;
        public TextView vSignatureAction;

        public View vContextMenu;

        public ViewHolder(View itemView) {
            super(itemView);

            vAnimator = (ViewAnimator) itemView.findViewById(R.id.view_animator);

            vProgress = (ProgressBar) itemView.findViewById(R.id.progress);
            vProgressMsg = (TextView) itemView.findViewById(R.id.progress_msg);

            vFile = itemView.findViewById(R.id.file);
            vFilename = (TextView) itemView.findViewById(R.id.filename);
            vFilesize = (TextView) itemView.findViewById(R.id.filesize);
            vThumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);

            vEncStatusIcon = (ImageView) itemView.findViewById(R.id.result_encryption_icon);
            vEncStatusText = (TextView) itemView.findViewById(R.id.result_encryption_text);

            vSigStatusIcon = (ImageView) itemView.findViewById(R.id.result_signature_icon);
            vSigStatusText = (TextView) itemView.findViewById(R.id.result_signature_text);
            vSignatureLayout = itemView.findViewById(R.id.result_signature_layout);
            vSignatureName = (TextView) itemView.findViewById(R.id.result_signature_name);
            vSignatureMail= (TextView) itemView.findViewById(R.id.result_signature_email);
            vSignatureAction = (TextView) itemView.findViewById(R.id.result_signature_action);

            vContextMenu = itemView.findViewById(R.id.context_menu);

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
        public TextView getSignatureAction() {
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

    private Drawable loadIcon(String mimeType) {
        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setType(mimeType);

        final List<ResolveInfo> matches = getActivity()
                .getPackageManager().queryIntentActivities(intent, 0);
        //noinspection LoopStatementThatDoesntLoop
        for (ResolveInfo match : matches) {
            return match.loadIcon(getActivity().getPackageManager());
        }
        return null;

    }

}

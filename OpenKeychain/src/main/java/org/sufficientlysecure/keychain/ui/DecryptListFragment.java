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


import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import android.app.Activity;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnDismissListener;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.cocosw.bottomsheet.BottomSheet;
import org.openintents.openpgp.OpenPgpMetadata;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.InputDataResult;
// this import NEEDS to be above the ViewModel AND SubViewHolder one, or it won't compile! (as of 16.09.15)
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.StatusHolder;
import org.sufficientlysecure.keychain.ui.DecryptListFragment.ViewHolder.SubViewHolder;
import org.sufficientlysecure.keychain.ui.DecryptListInterface.DecryptListView;
import org.sufficientlysecure.keychain.ui.adapter.SpacesItemDecoration;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.FileHelper;



/** Displays a list of decrypted inputs. */
public class DecryptListFragment extends Fragment implements DecryptListView, OnMenuItemClickListener {

    public static final String ARG_INPUT_URIS = "input_uris";
    public static final String ARG_CAN_DELETE = "can_delete";

    private static final int REQUEST_SAVE_DOCUMENT = 0x00007007;
    private static final int REQUEST_PERMISSION = 12;

    private DecryptListInterface mPresenter;
    private DecryptFilesAdapter mAdapter;

    boolean mShowDeleteFiles;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public static DecryptListFragment newInstance(@NonNull ArrayList<Uri> uris, boolean canDelete) {
        DecryptListFragment frag = new DecryptListFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_INPUT_URIS, uris);
        args.putBoolean(ARG_CAN_DELETE, canDelete);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_files_list_fragment, container, false);

        RecyclerView vFilesList = (RecyclerView) view.findViewById(R.id.decrypted_files_list);

        vFilesList.addItemDecoration(new SpacesItemDecoration(
                FormattingUtils.dpToPx(getActivity(), 4)));
        vFilesList.setHasFixedSize(true);
        // TODO make this a grid, for tablets!
        vFilesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        vFilesList.setItemAnimator(new DefaultItemAnimator());

        if (mAdapter == null) {
            mAdapter = new DecryptFilesAdapter();
        }
        vFilesList.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mPresenter == null) {
            mPresenter = new DecryptListPresenter();
            mPresenter.attachView(this);

            ArrayList<Uri> inputUris = getArguments().getParcelableArrayList(ARG_INPUT_URIS);
            // note the "canDelete" here is only indirectly connected to the mShowDeleteFiles field!
            boolean canDelete = getArguments().getBoolean(ARG_CAN_DELETE);
            mPresenter.initialize(inputUris, canDelete, savedInstanceState);
        } else {
            Log.d(Constants.TAG, "retained instance state!");
            mPresenter.attachView(this);
        }

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mPresenter.detachView();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mPresenter.onSaveInstanceState(outState);
   }

    @Override
    public void saveDocumentDialog(String filename, String mimeType) {
        // requires >=kitkat
        FileHelper.saveDocument(this, filename, mimeType, REQUEST_SAVE_DOCUMENT);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mPresenter.onActivityResult(requestCode, resultCode, data)) {
            return;
        }

        if (requestCode != REQUEST_SAVE_DOCUMENT) {
            super.onActivityResult(requestCode, resultCode, data);
        }

        hideKeyboard();

        // This happens after output file was selected, so start our operation
        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri saveUri = data.getData();
            mPresenter.onUiFileSelected(saveUri);
        }
    }

    @Override
    public void requestPermissions(String[] strings) {
        super.requestPermissions(strings, REQUEST_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        mPresenter.onRequestPermissionsResult(permissions, grantResults);
    }

    @Override
    public void displayFileContextMenu(final int pos, final int index) {

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        new BottomSheet.Builder(activity).sheet(R.menu.decrypt_bottom_sheet).listener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.decrypt_open:
                        mPresenter.onUiClickFileContextOpen(pos, index);
                        break;
                    case R.id.decrypt_share:
                        mPresenter.onUiClickFileContextShare(pos, index);
                        break;
                    case R.id.decrypt_save:
                        mPresenter.onUiClickFileContextSave(pos, index);
                        break;
                }
                return false;
            }
        }).grid().show();

    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        if (mAdapter.mMenuClickedModel == null) {
            return false;
        }

        switch (menuItem.getItemId()) {
            case R.id.view_log:
                mPresenter.onUiContextViewLog(mAdapter.mMenuClickedModel.mItemPosition);
                return true;
            case R.id.decrypt_delete:
                mPresenter.onUiContextDelete(mAdapter.mMenuClickedModel.mItemPosition);
                return true;
        }

        return false;
    }

    ConcurrentHashMap<Uri,Drawable> mIconCache = new ConcurrentHashMap<>();

    @Override
    public boolean iconCacheContainsKey(Uri cacheUri) {
        return mIconCache.containsKey(cacheUri);
    }

    @Override
    public void iconCachePut(Uri cacheUri, Drawable icon) {
        mIconCache.put(cacheUri, icon);
    }

    public class DecryptFilesAdapter extends RecyclerView.Adapter<ViewHolder> {
        private ArrayList<ViewModel> mDataset;
        private ViewModel mMenuClickedModel;

        public class ViewModel {
            int mItemPosition;
            InputDataResult mResult;

            int mProgress, mMax;
            String mProgressMsg;
            boolean mIsCancelled;
            boolean mProcessingKeyLookup;

            ViewModel(int itemPosition) {
                mItemPosition = itemPosition;
                mProgress = 0;
                mMax = 100;
                mIsCancelled = false;
            }

            void setResult(InputDataResult result) {
                mResult = result;
            }

            boolean hasResult() {
                return mResult != null;
            }

            void setCancelled(boolean isCancelled) {
                mIsCancelled = isCancelled;
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
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.decrypt_list_entry, parent, false);
            return new ViewHolder(view);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(ViewHolder holder, final int position) {
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final ViewModel model = mDataset.get(position);

            if (model.mIsCancelled) {
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

        private void bindItemCancelled(ViewHolder holder, final ViewModel model) {
            holder.vAnimator.setDisplayedChild(3);

            holder.vCancelledRetry.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPresenter.onUiClickRetry(model.mItemPosition);
                }
            });
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
                } else if ( ! TextUtils.isEmpty(metadata.getFilename())) {
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
                            mPresenter.onUiLongClickFile(model.mItemPosition, idx);
                            return true;
                        }
                        return false;
                    }
                });

                fileHolder.vFile.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (model.mResult.success()) {
                            mPresenter.onUiClickFile(model.mItemPosition, idx);
                        }
                    }
                });

            }

            holder.vSignatureLayout.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    mPresenter.onUiClickSignature(model.mItemPosition);
                }
            });

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
                    menu.getMenu().findItem(R.id.decrypt_delete).setEnabled(mShowDeleteFiles);
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

        public void add() {
            ViewModel newModel = new ViewModel(mDataset.size());
            mDataset.add(newModel);
            notifyItemInserted(mDataset.size());
        }

        public void setProgress(int pos, int progress, int max, String msg) {
            mDataset.get(pos).setProgress(progress, max, msg);
            notifyItemChanged(pos);
        }

        public void setCancelled(int pos, boolean isCancelled) {
            mDataset.get(pos).setCancelled(isCancelled);
            notifyItemChanged(pos);
        }

        public void setProcessingKeyLookup(int pos, boolean processingKeyLookup) {
            mDataset.get(pos).setProcessingKeyLookup(processingKeyLookup);
            notifyItemChanged(pos);
        }

        public void addResult(int pos, InputDataResult result) {
            ViewModel model = mDataset.get(pos);
            model.setResult(result);
            notifyItemChanged(pos);
        }

        public void resetItemData(int pos) {
            ViewModel model = mDataset.get(pos);
            model.setResult(null);
            model.setCancelled(false);
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
                vFilename = (TextView) itemView.findViewById(R.id.filename);
                vFilesize = (TextView) itemView.findViewById(R.id.filesize);
                vThumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);
            }
        }

        public ArrayList<SubViewHolder> mFileHolderList = new ArrayList<>();
        private int mCurrentFileListSize = 0;

        public ViewHolder(View itemView) {
            super(itemView);

            vAnimator = (ViewAnimator) itemView.findViewById(R.id.view_animator);

            vProgress = (ProgressBar) itemView.findViewById(R.id.progress);
            vProgressMsg = (TextView) itemView.findViewById(R.id.progress_msg);

            vEncStatusIcon = (ImageView) itemView.findViewById(R.id.result_encryption_icon);
            vEncStatusText = (TextView) itemView.findViewById(R.id.result_encryption_text);

            vSigStatusIcon = (ImageView) itemView.findViewById(R.id.result_signature_icon);
            vSigStatusText = (TextView) itemView.findViewById(R.id.result_signature_text);
            vSignatureLayout = itemView.findViewById(R.id.result_signature_layout);
            vSignatureName = (TextView) itemView.findViewById(R.id.result_signature_name);
            vSignatureMail= (TextView) itemView.findViewById(R.id.result_signature_email);
            vSignatureAction = (ViewAnimator) itemView.findViewById(R.id.result_signature_action);

            vFileList = (LinearLayout) itemView.findViewById(R.id.file_list);
            for (int i = 0; i < vFileList.getChildCount(); i++) {
                mFileHolderList.add(new SubViewHolder(vFileList.getChildAt(i)));
                mCurrentFileListSize += 1;
            }

            vContextMenu = itemView.findViewById(R.id.context_menu);

            vErrorMsg = (TextView) itemView.findViewById(R.id.result_error_msg);
            vErrorViewLog = (ImageView) itemView.findViewById(R.id.result_error_log);

            vCancelledRetry = (ImageView) itemView.findViewById(R.id.cancel_retry);

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

    @Override
    public void addItem() {
        mAdapter.add();
    }

    @Override
    public void setProgress(int pos, int progress, int max, String msg) {
        mAdapter.setProgress(pos, progress, max, msg);
    }

    @Override
    public void setCancelled(int pos, boolean isCancelled) {
        mAdapter.setCancelled(pos, isCancelled);
    }

    @Override
    public void setProcessingKeyLookup(int pos, boolean processingKeyLookup) {
        mAdapter.setProcessingKeyLookup(pos, processingKeyLookup);
    }

    @Override
    public void setInputDataResult(int pos, InputDataResult result) {
        mAdapter.addResult(pos, result);
    }

    @Override
    public void resetItemData(int pos) {
        mAdapter.resetItemData(pos);
    }

    @Override
    public void setShowDeleteFile(boolean showDeleteFiles) {
        mShowDeleteFiles = showDeleteFiles;
    }

    @Override
    public Fragment getFragment() {
        return this;
    }

    public void hideKeyboard() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }
        InputMethodManager inputManager = (InputMethodManager) activity
                .getSystemService(Context.INPUT_METHOD_SERVICE);

        // check if no view has focus
        View v = activity.getCurrentFocus();
        if (v == null)
            return;

        inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

}

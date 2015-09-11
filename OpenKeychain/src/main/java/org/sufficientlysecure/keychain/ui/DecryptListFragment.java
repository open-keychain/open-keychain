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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.app.Activity;
import android.content.ClipDescription;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import org.sufficientlysecure.keychain.ui.base.QueueingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.StatusHolder;
import org.sufficientlysecure.keychain.ui.DecryptListFragment.DecryptFilesAdapter.ViewModel;
import org.sufficientlysecure.keychain.ui.adapter.SpacesItemDecoration;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ParcelableHashMap;


public class DecryptListFragment
        extends QueueingCryptoOperationFragment<PgpDecryptVerifyInputParcel,DecryptVerifyResult>
        implements OnMenuItemClickListener {

    public static final String ARG_INPUT_URIS = "input_uris";
    public static final String ARG_OUTPUT_URIS = "output_uris";
    public static final String ARG_CANCELLED_URIS = "cancelled_uris";
    public static final String ARG_RESULTS = "results";

    private static final int REQUEST_CODE_OUTPUT = 0x00007007;
    public static final String ARG_CURRENT_URI = "current_uri";

    private ArrayList<Uri> mInputUris;
    private HashMap<Uri, Uri> mOutputUris;
    private ArrayList<Uri> mPendingInputUris;
    private ArrayList<Uri> mCancelledInputUris;

    private Uri mCurrentInputUri;

    private DecryptFilesAdapter mAdapter;

    /**
     * Creates new instance of this fragment
     */
    public static DecryptListFragment newInstance(ArrayList<Uri> uris) {
        DecryptListFragment frag = new DecryptListFragment();

        Bundle args = new Bundle();
        args.putParcelableArrayList(ARG_INPUT_URIS, uris);
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

        RecyclerView vFilesList = (RecyclerView) view.findViewById(R.id.decrypted_files_list);

        vFilesList.addItemDecoration(new SpacesItemDecoration(
                FormattingUtils.dpToPx(getActivity(), 4)));
        vFilesList.setHasFixedSize(true);
        // TODO make this a grid, for tablets!
        vFilesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        vFilesList.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new DecryptFilesAdapter(getActivity(), this);
        vFilesList.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(ARG_INPUT_URIS, mInputUris);

        HashMap<Uri,DecryptVerifyResult> results = new HashMap<>(mInputUris.size());
        for (Uri uri : mInputUris) {
            if (mPendingInputUris.contains(uri)) {
                continue;
            }
            DecryptVerifyResult result = mAdapter.getItemResult(uri);
            if (result != null) {
                results.put(uri, result);
            }
        }

        outState.putParcelable(ARG_RESULTS, new ParcelableHashMap<>(results));
        outState.putParcelable(ARG_OUTPUT_URIS, new ParcelableHashMap<>(mOutputUris));
        outState.putParcelableArrayList(ARG_CANCELLED_URIS, mCancelledInputUris);
        outState.putParcelable(ARG_CURRENT_URI, mCurrentInputUri);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = savedInstanceState != null ? savedInstanceState : getArguments();

        ArrayList<Uri> inputUris = getArguments().getParcelableArrayList(ARG_INPUT_URIS);
        ArrayList<Uri> cancelledUris = args.getParcelableArrayList(ARG_CANCELLED_URIS);
        ParcelableHashMap<Uri,Uri> outputUris = args.getParcelable(ARG_OUTPUT_URIS);
        ParcelableHashMap<Uri,DecryptVerifyResult> results = args.getParcelable(ARG_RESULTS);
        Uri currentInputUri = args.getParcelable(ARG_CURRENT_URI);

        displayInputUris(inputUris, currentInputUri, cancelledUris,
                outputUris != null ? outputUris.getMap() : null,
                results != null ? results.getMap() : null
        );
    }

    private void displayInputUris(ArrayList<Uri> inputUris, Uri currentInputUri,
            ArrayList<Uri> cancelledUris, HashMap<Uri,Uri> outputUris,
            HashMap<Uri,DecryptVerifyResult> results) {

        mInputUris = inputUris;
        mCurrentInputUri = currentInputUri;
        mOutputUris = outputUris != null ? outputUris : new HashMap<Uri,Uri>(inputUris.size());
        mCancelledInputUris = cancelledUris != null ? cancelledUris : new ArrayList<Uri>();

        mPendingInputUris = new ArrayList<>();

        for (final Uri uri : inputUris) {
            mAdapter.add(uri);

            if (uri.equals(mCurrentInputUri)) {
                continue;
            }

            if (mCancelledInputUris.contains(uri)) {
                mAdapter.setCancelled(uri, new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        retryUri(uri);
                    }
                });
                continue;
            }

            if (results != null && results.containsKey(uri)) {
                processResult(uri, results.get(uri));
            } else {
                mOutputUris.put(uri, TemporaryStorageProvider.createFile(getActivity()));
                mPendingInputUris.add(uri);
            }
        }

        if (mCurrentInputUri == null) {
            cryptoOperation();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OUTPUT: {
                // This happens after output file was selected, so start our operation
                if (resultCode == Activity.RESULT_OK && data != null) {
                    Uri decryptedFileUri = mOutputUris.get(mCurrentInputUri);
                    Uri saveUri = data.getData();
                    saveFile(decryptedFileUri, saveUri);
                    mCurrentInputUri = null;
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void saveFile(Uri decryptedFileUri, Uri saveUri) {
        Activity activity = getActivity();
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

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        mAdapter.setProgress(mCurrentInputUri, progress, max, msg);
        return true;
    }

    @Override
    public void onQueuedOperationError(DecryptVerifyResult result) {
        final Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        mAdapter.addResult(uri, result, null, null, null);

        cryptoOperation();
    }

    @Override
    public void onQueuedOperationSuccess(DecryptVerifyResult result) {
        Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        processResult(uri, result);

        cryptoOperation();
    }

    @Override
    public void onCryptoOperationCancelled() {
        super.onCryptoOperationCancelled();

        final Uri uri = mCurrentInputUri;
        mCurrentInputUri = null;

        mCancelledInputUris.add(uri);
        mAdapter.setCancelled(uri, new OnClickListener() {
            @Override
            public void onClick(View v) {
                retryUri(uri);
            }
        });

        cryptoOperation();

    }

    private void processResult(final Uri uri, final DecryptVerifyResult result) {

        new AsyncTask<Void, Void, Drawable>() {
            @Override
            protected Drawable doInBackground(Void... params) {

                Context context = getActivity();
                if (result.getDecryptionMetadata() == null || context == null) {
                    return null;
                }

                String type = result.getDecryptionMetadata().getMimeType();
                Uri outputUri = mOutputUris.get(uri);
                if (type == null || outputUri == null) {
                    return null;
                }

                TemporaryStorageProvider.setMimeType(context, outputUri, type);

                if (ClipDescription.compareMimeTypes(type, "image/*")) {
                    int px = FormattingUtils.dpToPx(context, 48);
                    Bitmap bitmap = FileHelper.getThumbnail(context, outputUri, new Point(px, px));
                    return new BitmapDrawable(context.getResources(), bitmap);
                }

                final Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(outputUri, type);

                final List<ResolveInfo> matches =
                        context.getPackageManager().queryIntentActivities(intent, 0);
                //noinspection LoopStatementThatDoesntLoop
                for (ResolveInfo match : matches) {
                    return match.loadIcon(getActivity().getPackageManager());
                }

                return null;

            }

            @Override
            protected void onPostExecute(Drawable icon) {
                processResult(uri, result, icon);
            }
        }.execute();

    }

    private void processResult(final Uri uri, DecryptVerifyResult result, Drawable icon) {

        OnClickListener onFileClick = null, onKeyClick = null;

        OpenPgpSignatureResult sigResult = result.getSignatureResult();
        if (sigResult != null) {
            final long keyId = sigResult.getKeyId();
            if (sigResult.getResult() != OpenPgpSignatureResult.RESULT_KEY_MISSING) {
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

        if (result.success() && result.getDecryptionMetadata() != null) {
            onFileClick = new OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayWithViewIntent(uri, false);
                }
            };
        }

        mAdapter.addResult(uri, result, icon, onFileClick, onKeyClick);

    }

    public void retryUri(Uri uri) {

        // never interrupt running operations!
        if (mCurrentInputUri != null) {
            return;
        }

        // un-cancel this one
        mCancelledInputUris.remove(uri);
        mPendingInputUris.add(uri);
        mAdapter.setCancelled(uri, null);

        cryptoOperation();

    }

    public void displayWithViewIntent(final Uri uri, boolean share) {
        Activity activity = getActivity();
        if (activity == null || mCurrentInputUri != null) {
            return;
        }

        final Uri outputUri = mOutputUris.get(uri);
        final DecryptVerifyResult result = mAdapter.getItemResult(uri);
        if (outputUri == null || result == null) {
            return;
        }

        final OpenPgpMetadata metadata = result.getDecryptionMetadata();

        // text/plain is a special case where we extract the uri content into
        // the EXTRA_TEXT extra ourselves, and display a chooser which includes
        // OpenKeychain's internal viewer
        if ("text/plain".equals(metadata.getMimeType())) {

            if (share) {
                try {
                    String plaintext = FileHelper.readTextFromUri(activity, outputUri, result.getCharset());

                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType(metadata.getMimeType());
                    intent.putExtra(Intent.EXTRA_TEXT, plaintext);
                    startActivity(intent);

                } catch (IOException e) {
                    Notify.create(activity, R.string.error_preparing_data, Style.ERROR).show();
                }

                return;
            }

            Intent intent = new Intent(activity, DisplayTextActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setDataAndType(outputUri, metadata.getMimeType());
            intent.putExtra(DisplayTextActivity.EXTRA_METADATA, result);
            activity.startActivity(intent);

        } else {

            Intent intent;
            if (share) {
                intent = new Intent(Intent.ACTION_SEND);
                intent.setType(metadata.getMimeType());
                intent.putExtra(Intent.EXTRA_STREAM, outputUri);
            } else {
                intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(outputUri, metadata.getMimeType());
            }

            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent chooserIntent = Intent.createChooser(intent, getString(R.string.intent_show));
            chooserIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            activity.startActivity(chooserIntent);
        }

    }

    @Override
    public PgpDecryptVerifyInputParcel createOperationInput() {

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
        DecryptVerifyResult result = model.mResult;
        switch (menuItem.getItemId()) {
            case R.id.view_log:
                Intent intent = new Intent(activity, LogDisplayActivity.class);
                intent.putExtra(LogDisplayFragment.EXTRA_RESULT, result);
                activity.startActivity(intent);
                return true;
            case R.id.decrypt_share:
                displayWithViewIntent(model.mInputUri, true);
                return true;
            case R.id.decrypt_save:
                OpenPgpMetadata metadata = result.getDecryptionMetadata();
                if (metadata == null) {
                    return true;
                }
                mCurrentInputUri = model.mInputUri;
                FileHelper.saveDocument(this, metadata.getFilename(), model.mInputUri, metadata.getMimeType(),
                        R.string.title_decrypt_to_file, R.string.specify_file_to_decrypt_to, REQUEST_CODE_OUTPUT);
                return true;
            case R.id.decrypt_delete:
                deleteFile(activity, model.mInputUri);
                return true;
        }
        return false;
    }

    private void deleteFile(Activity activity, Uri uri) {

        if ("file".equals(uri.getScheme())) {
            File file = new File(uri.getPath());
            if (file.delete()) {
                Notify.create(activity, R.string.file_delete_ok, Style.OK).show();
            } else {
                Notify.create(activity, R.string.file_delete_none, Style.WARN).show();
            }
            return;
        }

        if ("content".equals(uri.getScheme())) {
            try {
                int deleted = activity.getContentResolver().delete(uri, null, null);
                if (deleted > 0) {
                    Notify.create(activity, R.string.file_delete_ok, Style.OK).show();
                } else {
                    Notify.create(activity, R.string.file_delete_none, Style.WARN).show();
                }
            } catch (Exception e) {
                Log.e(Constants.TAG, "exception deleting file", e);
                Notify.create(activity, R.string.file_delete_exception, Style.ERROR).show();
            }
            return;
        }

        Notify.create(activity, R.string.file_delete_exception, Style.ERROR).show();

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
            OnClickListener mCancelled;

            ViewModel(Context context, Uri uri) {
                mContext = context;
                mInputUri = uri;
                mProgress = 0;
                mMax = 100;
                mCancelled = null;
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
                return !(mInputUri != null ? !mInputUri.equals(viewModel.mInputUri)
                        : viewModel.mInputUri != null);
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
            if (holder.vAnimator.getDisplayedChild() != 3) {
                holder.vAnimator.setDisplayedChild(3);
            }

            holder.vCancelledRetry.setOnClickListener(model.mCancelled);
        }

        private void bindItemProgress(ViewHolder holder, ViewModel model) {
            if (holder.vAnimator.getDisplayedChild() != 0) {
                holder.vAnimator.setDisplayedChild(0);
            }

            holder.vProgress.setProgress(model.mProgress);
            holder.vProgress.setMax(model.mMax);
            if (model.mProgressMsg != null) {
                holder.vProgressMsg.setText(model.mProgressMsg);
            }
        }

        private void bindItemSuccess(ViewHolder holder, final ViewModel model) {
            if (holder.vAnimator.getDisplayedChild() != 1) {
                holder.vAnimator.setDisplayedChild(1);
            }

            KeyFormattingUtils.setStatus(mContext, holder, model.mResult);

            final OpenPgpMetadata metadata = model.mResult.getDecryptionMetadata();

            String filename;
            if (metadata == null) {
                filename = mContext.getString(R.string.filename_unknown);
            } else if (TextUtils.isEmpty(metadata.getFilename())) {
                filename = mContext.getString("text/plain".equals(metadata.getMimeType())
                        ? R.string.filename_unknown_text : R.string.filename_unknown);
            } else {
                filename = metadata.getFilename();
            }
            holder.vFilename.setText(filename);

            long size = metadata == null ? 0 : metadata.getOriginalSize();
            if (size == -1 || size == 0) {
                holder.vFilesize.setText("");
            } else {
                holder.vFilesize.setText(FileHelper.readableFileSize(size));
            }

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
        }

        private void bindItemFailure(ViewHolder holder, final ViewModel model) {
            if (holder.vAnimator.getDisplayedChild() != 2) {
                holder.vAnimator.setDisplayedChild(2);
            }

            holder.vErrorMsg.setText(model.mResult.getLog().getLast().mType.getMsgId());

            holder.vErrorViewLog.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(mContext, LogDisplayActivity.class);
                    intent.putExtra(LogDisplayFragment.EXTRA_RESULT, model.mResult);
                    mContext.startActivity(intent);
                }
            });

        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }

        public DecryptVerifyResult getItemResult(Uri uri) {
            ViewModel model = new ViewModel(mContext, uri);
            int pos = mDataset.indexOf(model);
            if (pos == -1) {
                return null;
            }
            model = mDataset.get(pos);

            return model.mResult;
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

        public void setCancelled(Uri uri, OnClickListener retryListener) {
            ViewModel newModel = new ViewModel(mContext, uri);
            int pos = mDataset.indexOf(newModel);
            mDataset.get(pos).setCancelled(retryListener);
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

        public TextView vErrorMsg;
        public ImageView vErrorViewLog;

        public ImageView vCancelledRetry;

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

            vErrorMsg = (TextView) itemView.findViewById(R.id.result_error_msg);
            vErrorViewLog = (ImageView) itemView.findViewById(R.id.result_error_log);

            vCancelledRetry = (ImageView) itemView.findViewById(R.id.cancel_retry);

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

}

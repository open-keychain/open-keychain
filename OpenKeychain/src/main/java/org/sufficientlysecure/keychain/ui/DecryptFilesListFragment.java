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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.openintents.openpgp.OpenPgpMetadata;
import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler.MessageStatus;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.adapter.SpacesItemDecoration;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;

public class DecryptFilesListFragment extends CryptoOperationFragment {
    public static final String ARG_URIS = "uris";

    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    private ArrayList<Uri> mInputUris;
    private ArrayList<Uri> mPendingInputUris;
    private Uri mCurrentInputUri;

    private Uri mOutputUri = null;
    private RecyclerView mFilesList;
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

        mFilesList = (RecyclerView) view.findViewById(R.id.decrypted_files_list);

        mFilesList.addItemDecoration(new SpacesItemDecoration(
                FormattingUtils.dpToPx(getActivity(), 4)));
        mFilesList.setHasFixedSize(true);
        mFilesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFilesList.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new DecryptFilesAdapter(getActivity());
        mFilesList.setAdapter(mAdapter);

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

    private void displayInputUris(ArrayList<Uri> uris) {
        mInputUris = uris;
        mPendingInputUris = uris;
        for (Uri uri : uris) {
            mAdapter.add(uri);
        }
        cryptoOperation();
    }

    private void displayProgress(Uri uri, int progress, int max, String msg) {
        mAdapter.setProgress(uri, progress, max, msg);
    }

    private void displayInputResult(Uri uri, DecryptVerifyResult result) {
        mAdapter.addResult(uri, result);
    }

    @Override
    @SuppressLint("HandlerLeak")
    protected void cryptoOperation(CryptoInputParcel cryptoInput) {

        if (mCurrentInputUri == null) {

            if (mPendingInputUris.isEmpty()) {
                return;
            }

            mCurrentInputUri = mPendingInputUris.remove(0);

        }

        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action
        Bundle data = new Bundle();
        // use current operation, either decrypt metadata or decrypt payload
        intent.setAction(KeychainIntentService.ACTION_DECRYPT_VERIFY);

        // data

        Log.d(Constants.TAG, "mInputUri=" + mCurrentInputUri + ", mOutputUri=" + mOutputUri);

        PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel(mCurrentInputUri, mOutputUri)
                .setAllowSymmetricDecryption(true);

        data.putParcelable(KeychainIntentService.DECRYPT_VERIFY_PARCEL, input);
        data.putParcelable(KeychainIntentService.EXTRA_CRYPTO_INPUT, cryptoInput);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after decrypting is done in KeychainIntentService
        Handler saveHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                // handle pending messages
                if (handlePendingMessage(message)) {
                    return;
                }

                MessageStatus status = MessageStatus.fromInt(message.arg1);
                Bundle data = message.getData();

                switch (status) {
                    case UNKNOWN:
                    case EXCEPTION: {
                        Log.e(Constants.TAG, "error: " + status);
                        break;
                    }

                    case UPDATE_PROGRESS: {
                        int progress = data.getInt(ServiceProgressHandler.DATA_PROGRESS);
                        int max = data.getInt(ServiceProgressHandler.DATA_PROGRESS_MAX);
                        String msg;
                        if (data.containsKey(ServiceProgressHandler.DATA_MESSAGE_ID)) {
                            msg = getString(data.getInt(ServiceProgressHandler.DATA_MESSAGE_ID));
                        } else if (data.containsKey(ServiceProgressHandler.DATA_MESSAGE)) {
                            msg = data.getString(ServiceProgressHandler.DATA_MESSAGE);
                        } else {
                            msg = null;
                        }
                        displayProgress(mCurrentInputUri, progress, max, msg);
                        break;
                    }

                    case OKAY: {
                        // get returned data bundle
                        Bundle returnData = message.getData();

                        DecryptVerifyResult result =
                                returnData.getParcelable(DecryptVerifyResult.EXTRA_RESULT);

                        if (result.success()) {
                            // display signature result in activity
                            displayInputResult(mCurrentInputUri, result);
                            mCurrentInputUri = null;
                            return;
                        }

                        result.createNotify(getActivity()).show(DecryptFilesListFragment.this);
                    }
                }

            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // start service with intent
        getActivity().startService(intent);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_OUTPUT: {
                // This happens after output file was selected, so start our operation
                if (resultCode == Activity.RESULT_OK && data != null) {
                    mOutputUri = data.getData();
                    // startDecrypt();
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    public static class DecryptFilesAdapter extends RecyclerView.Adapter<ViewHolder> {
        private Context mContext;
        private ArrayList<ViewModel> mDataset;

        public static class ViewModel {
            Context mContext;
            Uri mUri;
            DecryptVerifyResult mResult;

            int mProgress, mMax;
            String mProgressMsg;

            ViewModel(Context context, Uri uri) {
                mContext = context;
                mUri = uri;
                mProgress = 0;
                mMax = 100;
            }

            void addResult(DecryptVerifyResult result) {
                mResult = result;
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
        public DecryptFilesAdapter(Context context) {
            mContext = context;
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

                OpenPgpSignatureResult signature = model.mResult.getSignatureResult();
                if (signature != null) {
                    KeyFormattingUtils.setStatusImage(mContext, holder.vStatusIcon, holder.vStatusText,
                            State.VERIFIED);
                    holder.vStatusText.setText("Yolo!");
                    holder.vSignatureName.setText(signature.getPrimaryUserId());
                } else {
                    KeyFormattingUtils.setStatusImage(mContext,
                            holder.vStatusIcon, holder.vStatusText, State.UNAVAILABLE);
                }

                OpenPgpMetadata metadata = model.mResult.getDecryptMetadata();
                holder.vFilename.setText(metadata.getFilename());

                long size = metadata.getOriginalSize();
                if (size == -1) {
                    holder.vFilesize.setText("");
                } else {
                    holder.vFilesize.setText(FileHelper.readableFileSize(size));
                }
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

        public void addResult(Uri uri, DecryptVerifyResult result) {
            ViewModel newModel = new ViewModel(mContext, uri);
            int pos = mDataset.indexOf(newModel);
            mDataset.get(pos).addResult(result);
            notifyItemChanged(pos);
        }

    }


    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewAnimator vAnimator;

        public ProgressBar vProgress;
        public TextView vProgressMsg;

        public TextView vFilename;
        public TextView vFilesize;
        public View vRemoveButton;
        public ImageView vThumbnail;

        public ImageView vStatusIcon;
        public TextView vStatusText;
        public TextView vSignatureName;
        public TextView vSignatureMail;

        public ViewHolder(View itemView) {
            super(itemView);

            vAnimator = (ViewAnimator) itemView.findViewById(R.id.view_animator);

            vProgress = (ProgressBar) itemView.findViewById(R.id.progress);
            vProgressMsg = (TextView) itemView.findViewById(R.id.progress_msg);

            vFilename = (TextView) itemView.findViewById(R.id.filename);
            vFilesize = (TextView) itemView.findViewById(R.id.filesize);
            vRemoveButton = itemView.findViewById(R.id.action_remove_file_from_list);
            vThumbnail = (ImageView) itemView.findViewById(R.id.thumbnail);

            vStatusIcon = (ImageView) itemView.findViewById(R.id.result_signature_icon);
            vStatusText = (TextView) itemView.findViewById(R.id.result_signature_text);
            vSignatureName = (TextView) itemView.findViewById(R.id.result_signature_name);
            vSignatureMail= (TextView) itemView.findViewById(R.id.result_signature_email);

        }

    }

}

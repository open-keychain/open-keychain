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
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.openintents.openpgp.OpenPgpMetadata;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.pgp.PgpDecryptVerifyInputParcel;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.DecryptFilesListFragment.DecryptFilesAdapter.ViewModel;
import org.sufficientlysecure.keychain.ui.adapter.SpacesItemDecoration;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;


public class DecryptFilesListFragment extends DecryptFragment {
    public static final String ARG_URI = "uri";

    private static final int REQUEST_CODE_OUTPUT = 0x00007007;

    private Uri mInputUri = null;
    private DecryptVerifyResult mResult;

    private Uri mOutputUri = null;
    private RecyclerView mFilesList;
    private DecryptFilesAdapter mAdapter;

    /**
     * Creates new instance of this fragment
     */
    public static DecryptFilesListFragment newInstance(Uri uri, DecryptVerifyResult result) {
        DecryptFilesListFragment frag = new DecryptFilesListFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        args.putParcelable(ARG_DECRYPT_VERIFY_RESULT, result);

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
        // mFilesList.setHasFixedSize(true);
        mFilesList.setLayoutManager(new LinearLayoutManager(getActivity()));
        mFilesList.setItemAnimator(new DefaultItemAnimator());

        mAdapter = new DecryptFilesAdapter(getActivity(), new ArrayList<ViewModel>());
        mFilesList.setAdapter(mAdapter);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_URI, mInputUri);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle state = getArguments();
        mInputUri = state.getParcelable(ARG_URI);

        if (savedInstanceState == null) {
            displayMetadata(state.<DecryptVerifyResult>getParcelable(ARG_DECRYPT_VERIFY_RESULT));
        }

    }

    private String removeEncryptedAppend(String name) {
        if (name.endsWith(Constants.FILE_EXTENSION_ASC)
                || name.endsWith(Constants.FILE_EXTENSION_PGP_MAIN)
                || name.endsWith(Constants.FILE_EXTENSION_PGP_ALTERNATE)) {
            return name.substring(0, name.length() - 4);
        }
        return name;
    }

    private void askForOutputFilename(String originalFilename) {
        if (TextUtils.isEmpty(originalFilename)) {
            originalFilename = removeEncryptedAppend(FileHelper.getFilename(getActivity(), mInputUri));
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            File file = new File(mInputUri.getPath());
            File parentDir = file.exists() ? file.getParentFile() : Constants.Path.APP_DIR;
            File targetFile = new File(parentDir, originalFilename);
            FileHelper.saveFile(this, getString(R.string.title_decrypt_to_file),
                    getString(R.string.specify_file_to_decrypt_to), targetFile, REQUEST_CODE_OUTPUT);
        } else {
            FileHelper.saveDocument(this, "*/*", originalFilename, REQUEST_CODE_OUTPUT);
        }
    }

    private void displayMetadata(DecryptVerifyResult result) {
        loadVerifyResult(result);

        OpenPgpMetadata metadata = result.getDecryptMetadata();
        mAdapter.add(metadata);
        mFilesList.requestFocus();

    }

    @Override
    @SuppressLint("HandlerLeak")
    protected void cryptoOperation(CryptoInputParcel cryptoInput) {
        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action
        Bundle data = new Bundle();
        // use current operation, either decrypt metadata or decrypt payload
        intent.setAction(KeychainIntentService.ACTION_DECRYPT_VERIFY);

        // data

        Log.d(Constants.TAG, "mInputUri=" + mInputUri + ", mOutputUri=" + mOutputUri);

        PgpDecryptVerifyInputParcel input = new PgpDecryptVerifyInputParcel(mInputUri, mOutputUri)
                .setAllowSymmetricDecryption(true);

        data.putParcelable(KeychainIntentService.DECRYPT_VERIFY_PARCEL, input);
        data.putParcelable(KeychainIntentService.EXTRA_CRYPTO_INPUT, cryptoInput);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after decrypting is done in KeychainIntentService
        ServiceProgressHandler saveHandler = new ServiceProgressHandler(
                getActivity(),
                getString(R.string.progress_decrypting),
                ProgressDialog.STYLE_HORIZONTAL,
                ProgressDialogFragment.ServiceType.KEYCHAIN_INTENT) {
            @Override
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                // handle pending messages
                if (handlePendingMessage(message)) {
                    return;
                }

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    DecryptVerifyResult result =
                            returnData.getParcelable(DecryptVerifyResult.EXTRA_RESULT);

                    if (result.success()) {
                        // display signature result in activity
                        loadVerifyResult(result);

                        /*
                        // A future open after decryption feature
                        if () {
                            Intent viewFile = new Intent(Intent.ACTION_VIEW);
                            viewFile.setInputData(mOutputUri);
                            startActivity(viewFile);
                        }
                        */
                    }
                    result.createNotify(getActivity()).show(DecryptFilesListFragment.this);
                }

            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(getActivity());

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

    @Override
    protected void onVerifyLoaded(boolean hideErrorOverlay) {

    }

    public static class DecryptFilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private Context mContext;
        private List<ViewModel> mDataset;

        public static class ViewModel {
            OpenPgpMetadata mMetadata;
            Bitmap thumbnail;

            ViewModel(Context context, OpenPgpMetadata metadata) {
                mMetadata = metadata;
                int px = FormattingUtils.dpToPx(context, 48);
                // this.thumbnail = FileHelper.getThumbnail(context, inputUri, new Point(px, px));
            }

            // Depends on inputUri only
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                ViewModel viewModel = (ViewModel) o;
                return !(mMetadata != null ? !mMetadata.equals(viewModel.mMetadata)
                        : viewModel.mMetadata != null);
            }

            // Depends on inputUri only
            @Override
            public int hashCode() {
                return mMetadata != null ? mMetadata.hashCode() : 0;
            }

            @Override
            public String toString() {
                return mMetadata.toString();
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

        // Provide a suitable constructor (depends on the kind of dataset)
        public DecryptFilesAdapter(Context context, List<ViewModel> myDataset) {
            mContext = context;
            mDataset = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //inflate your layout and pass it to view holder
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.file_list_entry, parent, false);
            return new ViewHolder(v);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            ViewHolder thisHolder = (ViewHolder) holder;
            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final ViewModel model = mDataset.get(position);

            thisHolder.filename.setText(model.mMetadata.getFilename());

            long size = model.mMetadata.getOriginalSize();
            if (size == -1) {
                thisHolder.fileSize.setText("");
            } else {
                thisHolder.fileSize.setText(FileHelper.readableFileSize(size));
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

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDataset.size();
        }

        public void add(OpenPgpMetadata metadata) {
            ViewModel newModel = new ViewModel(mContext, metadata);
            mDataset.add(newModel);
            notifyItemInserted(mDataset.size());
        }

        public void addAll(ArrayList<OpenPgpMetadata> metadatas) {
            if (metadatas != null) {
                int startIndex = mDataset.size();
                for (OpenPgpMetadata metadata : metadatas) {
                    ViewModel newModel = new ViewModel(mContext, metadata);
                    if (mDataset.contains(newModel)) {
                        Log.e(Constants.TAG, "Skipped duplicate " + metadata);
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

    }

}

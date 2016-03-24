package org.sufficientlysecure.keychain.ui.adapter;

import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.EncryptFilesFragment;
import org.sufficientlysecure.keychain.ui.EncryptFilesFragment.ViewModel;
import org.sufficientlysecure.keychain.util.FileHelper;

import java.util.ArrayList;

public class EncryptFilesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private ArrayList<ViewModel> mDataset;
    private FilesListListener mFileListListener;
    private View.OnClickListener mItemOnClickListener;
    private View.OnClickListener mFooterOnClickListener;
    private static final int TYPE_FOOTER = 0;
    private static final int TYPE_ITEM = 1;


    public interface FilesListListener {
        boolean useArmor();

        boolean encryptFilenames();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        public TextView filename;
        public TextView filenameOut;
        public TextView fileSize;
        public View removeButton;
        public View view;
        public ImageView thumbnail;


        public ViewHolder(View itemView) {
            super(itemView);
            filename = (TextView) itemView.findViewById(R.id.filename);
            filenameOut = (TextView) itemView.findViewById(R.id.filename_out);
            fileSize = (TextView) itemView.findViewById(R.id.filesize);
            removeButton = itemView.findViewById(R.id.action_remove_file_from_list);
            view = itemView;
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
    public EncryptFilesAdapter(ArrayList<ViewModel> dataset,
                               FilesListListener fileListListener,
                               View.OnClickListener onItemlickListener,
                               View.OnClickListener onFooterClickListener) {
        mDataset = dataset;
        mFileListListener = fileListListener;
        mItemOnClickListener = onItemlickListener;
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
            thisHolder.view.setTag(position);
            thisHolder.view.setOnClickListener(mItemOnClickListener);

            // - get element from your dataset at this position
            // - replace the contents of the view with that element
            final EncryptFilesFragment.ViewModel model = mDataset.get(position);

            thisHolder.filename.setText(model.filename);
            String ext = mFileListListener.useArmor()
                    ? Constants.FILE_EXTENSION_ASC : Constants.FILE_EXTENSION_PGP_MAIN;
            if (mFileListListener.encryptFilenames() && model.defaultFilenameOut) {
                thisHolder.filenameOut.setText((position + 1) + ext);
            } else {
                thisHolder.filenameOut.setText(model.filenameOut + ext);
            }

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
        // one extra for the footer!
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

    public void remove(ViewModel model) {
        int position = mDataset.indexOf(model);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }

}
package org.sufficientlysecure.keychain.ui.keyunlock.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Wizard Email Adapter
 */
public class WizardEmailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<ViewModel> mDataset;
    private View.OnClickListener mFooterOnClickListener;
    private static final int TYPE_FOOTER = 0;
    private static final int TYPE_ITEM = 1;

    public static class ViewModel {
        private String email;

        ViewModel(String email) {
            this.email = email;
        }

        @Override
        public String toString() {
            return email;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;
        public ImageButton mDeleteButton;

        public ViewHolder(View itemView) {
            super(itemView);
            mTextView = (TextView) itemView.findViewById(R.id.create_key_email_item_email);
            mDeleteButton = (ImageButton) itemView.
                    findViewById(R.id.create_key_email_item_delete_button);
        }
    }

    class FooterHolder extends RecyclerView.ViewHolder {
        public Button mAddButton;

        public FooterHolder(View itemView) {
            super(itemView);
            mAddButton = (Button) itemView.findViewById(R.id.create_key_add_email);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public WizardEmailAdapter(List<ViewModel> myDataset,
                              View.OnClickListener onFooterClickListener) {
        mDataset = myDataset;
        mFooterOnClickListener = onFooterClickListener;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.create_key_email_list_footer, parent, false);
            return new FooterHolder(v);
        } else {
            //inflate your layout and pass it to view holder
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.create_key_email_list_item, parent, false);
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

            thisHolder.mTextView.setText(model.email);
            thisHolder.mDeleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(model);
                }
            });
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

    public void add(String email) {
        mDataset.add(new ViewModel(email));
        notifyItemInserted(mDataset.size() - 1);
    }

    public void addAll(ArrayList<String> emails) {
        for (String email : emails) {
            mDataset.add(new WizardEmailAdapter.ViewModel(email));
        }
    }

    public void remove(ViewModel model) {
        int position = mDataset.indexOf(model);
        mDataset.remove(position);
        notifyItemRemoved(position);
    }
}

package org.sufficientlysecure.keychain.ui.transfer.view;


import java.util.List;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.transfer.loader.SecretKeyLoader;
import org.sufficientlysecure.keychain.ui.transfer.loader.SecretKeyLoader.SecretKeyItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.DividerItemDecoration;


public class TransferSecretKeyList extends RecyclerView {
    private OnClickTransferKeyListener onClickTransferKeyListener;

    public TransferSecretKeyList(Context context) {
        super(context);
        init(context);
    }

    public TransferSecretKeyList(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public TransferSecretKeyList(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        setLayoutManager(new LinearLayoutManager(context));
        addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));
    }

    public static class TransferKeyAdapter extends RecyclerView.Adapter<TransferKeyViewHolder> {
        private final Context context;
        private final LayoutInflater layoutInflater;
        private final OnClickTransferKeyListener onClickTransferKeyListener;

        private List<SecretKeyItem> data;


        public TransferKeyAdapter(Context context, LayoutInflater layoutInflater,
                OnClickTransferKeyListener onClickTransferKeyListener) {
            this.context = context;
            this.layoutInflater = layoutInflater;
            this.onClickTransferKeyListener = onClickTransferKeyListener;
        }

        @Override
        public TransferKeyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new TransferKeyViewHolder(layoutInflater.inflate(R.layout.key_transfer_item, parent, false));
        }

        @Override
        public void onBindViewHolder(TransferKeyViewHolder holder, int position) {
            SecretKeyItem item = data.get(position);
            holder.bind(context, item, onClickTransferKeyListener);
        }

        @Override
        public int getItemCount() {
            return data != null ? data.size() : 0;
        }

        @Override
        public long getItemId(int position) {
            return data.get(position).masterKeyId;
        }

        public void setData(List<SecretKeyItem> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        public Loader<List<SecretKeyItem>> createLoader(Context context) {
            return new SecretKeyLoader(context, context.getContentResolver());
        }
    }

    static class TransferKeyViewHolder extends RecyclerView.ViewHolder {
        private final TextView vName;
        private final TextView vEmail;
        private final TextView vCreation;
        private final View vSendButton;

        public TransferKeyViewHolder(View itemView) {
            super(itemView);

            vName = (TextView) itemView.findViewById(R.id.key_list_item_name);
            vEmail = (TextView) itemView.findViewById(R.id.key_list_item_email);
            vCreation = (TextView) itemView.findViewById(R.id.key_list_item_creation);

            vSendButton = itemView.findViewById(R.id.button_transfer);
        }

        private void bind(Context context, final SecretKeyItem item, final OnClickTransferKeyListener onClickTransferKeyListener) {
            if (item.name != null) {
                vName.setText(item.name);
                vName.setVisibility(View.VISIBLE);
            } else {
                vName.setVisibility(View.GONE);
            }
            if (item.email != null) {
                vEmail.setText(item.email);
                vEmail.setVisibility(View.VISIBLE);
            } else {
                vEmail.setVisibility(View.GONE);
            }

            String dateTime = DateUtils.formatDateTime(context, item.creationMillis,
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH);
            vCreation.setText(context.getString(R.string.label_key_created, dateTime));

            if (onClickTransferKeyListener != null) {
                vSendButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onClickTransferKeyListener.onClickTransferKey(item.masterKeyId);
                    }
                });
            } else {
                vSendButton.setOnClickListener(null);
            }
        }
    }

    public interface OnClickTransferKeyListener {
        void onClickTransferKey(long masterKeyId);
    }
}

/*
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
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

package org.sufficientlysecure.keychain.ui.transfer.view;


import java.util.ArrayList;
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
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.transfer.loader.SecretKeyLoader;
import org.sufficientlysecure.keychain.ui.transfer.loader.SecretKeyLoader.SecretKeyItem;
import org.sufficientlysecure.keychain.ui.util.recyclerview.DividerItemDecoration;


public class TransferSecretKeyList extends RecyclerView {
    private static final int STATE_INVISIBLE = 0;
    private static final int STATE_BUTTON = 1;
    private static final int STATE_PROGRESS = 2;
    private static final int STATE_TRANSFERRED = 3;
    // private static final int STATE_IMPORT_BUTTON = 4; // used in ReceivedSecretKeyList


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
        addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST, true));
        setItemAnimator(null);
    }

    public static class TransferKeyAdapter extends RecyclerView.Adapter<TransferKeyViewHolder> {
        private final Context context;
        private final LayoutInflater layoutInflater;
        private final OnClickTransferKeyListener onClickTransferKeyListener;

        private Long focusedMasterKeyId;
        private List<SecretKeyItem> data;
        private ArrayList<Long> finishedItems = new ArrayList<>();
        private boolean allItemsDisabled;


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
            boolean isFinished = finishedItems.contains(item.masterKeyId);
            holder.bind(context, item, onClickTransferKeyListener, focusedMasterKeyId, isFinished, allItemsDisabled);
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

        public void clearFinishedItems() {
            finishedItems.clear();
            notifyItemRangeChanged(0, getItemCount());
        }

        public void addToFinishedItems(long masterKeyId) {
            finishedItems.add(masterKeyId);
            // doeesn't notify, because it's non-trivial and this is called in conjunction with other refreshing things!
        }

        public void focusItem(Long masterKeyId) {
            focusedMasterKeyId = masterKeyId;
            notifyItemRangeChanged(0, getItemCount());
        }

        public Loader<List<SecretKeyItem>> createLoader(Context context) {
            return new SecretKeyLoader(context, context.getContentResolver());
        }

        public void setAllDisabled(boolean allItemsdisablde) {
            allItemsDisabled = allItemsdisablde;
            notifyItemRangeChanged(0, getItemCount());
        }
    }

    static class TransferKeyViewHolder extends RecyclerView.ViewHolder {
        private final TextView vName;
        private final TextView vEmail;
        private final TextView vCreation;
        private final View vSendButton;
        private final ViewAnimator vState;

        TransferKeyViewHolder(View itemView) {
            super(itemView);

            vName = (TextView) itemView.findViewById(R.id.key_list_item_name);
            vEmail = (TextView) itemView.findViewById(R.id.key_list_item_email);
            vCreation = (TextView) itemView.findViewById(R.id.key_list_item_creation);

            vSendButton = itemView.findViewById(R.id.button_transfer);
            vState = (ViewAnimator) itemView.findViewById(R.id.transfer_state);
        }

        private void bind(Context context, final SecretKeyItem item,
                final OnClickTransferKeyListener onClickTransferKeyListener, Long focusedMasterKeyId,
                boolean isFinished, boolean disableAll) {
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

            if (disableAll) {
                itemView.setAlpha(0.2f);
                vState.setDisplayedChild(STATE_INVISIBLE);
                vSendButton.setOnClickListener(null);
                return;
            }

            if (focusedMasterKeyId != null) {
                if (focusedMasterKeyId != item.masterKeyId) {
                    itemView.animate().alpha(0.2f).start();
                    vState.setDisplayedChild(isFinished ? STATE_TRANSFERRED : STATE_INVISIBLE);
                } else {
                    itemView.setAlpha(1.0f);
                    vState.setDisplayedChild(STATE_PROGRESS);
                }
            } else {
                itemView.animate().alpha(1.0f).start();
                vState.setDisplayedChild(isFinished ? STATE_TRANSFERRED : STATE_BUTTON);
            }

            if (focusedMasterKeyId == null && onClickTransferKeyListener != null) {
                vSendButton.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onClickTransferKeyListener.onUiClickTransferKey(item.masterKeyId);
                    }
                });
            } else {
                vSendButton.setOnClickListener(null);
            }
        }
    }

    public interface OnClickTransferKeyListener {
        void onUiClickTransferKey(long masterKeyId);
    }
}

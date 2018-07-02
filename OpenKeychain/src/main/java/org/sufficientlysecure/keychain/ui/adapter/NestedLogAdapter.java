/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

package org.sufficientlysecure.keychain.ui.adapter;


import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Pair;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;

public class NestedLogAdapter extends RecyclerView.Adapter<NestedLogAdapter.LogEntryViewHolder> {
    private static final int ENTRY_TYPE_REGULAR = 0;
    private static final int ENTRY_TYPE_SUBLOG = 1;
    private static final int LOG_ENTRY_ITEM_INDENT = 2;


    private final int mIndentFactor;
    private LogActionListener mListener;
    private List<Pair<OperationResult.LogEntryParcel, Integer>> mLogEntries;

    public NestedLogAdapter(Context context) {
        super();

        mIndentFactor = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                (float) 8, context.getResources().getDisplayMetrics());
    }

    public NestedLogAdapter(Context context, OperationResult.OperationLog log) {
        this(context);
        setLog(log);
    }

    public void setListener(LogActionListener listener) {
        mListener = listener;
    }

    public void setLog(OperationResult.OperationLog log) {
        List<OperationResult.LogEntryParcel> list = log.toList();

        if (mLogEntries != null) {
            mLogEntries.clear();
        } else {
            mLogEntries = new ArrayList<>(list.size());
        }

        int lastSection = 0;
        for (int i = 0; i < list.size(); i++) {
            OperationResult.LogEntryParcel parcel = list.get(i);
            if(parcel.mIndent < LOG_ENTRY_ITEM_INDENT) {
                lastSection = i;
            }

            mLogEntries.add(new Pair<>(parcel, lastSection));
        }

        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mLogEntries != null ? mLogEntries.size() : 0;
    }

    @Override
    public long getItemId(int position) {
        OperationResult.LogEntryParcel parcel = getItem(position);
        return parcel != null ? parcel.hashCode() : -1L;
    }

    public OperationResult.LogEntryParcel getItem(int position) {
        return mLogEntries != null ?
                mLogEntries.get(position).first : null;
    }

    public int getFirstSectionPosition(int position) {
        return mLogEntries != null ?
                mLogEntries.get(position).second : 0;
    }

    @Override
    public int getItemViewType(int position) {
        return (getItem(position) instanceof OperationResult.SubLogEntryParcel) ?
                ENTRY_TYPE_SUBLOG : ENTRY_TYPE_REGULAR;
    }

    public boolean isSection(int position) {
        return mLogEntries != null && mLogEntries.get(position).second == position;
    }

    @Override
    public LogEntryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        switch (viewType) {
            case ENTRY_TYPE_SUBLOG:
                return new SublogEntryViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.log_display_sublog_item, parent, false));

            case ENTRY_TYPE_REGULAR:
                return new LogEntryViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.log_display_regular_item, parent, false));

            default:
                return null;
        }
    }

    @Override
    public void onBindViewHolder(LogEntryViewHolder holder, int position) {
        holder.bind(getItem(position), mIndentFactor);
    }

    class LogEntryViewHolder extends RecyclerView.ViewHolder {
        private TextView mLogText;
        private ImageView mLogImg;

        public LogEntryViewHolder(View itemView) {
            super(itemView);

            mLogText = itemView.findViewById(R.id.log_text);
            mLogImg = itemView.findViewById(R.id.log_img);
        }

        public void bind(OperationResult.LogEntryParcel entry, int indentFactor) {
            String logText;
            if (entry.mParameters != null
                    && entry.mParameters.length > 0
                    && entry.mParameters[0] instanceof Integer) {

                logText = itemView.getResources().getQuantityString(entry.mType.getMsgId(),
                        (int) entry.mParameters[0], entry.mParameters);
            } else {
                logText = itemView.getResources().getString(entry.mType.getMsgId(),
                        entry.mParameters);
            }

            int textColor, indicatorColor;
            textColor = indicatorColor = FormattingUtils.getColorFromAttr(
                    itemView.getContext(), R.attr.colorText);
            
            switch (entry.mType.mLevel) {
                case DEBUG:
                    textColor = Color.GRAY;
                    indicatorColor = Color.GRAY;
                    break;
                case WARN:
                    indicatorColor = ContextCompat.getColor(itemView.getContext(),
                            R.color.android_orange_light);
                    break;
                case ERROR:
                    indicatorColor = ContextCompat.getColor(itemView.getContext(),
                            R.color.android_red_light);
                    break;
                case OK:
                    indicatorColor = ContextCompat.getColor(itemView.getContext(),
                            R.color.android_green_light);
                    break;
                case CANCELLED:
                    indicatorColor = ContextCompat.getColor(itemView.getContext(),
                            R.color.android_red_light);
                    break;
            }

            mLogText.setText(logText);
            mLogText.setTextColor(textColor);
            mLogImg.setBackgroundColor(indicatorColor);
            itemView.setPadding((entry.mIndent) * indentFactor, 0, 0, 0);
        }
    }

    class SublogEntryViewHolder extends LogEntryViewHolder implements View.OnClickListener {
        private TextView mSublogText;
        private ImageView mSublogImg;

        public SublogEntryViewHolder(View itemView) {
            super(itemView);

            itemView.setClickable(true);
            itemView.setOnClickListener(this);

            mSublogText = itemView.findViewById(R.id.log_second_text);
            mSublogImg = itemView.findViewById(R.id.log_second_img);
        }

        @Override
        public void bind(OperationResult.LogEntryParcel entry, int indentFactor) {
            super.bind(entry, indentFactor);

            OperationResult.LogEntryParcel sublogEntry = ((OperationResult.SubLogEntryParcel) entry)
                    .getSubResult().getLog().getLast();

            String logText;
            if (sublogEntry.mParameters != null
                    && sublogEntry.mParameters.length > 0
                    && sublogEntry.mParameters[0] instanceof Integer) {

                logText = itemView.getResources().getQuantityString(sublogEntry.mType.getMsgId(),
                        (int) sublogEntry.mParameters[0], sublogEntry.mParameters);
            } else {
                logText = itemView.getResources().getString(sublogEntry.mType.getMsgId(),
                        sublogEntry.mParameters);
            }

            int textColor, indicatorColor;
            textColor = indicatorColor = FormattingUtils.getColorFromAttr(
                    itemView.getContext(), R.attr.colorText);

            switch (sublogEntry.mType.mLevel) {
                case DEBUG:
                    textColor = Color.GRAY;
                    indicatorColor = Color.GRAY;
                    break;
                case WARN:
                    indicatorColor = ContextCompat.getColor(itemView.getContext(),
                            R.color.android_orange_light);
                    break;
                case ERROR:
                    indicatorColor = ContextCompat.getColor(itemView.getContext(),
                            R.color.android_red_light);
                    break;
                case OK:
                    indicatorColor = ContextCompat.getColor(itemView.getContext(),
                            R.color.android_green_light);
                    break;
                case CANCELLED:
                    indicatorColor = ContextCompat.getColor(itemView.getContext(),
                            R.color.android_red_light);
                    break;
            }

            mSublogText.setText(logText);
            mSublogText.setTextColor(textColor);
            mSublogImg.setBackgroundColor(indicatorColor);
        }

        @Override
        public void onClick(View v) {
            if (mListener != null) {
                OperationResult.LogEntryParcel parcel = getItem(getAdapterPosition());
                if (parcel instanceof OperationResult.SubLogEntryParcel) {
                    mListener.onSubEntryClicked((OperationResult.SubLogEntryParcel) parcel);
                }
            }
        }
    }

    public interface LogActionListener {
        void onSubEntryClicked(OperationResult.SubLogEntryParcel subLogEntryParcel);
    }
}

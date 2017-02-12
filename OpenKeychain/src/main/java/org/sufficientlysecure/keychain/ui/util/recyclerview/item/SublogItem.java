package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by daquexian on 17-2-10.
 */

public class SublogItem extends LogItem<SublogItem.SublogViewHolder> {

    public SublogItem(LogHeaderItem headerItem, OperationResult.LogEntryParcel entry) {
        super(headerItem, entry);
    }

    @Override
    public SublogViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new SublogViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SublogItem && mEntry.equals(((SublogItem) o).mEntry);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.log_display_sublog_item;
    }

    static class SublogViewHolder extends LogAbstractVH {
        private TextView mLogText;
        private ImageView mLogImg;
        private TextView mSublogText;
        private ImageView mSublogImg;

        private int mIntentFactor;

        SublogViewHolder(View view, FlexibleAdapter adapter) {
            this(view, adapter, false);
        }

        SublogViewHolder(View view, FlexibleAdapter adapter, boolean stickyHeader) {
            super(view, adapter, stickyHeader);

            view.setClickable(true);
            view.setOnClickListener(this);

            mLogText = (TextView) view.findViewById(R.id.log_text);
            mLogImg = (ImageView) view.findViewById(R.id.log_img);

            mSublogText = (TextView) view.findViewById(R.id.log_second_text);
            mSublogImg = (ImageView) view.findViewById(R.id.log_second_img);

            mIntentFactor = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (float) 8, view.getResources().getDisplayMetrics());
        }

        @Override
        public void bind(OperationResult.LogEntryParcel entry) {
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

            mLogText.setText(logText);
            mLogText.setTextColor(textColor);
            mLogImg.setBackgroundColor(indicatorColor);

            mSublogText.setText(logText);
            mSublogText.setTextColor(textColor);
            mSublogImg.setBackgroundColor(indicatorColor);

            getContentView().setPadding((entry.mIndent) * mIntentFactor, 0, 0, 0);
        }

        /* @Override
        public void onClick(View v) {
            if (mListener != null) {
                OperationResult.LogEntryParcel parcel = getItem(getAdapterPosition());
                if (parcel instanceof OperationResult.SubLogEntryParcel) {
                    mListener.onSubEntryClicked((OperationResult.SubLogEntryParcel) parcel);
                }
            }
        } */
    }
}

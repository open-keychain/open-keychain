package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.graphics.Color;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;

import eu.davidea.flexibleadapter.FlexibleAdapter;

/**
 * Created by daquexian on 17-2-10.
 */

public class RegularLogItem extends LogItem<RegularLogItem.RegularLogViewHolder> {
    public RegularLogItem(LogHeaderItem headerItem, OperationResult.LogEntryParcel entry) {
        super(headerItem, entry);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RegularLogItem && ((RegularLogItem) o).mEntry.equals(mEntry);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.log_display_regular_item;
    }

    @Override
    public RegularLogViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new RegularLogViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter);
    }

    static class RegularLogViewHolder extends LogAbstractVH {
        private TextView mLogText;
        private ImageView mLogImg;

        private int mIntentFactor;

        RegularLogViewHolder(View view, FlexibleAdapter adapter) {
            this(view, adapter, false);
        }

        RegularLogViewHolder(View view, FlexibleAdapter adapter, boolean stickyHeader) {
            super(view, adapter, stickyHeader);
            mLogText = (TextView) view.findViewById(R.id.log_text);
            mLogImg = (ImageView) view.findViewById(R.id.log_img);

            mIntentFactor = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                    (float) 8, view.getResources().getDisplayMetrics());
        }


        public void bind(OperationResult.LogEntryParcel entry) {
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
            getContentView().setPadding((entry.mIndent) * mIntentFactor, 0, 0, 0);

        }
    }
}

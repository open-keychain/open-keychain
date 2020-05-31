package org.sufficientlysecure.keychain.ui.widget;


import android.content.Context;
import androidx.annotation.StringRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.util.KeyInfoFormatter;

import java.util.Arrays;
import java.util.List;


class KeyChoiceSpinnerAdapter extends BaseAdapter {
    private final LayoutInflater layoutInflater;
    private final KeyInfoFormatter keyInfoFormatter;

    private Integer noneItemString;
    private List<UnifiedKeyInfo> data;

    KeyChoiceSpinnerAdapter(Context context) {
        super();

        layoutInflater = LayoutInflater.from(context);
        keyInfoFormatter = new KeyInfoFormatter(context);
    }

    public void setData(List<UnifiedKeyInfo> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public void setNoneItemString(@StringRes Integer noneItemString) {
        this.noneItemString = noneItemString;
        notifyDataSetChanged();
    }

    public boolean hasNoneItem() {
        return noneItemString != null;
    }

    @Override
    public int getCount() {
        return (data != null ? data.size() : 0) + (noneItemString != null ? 1 : 0);
    }

    public boolean isSingleEntry() {
        return data != null && data.size() == 1;
    }

    @Override
    public UnifiedKeyInfo getItem(int position) {
        if (noneItemString != null) {
            if (position == 0) {
                return null;
            }
            position -= 1;
        }
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (noneItemString != null) {
            if (position == 0) {
                return 0;
            }
            position -= 1;
        }
        return data != null ? data.get(position).master_key_id() : 0;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public int getItemViewType(int position) {
        if (noneItemString != null && position == 0) {
            return 1;
        } else {
            return super.getItemViewType(position);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (noneItemString != null) {
            if (position == 0) {
                if (convertView != null && convertView.getTag() == null) {
                    return convertView;
                } else {
                    View view = layoutInflater.inflate(R.layout.keyspinner_item_none, parent, false);
                    view.<TextView>findViewById(R.id.keyspinner_key_name).setText(noneItemString);
                    return view;
                }
            }
        }

        View view;
        KeyChoiceViewHolder viewHolder;
        if (convertView == null || !(convertView.getTag() instanceof KeyChoiceViewHolder)) {
            view = layoutInflater.inflate(R.layout.key_list_item, parent, false);
            viewHolder = new KeyChoiceViewHolder(view);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = ((KeyChoiceViewHolder) view.getTag());
        }

        UnifiedKeyInfo keyInfo = getItem(position);
        viewHolder.bind(keyInfo, isEnabled(position));

        return view;
    }

    public class KeyChoiceViewHolder {
        private TextView mMainUserId;
        private TextView mMainUserIdRest;
        private TextView mCreationDate;
        private ImageView mStatus;

        KeyChoiceViewHolder(View view) {
            mMainUserId = view.findViewById(R.id.key_list_item_name);
            mMainUserIdRest = view.findViewById(R.id.key_list_item_email);
            mStatus = view.findViewById(R.id.key_list_item_status_icon);
            mCreationDate = view.findViewById(R.id.key_list_item_creation);
        }

        public void bind(UnifiedKeyInfo keyInfo, boolean enabled) {
            keyInfoFormatter.setKeyInfo(keyInfo);
            keyInfoFormatter.formatUserId(mMainUserId, mMainUserIdRest);
            keyInfoFormatter.formatCreationDate(mCreationDate);
            keyInfoFormatter.formatStatusIcon(mStatus);
            keyInfoFormatter.greyInvalidKeys(Arrays.asList(mMainUserId, mMainUserIdRest, mCreationDate));
        }
    }
}

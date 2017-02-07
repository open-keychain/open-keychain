package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;

import java.util.ArrayList;
import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractHeaderItem;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by daquexian on 17-2-2.
 */

public class KeyHeaderItem extends BaseHeaderItem<KeyHeaderItem.ViewHolder> {
    private boolean mIsSecret;

    private static List<KeyHeaderItem> headerItemList = new ArrayList<>();

    KeyHeaderItem(Object object, String title) {
        super();
        mContextHash = object.hashCode();
        mTitle = title;
    }

    KeyHeaderItem(Object object, String title, boolean isSecret) {
        super();
        mContextHash = object.hashCode();
        mTitle = title;
        mIsSecret = isSecret;
    }

    @SuppressWarnings("unused")
    private KeyHeaderItem() {}

    /**
     * guarantee there is only one HeaderItem instance for a group, or more than one header will
     * be shown in some conditions.
     * @param object Activty or Fragment, used to distinguish header in different Activity/Fragment
     * @param title section title
     * @param isSecret whether a private key
     * @return a unique KeyHeaderItem
     */
    /* public static KeyHeaderItem getInstance(Object object, String title, boolean isSecret) {
        KeyHeaderItem newItem = new KeyHeaderItem(object, title, isSecret);
        for (KeyHeaderItem headerItem : headerItemList) {
            if (headerItem.equals(newItem)) {
                return headerItem;
            }
        }
        headerItemList.add(newItem);
        return newItem;
    } */

    @Override
    public boolean equals(Object o) {
        if (o instanceof KeyHeaderItem) {
            final KeyHeaderItem another = (KeyHeaderItem) o;
            return getContextHash() == another.getContextHash()
                    && isSecret() == another.isSecret()
                    && getTitle().equals(another.getTitle());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (getTitle().hashCode() << 1) + (isSecret() ? 1 : 0);
    }

    public boolean isSecret() {
        return mIsSecret;
    }

    public void setSecret(boolean isSecret) {
        this.mIsSecret = isSecret;
    }

    @Override
    public int getLayoutRes() {
        return mIsSecret ? R.layout.key_list_header_private : R.layout.key_list_header_public;
    }

    @Override
    public ViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, ViewHolder holder, int position, List payloads) {
        holder.bind(mTitle);
    }

    static final class ViewHolder extends FlexibleViewHolder {
        TextView textView;
        ViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter, true);
            textView = (TextView) view.findViewById(android.R.id.text1);
        }

        public void bind(String text) {
            textView.setText(text);
        }
    }
}

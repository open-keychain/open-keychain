package org.sufficientlysecure.keychain.ui.util.recyclerview.item;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;

import java.util.List;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractHeaderItem;
import eu.davidea.viewholders.FlexibleViewHolder;

/**
 * Created by daquexian on 17-2-7.
 */

public class CertHeaderItem extends BaseHeaderItem<CertHeaderItem.CertHeaderViewHolder> {
    CertHeaderItem(Object object, String title) {
        mContextHash = object.hashCode();
        mTitle = title;
    }

    @Override
    public int getLayoutRes() {
        return R.layout.view_key_adv_certs_header;
    }

    @Override
    public CertHeaderViewHolder createViewHolder(FlexibleAdapter adapter, LayoutInflater inflater, ViewGroup parent) {
        return new CertHeaderViewHolder(inflater.inflate(getLayoutRes(), parent, false), adapter);
    }

    @Override
    public void bindViewHolder(FlexibleAdapter adapter, CertHeaderViewHolder holder, int position, List payloads) {
        holder.bind(mTitle);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CertHeaderItem &&
                mContextHash == ((CertHeaderItem) o).mContextHash &&
                mTitle.equals(((CertHeaderItem) o).getTitle());
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    static final class CertHeaderViewHolder extends FlexibleViewHolder {
        private TextView mHeaderText;

        CertHeaderViewHolder(View view, FlexibleAdapter adapter) {
            super(view, adapter, true);
            mHeaderText = (TextView) itemView.findViewById(R.id.stickylist_header_text);
        }

        public void bind(String text) {
            mHeaderText.setText(text);
        }
    }
}

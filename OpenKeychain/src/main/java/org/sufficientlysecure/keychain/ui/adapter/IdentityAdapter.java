/*
 * Copyright (C) 2014-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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


import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.IdentityAdapter.ViewHolder;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityLoader.IdentityInfo;


public class IdentityAdapter extends RecyclerView.Adapter<ViewHolder> {
    private final Context context;
    private final LayoutInflater layoutInflater;
    private List<IdentityInfo> data;

    public IdentityAdapter(Context context) {
        super();
        this.layoutInflater = LayoutInflater.from(context);
        this.context = context;
    }

    public void setData(List<IdentityInfo> data) {
        this.data = data;

        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        IdentityInfo info = data.get(position);

        if (info.name != null) {
            holder.vName.setText(info.name);
        } else {
            holder.vName.setText(R.string.user_id_no_name);
        }
        if (info.email != null) {
            holder.vAddress.setText(info.email);
            holder.vAddress.setVisibility(View.VISIBLE);
        } else {
            holder.vAddress.setVisibility(View.GONE);
        }
        if (info.comment != null) {
            holder.vComment.setText(info.comment);
            holder.vComment.setVisibility(View.VISIBLE);
        } else {
            holder.vComment.setVisibility(View.GONE);
        }

        if (info.isPrimary) {
            holder.vName.setTypeface(null, Typeface.BOLD);
            holder.vAddress.setTypeface(null, Typeface.BOLD);
        } else {
            holder.vName.setTypeface(null, Typeface.NORMAL);
            holder.vAddress.setTypeface(null, Typeface.NORMAL);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(layoutInflater.inflate(R.layout.view_key_identity_user_id, null));
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private View v;

        private final TextView vName;
        private final TextView vAddress;
        private final TextView vComment;

        public ViewHolder(View view) {
            super(view);

            vName = (TextView) view.findViewById(R.id.user_id_item_name);
            vAddress = (TextView) view.findViewById(R.id.user_id_item_address);
            vComment = (TextView) view.findViewById(R.id.user_id_item_comment);
        }
    }
}

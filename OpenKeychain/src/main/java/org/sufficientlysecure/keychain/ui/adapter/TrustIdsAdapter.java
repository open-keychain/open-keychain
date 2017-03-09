/*
 * Copyright (C) 2016 Vincent Breitmoser <look@my.amazin.horse>
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


import java.util.HashMap;
import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiTrustIdentity;
import org.sufficientlysecure.keychain.ui.adapter.TrustIdsAdapter.ViewHolder;
import org.sufficientlysecure.keychain.ui.util.adapter.CursorAdapter;
import org.sufficientlysecure.keychain.ui.util.adapter.CursorAdapter.SimpleCursor;
import org.sufficientlysecure.keychain.ui.util.recyclerview.RecyclerItemClickListener;
import org.sufficientlysecure.keychain.ui.util.recyclerview.RecyclerItemClickListener.OnItemClickListener;


public class TrustIdsAdapter extends CursorAdapter<SimpleCursor, ViewHolder> {
    private static final String[] TRUST_IDS_PROJECTION = new String[] {
            ApiTrustIdentity._ID,
            ApiTrustIdentity.PACKAGE_NAME,
            ApiTrustIdentity.IDENTIFIER,
    };
    private static final int INDEX_PACKAGE_NAME = 1;
    private static final int INDEX_TRUST_ID = 2;


    private HashMap<String, Drawable> appIconCache = new HashMap<>();
    private Integer expandedPosition;
    private OnItemClickListener onItemClickListener;

    public TrustIdsAdapter(Context context, SimpleCursor simpleCursor) {
        super(context, simpleCursor, FLAG_REGISTER_CONTENT_OBSERVER);
    }

    private void launchTrustIdActivity(String packageName, String trustId, Context context) {
        try {
            Intent intent = createTrustIdActivityIntent(packageName, trustId);
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // can't help it
        }
    }

    private Intent createTrustIdActivityIntent(String packageName, String trustId) {
        Intent intent = new Intent();
        intent.setAction(packageName + ".TRUST_ID_ACTION");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(OpenPgpApi.EXTRA_TRUST_IDENTITY, trustId);
        return intent;
    }

    private boolean isTrustIdActivityAvailable(String packageName, String trustId, Context context) {
        Intent intent = createTrustIdActivityIntent(packageName, trustId);
        List<ResolveInfo> resolveInfos = context.getPackageManager().queryIntentActivities(intent, 0);
        return resolveInfos != null && !resolveInfos.isEmpty();
    }

    private Drawable getDrawableForPackageName(String packageName) {
        if (appIconCache.containsKey(packageName)) {
            return appIconCache.get(packageName);
        }

        PackageManager pm = getContext().getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);

            Drawable appIcon = pm.getApplicationIcon(ai);
            appIconCache.put(packageName, appIcon);

            return appIcon;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static CursorLoader createLoader(Context context, Uri dataUri) {
        Uri baseUri = ApiTrustIdentity.buildByKeyUri(dataUri);
        return new CursorLoader(context, baseUri, TrustIdsAdapter.TRUST_IDS_PROJECTION, null, null, null);
    }

    public void setExpandedView(Integer position) {
        if (position == null) {
            if (expandedPosition != null) {
                notifyItemChanged(expandedPosition);
            }
            expandedPosition = null;
        } else if (expandedPosition == null || !expandedPosition.equals(position)) {
            if (expandedPosition != null) {
                notifyItemChanged(expandedPosition);
            }
            expandedPosition = position;
            notifyItemChanged(position);
        }
    }

    public void setOnItemClickListener(RecyclerItemClickListener.OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.view_key_trust_id_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        moveCursorOrThrow(position);

        SimpleCursor cursor = getCursor();
        final String packageName = cursor.getString(INDEX_PACKAGE_NAME);
        final String trustId = cursor.getString(INDEX_TRUST_ID);

        Drawable drawable = getDrawableForPackageName(packageName);
        holder.vTrustId.setText(trustId);
        holder.vAppIcon.setImageDrawable(drawable);

        if (isTrustIdActivityAvailable(packageName, trustId, getContext())) {
            holder.vActionIcon.setVisibility(View.VISIBLE);
            holder.vActionIcon.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchTrustIdActivity(packageName, trustId, getContext());
                }
            });
        } else {
            holder.vActionIcon.setVisibility(View.GONE);
        }

        if (expandedPosition != null && position == expandedPosition) {
            holder.vButtonBar.setVisibility(View.VISIBLE);
        } else {
            holder.vButtonBar.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onItemClickListener != null) {
                    onItemClickListener.onItemClick(holder.itemView, position);
                }
            }
        });
    }

    public void swapCursor(Cursor data) {
        swapCursor(new SimpleCursor(data));
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView vTrustId;
        private final ImageView vAppIcon;
        private final ImageView vActionIcon;
        private final View vButtonBar;

        public ViewHolder(View view) {
            super(view);

            vTrustId = (TextView) view.findViewById(R.id.trust_id_name);
            vAppIcon = (ImageView) view.findViewById(R.id.trust_id_app_icon);
            vActionIcon = (ImageView) view.findViewById(R.id.trust_id_action);
            vButtonBar = view.findViewById(R.id.trust_id_button_bar);
        }
    }
}

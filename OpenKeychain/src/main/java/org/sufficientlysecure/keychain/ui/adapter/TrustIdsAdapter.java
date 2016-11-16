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

import android.app.Activity;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v4.content.CursorLoader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiTrustIdentity;


public class TrustIdsAdapter extends CursorAdapter {
    private static final String[] TRUST_IDS_PROJECTION = new String[] {
            ApiTrustIdentity._ID,
            ApiTrustIdentity.PACKAGE_NAME,
            ApiTrustIdentity.IDENTIFIER,
    };
    private static final int INDEX_PACKAGE_NAME = 1;
    private static final int INDEX_TRUST_ID = 2;


    protected LayoutInflater mInflater;
    private HashMap<String, Drawable> appIconCache = new HashMap<>();


    public TrustIdsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        String packageName = cursor.getString(INDEX_PACKAGE_NAME);
        String trustId = cursor.getString(INDEX_TRUST_ID);

        TextView vTrustId = (TextView) view.findViewById(R.id.trust_id_name);
        ImageView vAppIcon = (ImageView) view.findViewById(R.id.trust_id_app_icon);

        Drawable drawable = getDrawableForPackageName(packageName);
        vTrustId.setText(trustId);
        vAppIcon.setImageDrawable(drawable);
    }

    private Drawable getDrawableForPackageName(String packageName) {
        if (appIconCache.containsKey(packageName)) {
            return appIconCache.get(packageName);
        }

        PackageManager pm = mContext.getPackageManager();
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

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.view_key_trust_id_item, parent, false);
    }
}

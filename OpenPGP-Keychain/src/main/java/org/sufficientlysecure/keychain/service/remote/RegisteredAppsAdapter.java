/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.service.remote;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;

public class RegisteredAppsAdapter extends CursorAdapter {

    private LayoutInflater mInflater;
    private PackageManager pm;

    public RegisteredAppsAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);

        mInflater = LayoutInflater.from(context);
        pm = context.getApplicationContext().getPackageManager();
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView text = (TextView) view.findViewById(R.id.api_apps_adapter_item_name);
        ImageView icon = (ImageView) view.findViewById(R.id.api_apps_adapter_item_icon);

        String packageName = cursor.getString(cursor.getColumnIndex(ApiApps.PACKAGE_NAME));
        if (packageName != null) {
            // get application name
            try {
                ApplicationInfo ai = pm.getApplicationInfo(packageName, 0);

                text.setText(pm.getApplicationLabel(ai));
                icon.setImageDrawable(pm.getApplicationIcon(ai));
            } catch (final NameNotFoundException e) {
                // fallback
                text.setText(packageName);
            }
        } else {
            // fallback
            text.setText(packageName);
        }

    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return mInflater.inflate(R.layout.api_apps_adapter_list_item, null);
    }

}

/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.remote_api;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;

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

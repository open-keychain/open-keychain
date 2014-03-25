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

package org.sufficientlysecure.keychain.remote.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;
import org.sufficientlysecure.keychain.util.Log;

public class AppsListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // This is the Adapter being used to display the list's data.
    RegisteredAppsAdapter mAdapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                String selectedPackageName = mAdapter.getItemPackageName(position);
                // edit app settings
                Intent intent = new Intent(getActivity(), AppSettingsActivity.class);
                intent.setData(KeychainContract.ApiApps.buildByPackageNameUri(selectedPackageName));
                startActivity(intent);
            }
        });

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.api_no_apps));

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = new RegisteredAppsAdapter(getActivity(), null, 0);
        setListAdapter(mAdapter);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    // These are the Contacts rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
            ApiApps._ID, // 0
            ApiApps.PACKAGE_NAME // 1
    };

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        Uri baseUri = ApiApps.CONTENT_URI;

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        return new CursorLoader(getActivity(), baseUri, PROJECTION, null, null,
                ApiApps.PACKAGE_NAME + " COLLATE LOCALIZED ASC");
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    private class RegisteredAppsAdapter extends CursorAdapter {

        private LayoutInflater mInflater;
        private PackageManager mPM;

        public RegisteredAppsAdapter(Context context, Cursor c, int flags) {
            super(context, c, flags);

            mInflater = LayoutInflater.from(context);
            mPM = context.getApplicationContext().getPackageManager();
        }

        /**
         * Similar to CursorAdapter.getItemId().
         * Required to build Uris for api app view, which is not based on row ids
         *
         * @param position
         * @return
         */
        public String getItemPackageName(int position) {
            if (mDataValid && mCursor != null) {
                if (mCursor.moveToPosition(position)) {
                    return mCursor.getString(1);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView text = (TextView) view.findViewById(R.id.api_apps_adapter_item_name);
            ImageView icon = (ImageView) view.findViewById(R.id.api_apps_adapter_item_icon);

            String packageName = cursor.getString(cursor.getColumnIndex(ApiApps.PACKAGE_NAME));
            if (packageName != null) {
                // get application name
                try {
                    ApplicationInfo ai = mPM.getApplicationInfo(packageName, 0);

                    text.setText(mPM.getApplicationLabel(ai));
                    icon.setImageDrawable(mPM.getApplicationIcon(ai));
                } catch (final PackageManager.NameNotFoundException e) {
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

}

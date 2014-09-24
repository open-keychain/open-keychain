/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.CursorJoiner;
import android.database.MatrixCursor;
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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.KeychainContract.ApiApps;

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
                boolean isInstalled = mAdapter.getItemIsInstalled(position);
                String selectedPackageName = mAdapter.getItemPackageName(position);

                if (isInstalled) {
                    // edit app settings
                    Intent intent = new Intent(getActivity(), AppSettingsActivity.class);
                    intent.setData(KeychainContract.ApiApps.buildByPackageNameUri(selectedPackageName));
                    startActivity(intent);
                } else {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("market://details?id=" + selectedPackageName)));
                    } catch (ActivityNotFoundException anfe) {
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                Uri.parse("http://play.google.com/store/apps/details?id=" + selectedPackageName)));
                    }
                }
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

    private static final String TEMP_COLUMN_INSTALLED = "INSTALLED";
    private static final String TEMP_COLUMN_NAME = "NAME";

    // These are the Contacts rows that we will retrieve.
    static final String[] PROJECTION = new String[]{
            ApiApps._ID, // 0
            ApiApps.PACKAGE_NAME, // 1
            "0 as " + TEMP_COLUMN_INSTALLED, // changed later in cursor joiner
            "null as " + TEMP_COLUMN_NAME // installed apps can retrieve app name from Android OS
    };

    private static final int INDEX_ID = 0;
    private static final int INDEX_PACKAGE_NAME = 1;
    private static final int INDEX_INSTALLED = 2;
    private static final int INDEX_NAME = 3;

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
        MatrixCursor availableAppsCursor = new MatrixCursor(new String[]{
                ApiApps._ID,
                ApiApps.PACKAGE_NAME,
                TEMP_COLUMN_INSTALLED,
                TEMP_COLUMN_NAME
        });
        availableAppsCursor.addRow(new Object[]{1, "com.fsck.k9", 0, "K-9 Mail"});
        availableAppsCursor.addRow(new Object[]{1, "eu.siacs.conversations", 0, "Conversations (Instant Messaging)"});
//        availableAppsCursor.addRow(new Object[]{1, "org.sufficientlysecure.keychain.demo", 0, "API Example"});

        MatrixCursor mergedCursor = new MatrixCursor(new String[]{
                ApiApps._ID,
                ApiApps.PACKAGE_NAME,
                TEMP_COLUMN_INSTALLED,
                TEMP_COLUMN_NAME
        });

        CursorJoiner joiner = new CursorJoiner(
                availableAppsCursor,
                new String[]{ApiApps.PACKAGE_NAME},
                data,
                new String[]{ApiApps.PACKAGE_NAME});
        for (CursorJoiner.Result joinerResult : joiner) {
            switch (joinerResult) {
                case LEFT: {
                    // handle case where a row in cursorA is unique
                    mergedCursor.addRow(new Object[]{
                            availableAppsCursor.getLong(INDEX_ID),
                            availableAppsCursor.getString(INDEX_PACKAGE_NAME),
                            availableAppsCursor.getInt(INDEX_INSTALLED),
                            availableAppsCursor.getString(INDEX_NAME)
                    });
                    break;
                }
                case RIGHT: {
                    // handle case where a row in cursorB is unique
                    String packageName = data.getString(INDEX_PACKAGE_NAME);
                    int installed;
                    try {
                        getActivity().getPackageManager().getApplicationInfo(packageName, 0);
                        installed = 1;
                    } catch (final PackageManager.NameNotFoundException e) {
                        installed = 0;
                    }

                    mergedCursor.addRow(new Object[]{
                            data.getLong(INDEX_ID),
                            packageName,
                            installed,
                            null
                    });
                    break;
                }
                case BOTH: {
                    // handle case where a row with the same key is in both cursors
                    String packageName = data.getString(INDEX_PACKAGE_NAME);
                    String name;
                    int installed;
                    try {
                        getActivity().getPackageManager().getApplicationInfo(packageName, 0);
                        installed = 1;
                        name = data.getString(INDEX_NAME);
                    } catch (final PackageManager.NameNotFoundException e) {
                        installed = 0;
                        // if not installed take name from available apps list
                        name = availableAppsCursor.getString(INDEX_NAME);
                    }

                    mergedCursor.addRow(new Object[]{
                            data.getLong(INDEX_ID),
                            packageName,
                            installed,
                            name
                    });
                    break;
                }
            }
        }


        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(mergedCursor);
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
         * Required to build Uris for api apps, which are not based on row ids
         *
         * @param position
         * @return
         */
        public String getItemPackageName(int position) {
            if (mDataValid && mCursor != null) {
                if (mCursor.moveToPosition(position)) {
                    return mCursor.getString(INDEX_PACKAGE_NAME);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }

        public boolean getItemIsInstalled(int position) {
            if (mDataValid && mCursor != null) {
                if (mCursor.moveToPosition(position)) {
                    return (mCursor.getInt(INDEX_INSTALLED) == 1);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            TextView text = (TextView) view.findViewById(R.id.api_apps_adapter_item_name);
            ImageView icon = (ImageView) view.findViewById(R.id.api_apps_adapter_item_icon);
            ImageView installIcon = (ImageView) view.findViewById(R.id.api_apps_adapter_install_icon);

            String packageName = cursor.getString(INDEX_PACKAGE_NAME);
            int installed = cursor.getInt(INDEX_INSTALLED);
            String name = cursor.getString(INDEX_NAME);

            // get application name and icon
            try {
                ApplicationInfo ai = mPM.getApplicationInfo(packageName, 0);

                text.setText(mPM.getApplicationLabel(ai));
                icon.setImageDrawable(mPM.getApplicationIcon(ai));
            } catch (final PackageManager.NameNotFoundException e) {
                // fallback
                if (name == null) {
                    text.setText(packageName);
                } else {
                    text.setText(name);
                }
            }

            if (installed == 1) {
                installIcon.setVisibility(View.GONE);
            } else {
                installIcon.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return mInflater.inflate(R.layout.api_apps_adapter_list_item, null);
        }
    }

}

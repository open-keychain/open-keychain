/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.ApiApp;
import org.sufficientlysecure.keychain.provider.ApiAppDao;
import org.sufficientlysecure.keychain.remote.ui.AppsListFragment.ApiAppAdapter;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;
import org.sufficientlysecure.keychain.ui.keyview.loader.AsyncTaskLiveData;
import timber.log.Timber;


public class AppsListFragment extends RecyclerFragment<ApiAppAdapter> {
    private ApiAppAdapter adapter;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setHasOptionsMenu(true);

        adapter = new ApiAppAdapter(getActivity());
        setAdapter(adapter);
        setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        new ApiAppsLiveData(getContext()).observe(this, this::onLoad);
    }

    private void onLoad(List<ListedApp> apiApps) {
        if (apiApps == null) {
            hideList(false);
            adapter.setData(null);
            return;
        }
        adapter.setData(apiApps);
        showList(true);
    }

    public void onItemClick(int position) {
        ListedApp listedApp = adapter.data.get(position);

        if (listedApp.isInstalled) {
            if (listedApp.isRegistered) {
                // Edit app settings
                Intent intent = new Intent(getActivity(), AppSettingsActivity.class);
                intent.putExtra(AppSettingsActivity.EXTRA_PACKAGE_NAME, listedApp.packageName);
                startActivity(intent);
            } else {
                Intent i;
                PackageManager manager = getActivity().getPackageManager();
                try {
                    i = manager.getLaunchIntentForPackage(listedApp.packageName);
                    if (i == null) {
                        throw new PackageManager.NameNotFoundException();
                    }
                    // Start like the Android launcher would do
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    i.addCategory(Intent.CATEGORY_LAUNCHER);
                    startActivity(i);
                } catch (PackageManager.NameNotFoundException e) {
                    Timber.e(e, "startApp");
                }
            }
        } else {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=" + listedApp.packageName)));
            } catch (ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=" + listedApp.packageName)));
            }
        }
    }

    public class ApiAppAdapter extends Adapter<ApiAppViewHolder> {
        private final LayoutInflater inflater;

        private List<ListedApp> data;

        ApiAppAdapter(Context context) {
            super();

            inflater = LayoutInflater.from(context);
        }

        @Override
        public ApiAppViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ApiAppViewHolder(inflater.inflate(R.layout.api_apps_adapter_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(ApiAppViewHolder holder, int position) {
            ListedApp item = data.get(position);
            holder.bind(item);
        }

        @Override
        public int getItemCount() {
            return data != null ? data.size() : 0;
        }

        public void setData(List<ListedApp> data) {
            this.data = data;
            notifyDataSetChanged();
        }
    }

    public class ApiAppViewHolder extends RecyclerView.ViewHolder {
        private final TextView text;
        private final ImageView icon;
        private final ImageView installIcon;

        ApiAppViewHolder(View itemView) {
            super(itemView);

            text = itemView.findViewById(R.id.api_apps_adapter_item_name);
            icon = itemView.findViewById(R.id.api_apps_adapter_item_icon);
            installIcon = itemView.findViewById(R.id.api_apps_adapter_install_icon);
            itemView.setOnClickListener((View view) -> onItemClick(getAdapterPosition()));
        }

        void bind(ListedApp listedApp) {
            text.setText(listedApp.readableName);
            if (listedApp.applicationIconRes != null) {
                icon.setImageResource(listedApp.applicationIconRes);
            } else {
                icon.setImageDrawable(listedApp.applicationIcon);
            }
            installIcon.setVisibility(listedApp.isInstalled ? View.GONE : View.VISIBLE);
        }
    }

    public static class ApiAppsLiveData extends AsyncTaskLiveData<List<ListedApp>> {
        private final ApiAppDao apiAppDao;
        private final PackageManager packageManager;

        ApiAppsLiveData(Context context) {
            super(context, null);

            packageManager = getContext().getPackageManager();
            apiAppDao = ApiAppDao.getInstance(context);
        }

        @Override
        protected List<ListedApp> asyncLoadData() {
            ArrayList<ListedApp> result = new ArrayList<>();

            loadRegisteredApps(result);
            addPlaceholderApps(result);

            Collections.sort(result, (o1, o2) -> o1.readableName.compareTo(o2.readableName));
            return result;
        }

        private void loadRegisteredApps(ArrayList<ListedApp> result) {
            List<ApiApp> registeredApiApps = apiAppDao.getAllApiApps();

            for (ApiApp apiApp : registeredApiApps) {
                ListedApp listedApp;
                try {
                    ApplicationInfo ai = packageManager.getApplicationInfo(apiApp.package_name(), 0);
                    CharSequence applicationLabel = packageManager.getApplicationLabel(ai);
                    Drawable applicationIcon = packageManager.getApplicationIcon(ai);

                    listedApp = new ListedApp(apiApp.package_name(), true, true, applicationLabel, applicationIcon, null);
                } catch (PackageManager.NameNotFoundException e) {
                    listedApp = new ListedApp(apiApp.package_name(), false, true, apiApp.package_name(), null, null);
                }
                result.add(listedApp);
            }
        }

        private void addPlaceholderApps(ArrayList<ListedApp> result) {
            for (ListedApp placeholderApp : PLACERHOLDER_APPS) {
                if (!containsByPackageName(result, placeholderApp.packageName)) {
                    try {
                        packageManager.getApplicationInfo(placeholderApp.packageName, 0);
                        result.add(placeholderApp.withIsInstalled());
                    } catch (PackageManager.NameNotFoundException e) {
                        result.add(placeholderApp);
                    }
                }
            }
        }

        private boolean containsByPackageName(ArrayList<ListedApp> result, String packageName) {
            for (ListedApp app : result) {
                if (packageName.equals(app.packageName)) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class ListedApp {
        final String packageName;
        final boolean isInstalled;
        final boolean isRegistered;
        final String readableName;
        final Drawable applicationIcon;
        final Integer applicationIconRes;

        ListedApp(String packageName, boolean isInstalled, boolean isRegistered, CharSequence readableName,
                Drawable applicationIcon, Integer applicationIconRes) {
            this.packageName = packageName;
            this.isInstalled = isInstalled;
            this.isRegistered = isRegistered;
            this.readableName = readableName.toString();
            this.applicationIcon = applicationIcon;
            this.applicationIconRes = applicationIconRes;
        }

        public ListedApp withIsInstalled() {
            return new ListedApp(packageName, true, isRegistered, readableName, applicationIcon, applicationIconRes);
        }
    }

    private static final ListedApp[] PLACERHOLDER_APPS = {
            new ListedApp("com.fsck.k9", false, false, "K-9 Mail", null, R.drawable.apps_k9),
            new ListedApp("com.zeapo.pwdstore", false, false, "Password Store", null, R.drawable.apps_password_store),
            new ListedApp("eu.siacs.conversations", false, false, "Conversations (Instant Messaging)", null,
                    R.drawable.apps_conversations)
    };

}

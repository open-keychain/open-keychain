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


import java.util.List;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;
import android.arch.lifecycle.ViewModelProviders;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.livedata.ApiAppsLiveData;
import org.sufficientlysecure.keychain.livedata.ApiAppsLiveData.ListedApp;
import org.sufficientlysecure.keychain.remote.ui.AppsListFragment.ApiAppAdapter;
import org.sufficientlysecure.keychain.ui.base.RecyclerFragment;
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

        ApiAppsViewModel viewModel = ViewModelProviders.of(this).get(ApiAppsViewModel.class);
        viewModel.getListedAppLiveData(requireContext()).observe(this, this::onLoad);
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
                PackageManager manager = requireActivity().getPackageManager();
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

        @NonNull
        @Override
        public ApiAppViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ApiAppViewHolder(inflater.inflate(R.layout.api_apps_adapter_list_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ApiAppViewHolder holder, int position) {
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

    public static class ApiAppsViewModel extends ViewModel {
        LiveData<List<ListedApp>> listedAppLiveData;

        LiveData<List<ListedApp>> getListedAppLiveData(Context context) {
            if (listedAppLiveData == null) {
                listedAppLiveData = new ApiAppsLiveData(context);
            }
            return listedAppLiveData;
        }
    }

}

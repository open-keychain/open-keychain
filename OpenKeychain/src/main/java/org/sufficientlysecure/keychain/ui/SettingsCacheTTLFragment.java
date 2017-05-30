/*
 * Copyright (C) 2015 Vincent Breitmoser <v.breitmoser@my.amazin.horse>
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

package org.sufficientlysecure.keychain.ui;


import java.util.ArrayList;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.util.recyclerview.DividerItemDecoration;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.Preferences.CacheTTLPrefs;


public class SettingsCacheTTLFragment extends Fragment {

    public static final String ARG_TTL_PREFS = "ttl_prefs";

    private CacheTTLListAdapter mAdapter;

    public static SettingsCacheTTLFragment newInstance(CacheTTLPrefs ttlPrefs) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_TTL_PREFS, ttlPrefs);

        SettingsCacheTTLFragment fragment = new SettingsCacheTTLFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle
            savedInstanceState) {

        return inflater.inflate(R.layout.settings_cache_ttl_fragment, null);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        CacheTTLPrefs prefs = (CacheTTLPrefs) getArguments().getSerializable(ARG_TTL_PREFS);

        mAdapter = new CacheTTLListAdapter(prefs);

        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.cache_ttl_recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerView.setAdapter(mAdapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.VERTICAL_LIST,
                true));

    }

    private void savePreference() {
        FragmentActivity activity = getActivity();
        if (activity == null) {
            return;
        }
        CacheTTLPrefs prefs = mAdapter.getPrefs();
        Preferences.getPreferences(activity).setPassphraseCacheTtl(prefs);
    }

    public class CacheTTLListAdapter extends RecyclerView.Adapter<CacheTTLListAdapter.ViewHolder> {

        private final ArrayList<Boolean> mPositionIsChecked;

        public CacheTTLListAdapter(CacheTTLPrefs prefs) {
            this.mPositionIsChecked = new ArrayList<>();
            for (int ttlTime : CacheTTLPrefs.CACHE_TTLS) {
                mPositionIsChecked.add(prefs.ttlTimes.contains(ttlTime));
            }

        }

        public CacheTTLPrefs getPrefs() {
            ArrayList<String> ttls = new ArrayList<>();
            for (int i = 0; i < mPositionIsChecked.size(); i++) {
                if (mPositionIsChecked.get(i)) {
                    ttls.add(Integer.toString(CacheTTLPrefs.CACHE_TTLS.get(i)));
                }
            }
            return new CacheTTLPrefs(ttls);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.settings_cache_ttl_item, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.bind(position);
        }

        @Override
        public int getItemCount() {
            return mPositionIsChecked.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {

            CheckBox mChecked;
            TextView mTitle;

            public ViewHolder(View itemView) {
                super(itemView);
                mChecked = (CheckBox) itemView.findViewById(R.id.ttl_selected);
                mTitle = (TextView) itemView.findViewById(R.id.ttl_title);

                itemView.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mChecked.performClick();
                    }
                });
            }

            public void bind(final int position) {

                int ttl = CacheTTLPrefs.CACHE_TTLS.get(position);
                boolean isChecked = mPositionIsChecked.get(position);

                mTitle.setText(CacheTTLPrefs.CACHE_TTL_NAMES.get(ttl));
                // avoid some ui flicker by skipping unnecessary updates
                if (mChecked.isChecked() != isChecked) {
                    mChecked.setChecked(isChecked);
                }

                mChecked.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        setTtlChecked(position);
                        savePreference();
                    }
                });

            }

            private void setTtlChecked(int position) {
                boolean isChecked = mPositionIsChecked.get(position);
                int checkedItems = countCheckedItems();

                boolean isLastChecked = isChecked && checkedItems == 1;
                boolean isOneTooMany = !isChecked && checkedItems >= 3;
                if (isLastChecked) {
                    Notify.create(getActivity(), R.string.settings_cache_ttl_at_least_one, Style.ERROR).show();
                } else if (isOneTooMany) {
                    Notify.create(getActivity(), R.string.settings_cache_ttl_max_three, Style.ERROR).show();
                } else {
                    mPositionIsChecked.set(position, !isChecked);
                }
                notifyItemChanged(position);
            }

            private int countCheckedItems() {
                int result = 0;
                for (boolean isChecked : mPositionIsChecked) {
                    if (isChecked) {
                        result += 1;
                    }
                }
                return result;
            }

        }

    }
}

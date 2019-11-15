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

package org.sufficientlysecure.keychain.ui.adapter;

import android.app.Activity;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.appcompat.app.AppCompatActivity;
import android.util.SparseArray;
import android.view.ViewGroup;

import java.util.ArrayList;

public class PagerTabStripAdapter extends FragmentPagerAdapter {
    protected final Activity mActivity;
    protected final ArrayList<TabInfo> mTabs = new ArrayList<>();
    SparseArray<Fragment> registeredFragments = new SparseArray<Fragment>();

    static final class TabInfo {
        public final Class<?> clss;
        public final Bundle args;
        public final String title;

        TabInfo(Class<?> clss, Bundle args, String title) {
            this.clss = clss;
            this.args = args;
            this.title = title;
        }
    }

    public PagerTabStripAdapter(AppCompatActivity activity) {
        super(activity.getSupportFragmentManager());
        mActivity = activity;
    }

    public void addTab(Class<?> clss, Bundle args, String title) {
        TabInfo info = new TabInfo(clss, args, title);
        mTabs.add(info);
        notifyDataSetChanged();
    }

    public void removeTab(int index) {
        mTabs.remove(index);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mTabs.size();
    }

    @Override
    public Fragment getItem(int position) {
        TabInfo info = mTabs.get(position);
        return Fragment.instantiate(mActivity, info.clss.getName(), info.args);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mTabs.get(position).title;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Fragment fragment = (Fragment) super.instantiateItem(container, position);
        registeredFragments.put(position, fragment);
        return fragment;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }
}

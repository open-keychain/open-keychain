/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBarActivity;

import java.util.ArrayList;

public class PagerTabStripAdapter extends FragmentPagerAdapter {
    protected final Activity mActivity;
    protected final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();

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

    public PagerTabStripAdapter(ActionBarActivity activity) {
        super(activity.getSupportFragmentManager());
        mActivity = activity;
    }

    public void addTab(Class<?> clss, Bundle args, String title) {
        TabInfo info = new TabInfo(clss, args, title);
        mTabs.add(info);
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
}

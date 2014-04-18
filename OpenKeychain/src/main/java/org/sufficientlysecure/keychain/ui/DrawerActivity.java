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

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.FontAwesomeText;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;

public class DrawerActivity extends ActionBarActivity {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;
    private boolean mIsDrawerLocked = false;

    private Class mSelectedItem;

    private static final int MENU_ID_PREFERENCE = 222;
    private static final int MENU_ID_HELP = 223;

    protected void setupDrawerNavigation(Bundle savedInstanceState) {
        mDrawerTitle = getString(R.string.app_name);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);
        ViewGroup viewGroup = (ViewGroup) findViewById(R.id.content_frame);
        int leftMarginLoaded = ((ViewGroup.MarginLayoutParams) viewGroup.getLayoutParams()).leftMargin;
        int leftMarginInTablets = (int) getResources().getDimension(R.dimen.drawer_size);
        int errorInMarginAllowed = 5;

        // if the left margin of the loaded layout is close to the
        // one used in tablets then set drawer as open and locked
        if (Math.abs(leftMarginLoaded - leftMarginInTablets) < errorInMarginAllowed) {
            mDrawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN, mDrawerList);
            mDrawerLayout.setScrimColor(Color.TRANSPARENT);
            mIsDrawerLocked = true;
        } else {
            // set a custom shadow that overlays the main content when the drawer opens
            mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
            mIsDrawerLocked = false;
        }

        NavItem mItemIconTexts[] = new NavItem[]{
                new NavItem("fa-user", getString(R.string.nav_keys)),
                new NavItem("fa-lock", getString(R.string.nav_encrypt)),
                new NavItem("fa-unlock", getString(R.string.nav_decrypt)),
                new NavItem("fa-android", getString(R.string.nav_apps))};

        mDrawerList.setAdapter(new NavigationDrawerAdapter(this, R.layout.drawer_list_item,
                mItemIconTexts));

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // enable ActionBar app icon to behave as action to toggle nav drawer
        // if the drawer is not locked
        if (!mIsDrawerLocked) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeButtonEnabled(true);
        }

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
                mDrawerLayout, /* DrawerLayout object */
                R.drawable.ic_drawer, /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open, /* "open drawer" description for accessibility */
                R.string.drawer_close /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getSupportActionBar().setTitle(mTitle);

                callIntentForDrawerItem(mSelectedItem);
            }

            public void onDrawerOpened(View drawerView) {
                mTitle = getSupportActionBar().getTitle();
                getSupportActionBar().setTitle(mDrawerTitle);
                // creates call to onPrepareOptionsMenu()
                supportInvalidateOptionsMenu();
            }
        };

        if (!mIsDrawerLocked) {
            mDrawerLayout.setDrawerListener(mDrawerToggle);
        } else {
            // If the drawer is locked open make it un-focusable
            // so that it doesn't consume all the Back button presses
            mDrawerLayout.setFocusableInTouchMode(false);
        }
    }

    /**
     * Uses startActivity to call the Intent of the given class
     *
     * @param drawerItem the class of the drawer item you want to load. Based on Constants.DrawerItems.*
     */
    public void callIntentForDrawerItem(Class drawerItem) {
        // creates call to onPrepareOptionsMenu()
        supportInvalidateOptionsMenu();

        // call intent activity if selected
        if (drawerItem != null) {
            finish();
            overridePendingTransition(0, 0);

            Intent intent = new Intent(this, drawerItem);
            startActivity(intent);

            // disable animation of activity start
            overridePendingTransition(0, 0);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (mDrawerToggle == null) {
            return super.onCreateOptionsMenu(menu);
        }

        menu.add(42, MENU_ID_PREFERENCE, 100, R.string.menu_preferences);
        menu.add(42, MENU_ID_HELP, 101, R.string.menu_help);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle == null) {
            return super.onOptionsItemSelected(item);
        }

        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }

        switch (item.getItemId()) {
            case MENU_ID_PREFERENCE: {
                Intent intent = new Intent(this, PreferencesActivity.class);
                startActivity(intent);
                return true;
            }
            case MENU_ID_HELP: {
                Intent intent = new Intent(this, HelpActivity.class);
                startActivity(intent);
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * The click listener for ListView in the navigation drawer
     */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        // set selected class
        mSelectedItem = Constants.DrawerItems.ARRAY[position];

        // setTitle(mDrawerTitles[position]);
        // If drawer isn't locked just close the drawer and
        // it will move to the selected item by itself (via drawer toggle listener)
        if (!mIsDrawerLocked) {
            mDrawerLayout.closeDrawer(mDrawerList);
            // else move to the selected item yourself
        } else {
            callIntentForDrawerItem(mSelectedItem);
        }
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during onPostCreate() and
     * onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        if (mDrawerToggle != null) {
            mDrawerToggle.syncState();
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        if (mDrawerToggle != null) {
            mDrawerToggle.onConfigurationChanged(newConfig);
        }
    }

    private class NavItem {
        public String icon;
        public String title;

        public NavItem(String icon, String title) {
            super();
            this.icon = icon;
            this.title = title;
        }
    }

    private class NavigationDrawerAdapter extends ArrayAdapter<NavItem> {
        Context mContext;
        int mLayoutResourceId;
        NavItem mData[] = null;

        public NavigationDrawerAdapter(Context context, int layoutResourceId, NavItem[] data) {
            super(context, layoutResourceId, data);
            this.mLayoutResourceId = layoutResourceId;
            this.mContext = context;
            this.mData = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            NavItemHolder holder;

            if (row == null) {
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                row = inflater.inflate(mLayoutResourceId, parent, false);

                holder = new NavItemHolder();
                holder.mImg = (FontAwesomeText) row.findViewById(R.id.drawer_item_icon);
                holder.mTxtTitle = (TextView) row.findViewById(R.id.drawer_item_text);

                row.setTag(holder);
            } else {
                holder = (NavItemHolder) row.getTag();
            }

            NavItem item = mData[position];
            holder.mTxtTitle.setText(item.title);
            holder.mImg.setIcon(item.icon);

            return row;
        }

    }

    static class NavItemHolder {
        FontAwesomeText mImg;
        TextView mTxtTitle;
    }

}

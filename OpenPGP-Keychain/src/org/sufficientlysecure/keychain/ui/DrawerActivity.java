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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.remote.RegisteredAppsListActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.ActionProvider;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.beardedhen.androidbootstrap.FontAwesomeText;

/**
 * some fundamental ideas from https://github.com/tobykurien/SherlockNavigationDrawer
 * 
 * 
 */
public class DrawerActivity extends SherlockFragmentActivity {
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    private static Class[] mItemsClass = new Class[] { KeyListPublicActivity.class,
            EncryptActivity.class, DecryptActivity.class, ImportKeysActivity.class,
            KeyListSecretActivity.class, PreferencesActivity.class,
            RegisteredAppsListActivity.class, HelpActivity.class };

    protected void setupDrawerNavigation(Bundle savedInstanceState) {
        mDrawerTitle = getString(R.string.app_name);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        // set a custom shadow that overlays the main content when the drawer
        // opens
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
        // set up the drawer's list view with items and click listener
        // mDrawerList
        // .setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, mItemsText));

        NavItem mItemIconTexts[] = new NavItem[] {
                new NavItem("fa-user", getString(R.string.nav_contacts)),
                new NavItem("fa-lock", getString(R.string.nav_encrypt)),
                new NavItem("fa-unlock", getString(R.string.nav_decrypt)),
                new NavItem("fa-download", getString(R.string.nav_import)),
                new NavItem("fa-key", getString(R.string.nav_secret_keys)),
                new NavItem("fa-wrench", getString(R.string.nav_settings)),
                new NavItem("fa-android", getString(R.string.nav_apps)),
                new NavItem("fa-question", getString(R.string.nav_help)), };

        mDrawerList.setAdapter(new NavigationDrawerAdapter(this, R.layout.drawer_list_item,
                mItemIconTexts));

        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());

        // <com.beardedhen.androidbootstrap.FontAwesomeText
        // android:layout_width="wrap_content"
        // android:layout_height="wrap_content"
        // android:layout_margin="10dp"
        // android:textSize="32sp"
        // fontawesometext:fa_icon="fa-github" />

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);

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
                // creates call to onPrepareOptionsMenu()
                supportInvalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                mTitle = getSupportActionBar().getTitle();
                getSupportActionBar().setTitle(mDrawerTitle);
                // creates call to onPrepareOptionsMenu()
                supportInvalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // if (savedInstanceState == null) {
        // selectItem(0);
        // }
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open, hide action items related to the content
        // view
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
        // menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // The action bar home/up action should open or close the drawer.
        // ActionBarDrawerToggle will take care of this.
        if (mDrawerToggle.onOptionsItemSelected(getMenuItem(item))) {
            return true;
        }

        return super.onOptionsItemSelected(item);

        // Handle action buttons
        // switch (item.getItemId()) {
        // case R.id.action_websearch:
        // // create intent to perform web search for this planet
        // Intent intent = new Intent(Intent.ACTION_WEB_SEARCH);
        // intent.putExtra(SearchManager.QUERY, getSupportActionBar().getTitle());
        // // catch event that there's no activity to handle intent
        // if (intent.resolveActivity(getPackageManager()) != null) {
        // startActivity(intent);
        // } else {
        // Toast.makeText(this, R.string.app_not_available, Toast.LENGTH_LONG).show();
        // }
        // return true;
        // default:
        // return super.onOptionsItemSelected(item);
        // }
    }

    private android.view.MenuItem getMenuItem(final MenuItem item) {
        return new android.view.MenuItem() {
            @Override
            public int getItemId() {
                return item.getItemId();
            }

            public boolean isEnabled() {
                return true;
            }

            @Override
            public boolean collapseActionView() {
                return false;
            }

            @Override
            public boolean expandActionView() {
                return false;
            }

            @Override
            public ActionProvider getActionProvider() {
                return null;
            }

            @Override
            public View getActionView() {
                return null;
            }

            @Override
            public char getAlphabeticShortcut() {
                return 0;
            }

            @Override
            public int getGroupId() {
                return 0;
            }

            @Override
            public Drawable getIcon() {
                return null;
            }

            @Override
            public Intent getIntent() {
                return null;
            }

            @Override
            public ContextMenuInfo getMenuInfo() {
                return null;
            }

            @Override
            public char getNumericShortcut() {
                return 0;
            }

            @Override
            public int getOrder() {
                return 0;
            }

            @Override
            public SubMenu getSubMenu() {
                return null;
            }

            @Override
            public CharSequence getTitle() {
                return null;
            }

            @Override
            public CharSequence getTitleCondensed() {
                return null;
            }

            @Override
            public boolean hasSubMenu() {
                return false;
            }

            @Override
            public boolean isActionViewExpanded() {
                return false;
            }

            @Override
            public boolean isCheckable() {
                return false;
            }

            @Override
            public boolean isChecked() {
                return false;
            }

            @Override
            public boolean isVisible() {
                return false;
            }

            @Override
            public android.view.MenuItem setActionProvider(ActionProvider actionProvider) {
                return null;
            }

            @Override
            public android.view.MenuItem setActionView(View view) {
                return null;
            }

            @Override
            public android.view.MenuItem setActionView(int resId) {
                return null;
            }

            @Override
            public android.view.MenuItem setAlphabeticShortcut(char alphaChar) {
                return null;
            }

            @Override
            public android.view.MenuItem setCheckable(boolean checkable) {
                return null;
            }

            @Override
            public android.view.MenuItem setChecked(boolean checked) {
                return null;
            }

            @Override
            public android.view.MenuItem setEnabled(boolean enabled) {
                return null;
            }

            @Override
            public android.view.MenuItem setIcon(Drawable icon) {
                return null;
            }

            @Override
            public android.view.MenuItem setIcon(int iconRes) {
                return null;
            }

            @Override
            public android.view.MenuItem setIntent(Intent intent) {
                return null;
            }

            @Override
            public android.view.MenuItem setNumericShortcut(char numericChar) {
                return null;
            }

            @Override
            public android.view.MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
                return null;
            }

            @Override
            public android.view.MenuItem setOnMenuItemClickListener(
                    OnMenuItemClickListener menuItemClickListener) {
                return null;
            }

            @Override
            public android.view.MenuItem setShortcut(char numericChar, char alphaChar) {
                return null;
            }

            @Override
            public void setShowAsAction(int actionEnum) {
            }

            @Override
            public android.view.MenuItem setShowAsActionFlags(int actionEnum) {
                return null;
            }

            @Override
            public android.view.MenuItem setTitle(CharSequence title) {
                return null;
            }

            @Override
            public android.view.MenuItem setTitle(int title) {
                return null;
            }

            @Override
            public android.view.MenuItem setTitleCondensed(CharSequence title) {
                return null;
            }

            @Override
            public android.view.MenuItem setVisible(boolean visible) {
                return null;
            }
        };
    }

    /* The click listner for ListView in the navigation drawer */
    private class DrawerItemClickListener implements ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            selectItem(position);
        }
    }

    private void selectItem(int position) {
        // update selected item and title, then close the drawer
        mDrawerList.setItemChecked(position, true);
        // setTitle(mDrawerTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);

        finish();
        overridePendingTransition(0, 0);

        Intent intent = new Intent(this, mItemsClass[position]);
        startActivity(intent);
        // disable animation of activity start
        overridePendingTransition(0, 0);
    }

    /**
     * When using the ActionBarDrawerToggle, you must call it during onPostCreate() and
     * onConfigurationChanged()...
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        // Pass any configuration change to the drawer toggles
        mDrawerToggle.onConfigurationChanged(newConfig);
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
        Context context;
        int layoutResourceId;
        NavItem data[] = null;

        public NavigationDrawerAdapter(Context context, int layoutResourceId, NavItem[] data) {
            super(context, layoutResourceId, data);
            this.layoutResourceId = layoutResourceId;
            this.context = context;
            this.data = data;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View row = convertView;
            NavItemHolder holder = null;

            if (row == null) {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);

                holder = new NavItemHolder();
                holder.img = (FontAwesomeText) row.findViewById(R.id.drawer_item_icon);
                holder.txtTitle = (TextView) row.findViewById(R.id.drawer_item_text);

                row.setTag(holder);
            } else {
                holder = (NavItemHolder) row.getTag();
            }

            NavItem item = data[position];
            holder.txtTitle.setText(item.title);
            holder.img.setIcon(item.icon);

            return row;
        }

    }

    static class NavItemHolder {
        FontAwesomeText img;
        TextView txtTitle;
    }

}
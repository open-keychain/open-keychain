package org.sufficientlysecure.keychain.ui;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class EncryptDecryptActivity extends SherlockFragmentActivity {
    private FragmentActivity mActivity;
    private ActionBar mActionBar;
    private ActionBar.Tab mTab1;
    private ActionBar.Tab mTab2;
    private ActionBar.Tab mTab3;

    // @Override
    // public boolean onCreateOptionsMenu(Menu menu) {
    // MenuInflater inflater = getSupportMenuInflater();
    // inflater.inflate(R.menu.lists_activity, menu);
    // return true;
    // }

    /**
     * Menu item to go back home in ActionBar, other menu items are defined in Fragments
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(mActivity, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;

            // case R.id.menu_import:
            // ImportExportHelper.openFileStream(mActivity);
            // return true;
            //
            // case R.id.menu_export:
            // ImportExportHelper.exportLists(mActivity);
            // return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * Set up Tabs on create
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;

        setContentView(R.layout.lists_activity);

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mTab1 = getSupportActionBar().newTab();
        mTab2 = getSupportActionBar().newTab();
        mTab3 = getSupportActionBar().newTab();

        mTab1.setTabListener(new TabListener<KeyListPublicFragment>(this, "publicList",
                KeyListPublicFragment.class));
        mTab2.setTabListener(new TabListener<KeyListPublicFragment>(this, "import",
                KeyListPublicFragment.class));

        setTabTextBasedOnOrientation(getResources().getConfiguration());

        mActionBar.addTab(mTab1);
        mActionBar.addTab(mTab2);
        // mActionBar.addTab(mTab3);
    }

    private void setTabTextBasedOnOrientation(Configuration config) {
        // longer names for landscape mode or tablets
        // if (config.orientation == Configuration.ORIENTATION_LANDSCAPE
        // || config.screenLayout == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
        mTab1.setText(getString(R.string.dashboard_manage_keys));
        mTab2.setText(getString(R.string.dashboard_manage_keys));

        // } else {
        // mTab1.setText(getString(R.string.lists_tab_blacklist_short));
        // mTab2.setText(getString(R.string.lists_tab_whitelist_short));
        // mTab3.setText(getString(R.string.lists_tab_redirection_list_short));
        // }
    }

    /**
     * Change text on orientation change
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setTabTextBasedOnOrientation(newConfig);
    }

    public static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        /**
         * Constructor used each time a new tab is created.
         * 
         * @param activity
         *            The host Activity, used to instantiate the fragment
         * @param tag
         *            The identifier tag for the fragment
         * @param clz
         *            The fragment's Class, used to instantiate the fragment
         */
        public TabListener(Activity activity, String tag, Class<T> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }

        /**
         * Open Fragment based on selected Tab
         */
        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ignoredFt) {
            // bug in compatibility lib:
            // http://stackoverflow.com/questions/8645549/null-fragmenttransaction-being-passed-to-tablistener-ontabselected
            FragmentManager fragMgr = ((FragmentActivity) mActivity).getSupportFragmentManager();
            FragmentTransaction ft = fragMgr.beginTransaction();

            mFragment = Fragment.instantiate(mActivity, mClass.getName());
            ft.replace(R.id.lists_tabs_container, mFragment, mTag);
            ft.commit();
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ignoredFt) {
            FragmentManager fragMgr = ((FragmentActivity) mActivity).getSupportFragmentManager();
            FragmentTransaction ft = fragMgr.beginTransaction();

            if (mFragment != null) {
                // Remove the fragment
                ft.remove(mFragment);
            }

            ft.commit();
        }
    }

}
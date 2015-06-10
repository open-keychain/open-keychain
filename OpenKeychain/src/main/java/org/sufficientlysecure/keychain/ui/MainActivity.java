/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2015 Kai Jiang <jiangkai@gmail.com>
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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.remote.ui.AppsListFragment;
import org.sufficientlysecure.keychain.ui.base.BaseNfcActivity;
import org.sufficientlysecure.keychain.util.FabContainer;
import org.sufficientlysecure.keychain.util.Preferences;

public class MainActivity extends BaseNfcActivity implements FabContainer, OnBackStackChangedListener {

    private static final int ID_KEYS = 1;
    private static final int ID_ENCRYPT_DECRYPT = 2;
    private static final int ID_APPS = 3;
    private static final int ID_SETTINGS = 4;
    private static final int ID_HELP = 5;

    public Drawer.Result mDrawerResult;
    private Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.app_name);
        setSupportActionBar(mToolbar);

        mDrawerResult = new Drawer()
                .withActivity(this)
                .withHeader(R.layout.main_drawer_header)
                .withToolbar(mToolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.nav_keys).withIcon(CommunityMaterial.Icon.cmd_key)
                                .withIdentifier(ID_KEYS).withCheckable(false),
                        new PrimaryDrawerItem().withName(R.string.nav_encrypt_decrypt).withIcon(FontAwesome.Icon.faw_lock)
                                .withIdentifier(ID_ENCRYPT_DECRYPT).withCheckable(false),
                        new PrimaryDrawerItem().withName(R.string.title_api_registered_apps).withIcon(CommunityMaterial.Icon.cmd_apps)
                                .withIdentifier(ID_APPS).withCheckable(false)
                )
                .addStickyDrawerItems(
                        // display and stick on bottom of drawer
                        new PrimaryDrawerItem().withName(R.string.menu_preferences).withIcon(GoogleMaterial.Icon.gmd_settings).withIdentifier(ID_SETTINGS).withCheckable(false),
                        new PrimaryDrawerItem().withName(R.string.menu_help).withIcon(CommunityMaterial.Icon.cmd_help_circle).withIdentifier(ID_HELP).withCheckable(false)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            Intent intent = null;
                            switch(drawerItem.getIdentifier()) {
                                case ID_KEYS:
                                    onKeysSelected();
                                    break;
                                case ID_ENCRYPT_DECRYPT:
                                    onEnDecryptSelected();
                                    break;
                                case ID_APPS:
                                    onAppsSelected();
                                    break;
                                case ID_SETTINGS:
                                    intent = new Intent(MainActivity.this, SettingsActivity.class);
                                    break;
                                case ID_HELP:
                                    intent = new Intent(MainActivity.this, HelpActivity.class);
                                    break;
                            }
                            if (intent != null) {
                                MainActivity.this.startActivity(intent);
                            }
                        }
                    }
                })
                .withSelectedItem(-1)
                .withSavedInstance(savedInstanceState)
                .build();

        // if this is the first time show first time activity
        Preferences prefs = Preferences.getPreferences(this);
        if (prefs.isFirstTime()) {
            Intent intent = new Intent(this, CreateKeyActivity.class);
            intent.putExtra(CreateKeyActivity.EXTRA_FIRST_TIME, true);
            startActivity(intent);
            finish();
            return;
        }

        getSupportFragmentManager().addOnBackStackChangedListener(this);

        Intent data = getIntent();
        // If we got an EXTRA_RESULT in the intent, show the notification
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        }

        if (savedInstanceState == null) {
            // initialize FragmentLayout with KeyListFragment at first
            onKeysSelected();
        }

    }

    private void setFragment(Fragment fragment, boolean addToBackStack) {

        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);

        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.main_fragment_container, fragment);
        if (addToBackStack) {
            ft.addToBackStack(null);
        }
        ft.commit();

    }

    private void onKeysSelected() {
        mToolbar.setTitle(R.string.app_name);
        mDrawerResult.setSelectionByIdentifier(ID_KEYS, false);
        Fragment frag = new KeyListFragment();
        setFragment(frag, false);
    }

    private void onEnDecryptSelected() {
        mToolbar.setTitle(R.string.nav_encrypt_decrypt);
        mDrawerResult.setSelectionByIdentifier(ID_ENCRYPT_DECRYPT, false);
        Fragment frag = new EncryptDecryptOverviewFragment();
        setFragment(frag, true);
    }

    private void onAppsSelected() {
        mToolbar.setTitle(R.string.nav_apps);
        mDrawerResult.setSelectionByIdentifier(ID_APPS, false);
        Fragment frag = new AppsListFragment();
        setFragment(frag, true);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // add the values which need to be saved from the drawer to the bundle
        outState = mDrawerResult.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        // close the drawer first and if the drawer is closed do regular backstack handling
        if (mDrawerResult != null && mDrawerResult.isDrawerOpen()) {
            mDrawerResult.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void fabMoveUp(int height) {
        Object fragment = getSupportFragmentManager()
                .findFragmentById(R.id.main_fragment_container);
        if (fragment instanceof FabContainer) {
            ((FabContainer) fragment).fabMoveUp(height);
        }
    }

    @Override
    public void fabRestorePosition() {
        Object fragment = getSupportFragmentManager()
                .findFragmentById(R.id.main_fragment_container);
        if (fragment instanceof FabContainer) {
            ((FabContainer) fragment).fabRestorePosition();
        }
    }


    @Override
    public void onBackStackChanged() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager == null) {
            return;
        }
        Fragment frag = fragmentManager.findFragmentById(R.id.main_fragment_container);
        if (frag == null) {
            return;
        }

        // make sure the selected icon is the one shown at this point
        if (frag instanceof KeyListFragment) {
            mDrawerResult.setSelection(mDrawerResult.getPositionFromIdentifier(ID_KEYS), false);
        } else if (frag instanceof EncryptDecryptOverviewFragment) {
            mDrawerResult.setSelection(mDrawerResult.getPositionFromIdentifier(ID_ENCRYPT_DECRYPT), false);
        } else if (frag instanceof AppsListFragment) {
            mDrawerResult.setSelection(mDrawerResult.getPositionFromIdentifier(ID_APPS), false);
        }
    }

}

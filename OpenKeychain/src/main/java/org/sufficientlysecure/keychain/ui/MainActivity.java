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

package org.sufficientlysecure.keychain.ui;


import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.fontawesome_typeface_library.FontAwesome;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.ui.base.BaseSecurityTokenActivity;
import org.sufficientlysecure.keychain.ui.transfer.view.TransferFragment;
import org.sufficientlysecure.keychain.ui.transfer.view.TransferNotAvailableFragment;
import org.sufficientlysecure.keychain.util.FabContainer;
import org.sufficientlysecure.keychain.util.Preferences;

public class MainActivity extends BaseSecurityTokenActivity implements FabContainer {

    static final int ID_KEYS = 1;
    static final int ID_ENCRYPT_DECRYPT = 2;
    static final int ID_BACKUP = 4;
    public static final int ID_TRANSFER = 5;
    static final int ID_SETTINGS = 6;
    static final int ID_HELP = 7;
    static final int ID_SHOP = 8;
    static final int ID_AUTOCRYPT = 9;

    // both of these are used for instrumentation testing only
    public static final String EXTRA_SKIP_FIRST_TIME = "skip_first_time";
    public static final String EXTRA_INIT_FRAG = "init_frag";

    public Drawer mDrawer;
    private Toolbar mToolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.app_name);
        setSupportActionBar(mToolbar);

        mDrawer = new DrawerBuilder()
                .withActivity(this)
                .withHeader(R.layout.main_drawer_header)
                .withToolbar(mToolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.nav_shop).withIcon(CommunityMaterial.Icon.cmd_shopping)
                                .withIdentifier(ID_SHOP).withSelectable(false).withTypeface(Typeface.DEFAULT_BOLD),
                        new PrimaryDrawerItem().withName(R.string.nav_keys).withIcon(CommunityMaterial.Icon.cmd_key)
                                .withIdentifier(ID_KEYS).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.nav_encrypt_decrypt).withIcon(FontAwesome.Icon.faw_lock)
                                .withIdentifier(ID_ENCRYPT_DECRYPT).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.nav_backup).withIcon(CommunityMaterial.Icon.cmd_backup_restore)
                                .withIdentifier(ID_BACKUP).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.nav_transfer)
                                .withIcon(R.drawable.ic_wifi_lock_24dp)
                                .withIconColorRes(R.color.md_grey_600)
                                .withIconTintingEnabled(true)
                                .withIdentifier(ID_TRANSFER).withSelectable(false),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(R.string.menu_preferences).withIcon(GoogleMaterial.Icon.gmd_settings).withIdentifier(ID_SETTINGS).withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.menu_help).withIcon(CommunityMaterial.Icon.cmd_help_circle).withIdentifier(ID_HELP).withSelectable(false),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(R.string.nav_autocrypt).withIcon(GoogleMaterial.Icon.gmd_blur_on)
                                .withTypeface(Typeface.DEFAULT_BOLD).withSelectable(false).withIdentifier(ID_AUTOCRYPT)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            PrimaryDrawerItem item = (PrimaryDrawerItem) drawerItem;
                            Intent intent = null;
                            switch ((int) item.getIdentifier()) {
                                case ID_KEYS:
                                    onKeysSelected();
                                    break;
                                case ID_ENCRYPT_DECRYPT:
                                    onEnDecryptSelected();
                                    break;
                                case ID_BACKUP:
                                    onBackupSelected();
                                    break;
                                case ID_TRANSFER:
                                    onTransferSelected();
                                    break;
                                case ID_SETTINGS:
                                    intent = new Intent(MainActivity.this, SettingsActivity.class);
                                    break;
                                case ID_HELP:
                                    intent = new Intent(MainActivity.this, HelpActivity.class);
                                    break;
                                case ID_SHOP:
                                    onShopSelected();
                                    break;
                                case ID_AUTOCRYPT:
                                    onAutocryptSelected();
                                    break;
                            }
                            if (intent != null) {
                                MainActivity.this.startActivity(intent);
                            }
                        }

                        return false;
                    }
                })
                .withSelectedItem(-1)
                .withSavedInstance(savedInstanceState)
                .build();

        // if this is the first time show first time activity
        Preferences prefs = Preferences.getPreferences(this);
        if (!getIntent().getBooleanExtra(EXTRA_SKIP_FIRST_TIME, false) && prefs.isFirstTime()) {
            Intent intent = new Intent(this, CreateKeyActivity.class);
            intent.putExtra(CreateKeyActivity.EXTRA_FIRST_TIME, true);
            startActivity(intent);
            finish();
            return;
        }

        // all further initialization steps are saved as instance state
        if (savedInstanceState != null) {
            return;
        }

        Intent data = getIntent();
        // If we got an EXTRA_RESULT in the intent, show the notification
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        }

        onKeysSelected();

        if (data != null && data.hasExtra(EXTRA_INIT_FRAG)) {
            // initialize FragmentLayout with KeyListFragment at first
            switch (data.getIntExtra(EXTRA_INIT_FRAG, -1)) {
                case ID_ENCRYPT_DECRYPT:
                    onEnDecryptSelected();
                    break;
                case ID_TRANSFER:
                    onTransferSelected();
                    break;
            }
        }

    }

    @Override
    public void onNewIntent(Intent data) {
        super.onNewIntent(data);

        setIntent(data);
        if (data != null && data.hasExtra(EXTRA_INIT_FRAG)) {
            // initialize FragmentLayout with KeyListFragment at first
            switch (data.getIntExtra(EXTRA_INIT_FRAG, -1)) {
                case ID_ENCRYPT_DECRYPT:
                    onEnDecryptSelected();
                    break;
                case ID_TRANSFER:
                    onTransferSelected();
                    break;
            }
        }
    }

    private void setFragment(Fragment frag) {
        FragmentManager fragmentManager = getSupportFragmentManager();

        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.main_fragment_container, frag);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    public void onKeysSelected() {
        mToolbar.setTitle(R.string.app_name);
        mDrawer.setSelection(ID_KEYS, false);
        Fragment frag = new KeyListFragment();
        setFragment(frag);
    }

    private void onEnDecryptSelected() {
        mToolbar.setTitle(R.string.nav_encrypt_decrypt);
        mDrawer.setSelection(ID_ENCRYPT_DECRYPT, false);
        Fragment frag = new EncryptDecryptFragment();
        setFragment(frag);
    }

    private void onBackupSelected() {
        mToolbar.setTitle(R.string.nav_backup);
        mDrawer.setSelection(ID_BACKUP, false);
        Fragment frag = new BackupRestoreFragment();
        setFragment(frag);
    }

    private void onTransferSelected() {
        mToolbar.setTitle(R.string.nav_transfer);
        mDrawer.setSelection(ID_TRANSFER, false);
        if (Build.VERSION.SDK_INT < VERSION_CODES.LOLLIPOP) {
            Fragment frag = new TransferNotAvailableFragment();
            setFragment(frag);
        } else {
            Fragment frag = new TransferFragment();
            setFragment(frag);
        }
    }

    private void onShopSelected() {
        mToolbar.setTitle(R.string.shop_title);
        mDrawer.setSelection(ID_SHOP, false);
        Fragment frag = new SecurityKeyShopFragment();
        setFragment(frag);
    }

    private void onAutocryptSelected() {
        String url = "https://addons.thunderbird.net/en-US/thunderbird/addon/autocrypt/";
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // add the values which need to be saved from the drawer to the bundle
        outState = mDrawer.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        // close the drawer first and if the drawer is closed do regular backstack handling
        if (mDrawer != null && mDrawer.isDrawerOpen()) {
            mDrawer.closeDrawer();
        } else {
            FragmentManager fragmentManager = getSupportFragmentManager();
            if (fragmentManager.findFragmentById(R.id.main_fragment_container) instanceof KeyListFragment) {
                super.onBackPressed();
            } else {
                onKeysSelected();
            }
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

}

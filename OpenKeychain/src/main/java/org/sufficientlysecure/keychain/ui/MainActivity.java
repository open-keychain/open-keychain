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
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import com.mikepenz.community_material_typeface_library.CommunityMaterial;
import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.remote.ui.AppsListFragment;
import org.sufficientlysecure.keychain.util.Preferences;
import org.sufficientlysecure.keychain.util.FabContainer;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;

public class MainActivity extends ActionBarActivity implements FabContainer {

    public Drawer.Result result;

//    public Drawer.Result result;
    @Override
    public void onCreate(Bundle savedInstanceState) {
//        // don't open drawer on first run
//        disableLearningPattern();
//
////        addMultiPaneSupport();
//
//        // set the header image
//        // create and set the header
//        setDrawerHeaderImage(R.drawable.drawer_header);
//
//        // create sections
//        addSection(newSection(getString(R.string.nav_keys), R.drawable.ic_vpn_key_black_24dp, new KeyListFragment()));
//        addSection(newSection(getString(R.string.nav_encrypt_decrypt), R.drawable.ic_lock_black_24dp, new EncryptDecryptOverviewFragment()));
//        addSection(newSection(getString(R.string.title_api_registered_apps), R.drawable.ic_apps_black_24dp, new AppsListFragment()));
//
//        // create bottom section
//        addBottomSection(newSection(getString(R.string.menu_preferences), R.drawable.ic_settings_black_24dp, new Intent(this, SettingsActivity.class)));
//        addBottomSection(newSection(getString(R.string.menu_help), R.drawable.ic_help_black_24dp, new Intent(this, HelpActivity.class)));
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        final Toolbar toolbar = (Toolbar) findViewById(R.id.activity_main_toolbar);
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        result = new Drawer()
                .withActivity(this)
                .withHeader(R.layout.main_drawer_header)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.nav_keys).withIcon(CommunityMaterial.Icon.cmd_key).withIdentifier(1).withCheckable(false),
                        new PrimaryDrawerItem().withName(R.string.nav_encrypt_decrypt).withIcon(FontAwesome.Icon.faw_lock).withIdentifier(2).withCheckable(false),
                        new PrimaryDrawerItem().withName(R.string.title_api_registered_apps).withIcon(CommunityMaterial.Icon.cmd_apps).withIdentifier(3).withCheckable(false),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(R.string.menu_preferences).withIcon(GoogleMaterial.Icon.gmd_settings).withIdentifier(4).withCheckable(false),
                        new PrimaryDrawerItem().withName(R.string.menu_help).withIcon(CommunityMaterial.Icon.cmd_help_circle).withIdentifier(5).withCheckable(false)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            Intent intent = null;
                            switch(drawerItem.getIdentifier()) {
                                case 1:
                                    break;
                                case 2:
                                    break;
                                case 3:
                                    break;
                                case 4:
                                    intent = new Intent(MainActivity.this, SettingsActivity.class);
                                    break;
                                case 5:
                                    intent = new Intent(MainActivity.this, HelpActivity.class);
                                    break;
                            }
                            if (intent != null) {
                                MainActivity.this.startActivity(intent);
                            }
                        }
                        Toast.makeText(MainActivity.this, Integer.toString(drawerItem.getIdentifier()), Toast.LENGTH_SHORT).show();
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

        Intent data = getIntent();
        // If we got an EXTRA_RESULT in the intent, show the notification
        if (data != null && data.hasExtra(OperationResult.EXTRA_RESULT)) {
            OperationResult result = data.getParcelableExtra(OperationResult.EXTRA_RESULT);
            result.createNotify(this).show();
        }
    }

//    private void onPreferenceSelected() {
//        Intent intent = new Intent(this, SettingsActivity.class);
//        startActivity(intent);
//    }
//
//    private void onHelpSelected() {
//        Intent intent = new Intent(this, HelpActivity.class);
//        startActivity(intent);
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //add the values which need to be saved from the drawer to the bundle
        outState = result.saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed(){
        //handle the back press :D close the drawer first and if the drawer is closed close the activity
        if (result != null && result.isDrawerOpen()) {
            result.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void fabMoveUp(int height) {
//        Object fragment = getCurrentSection().getTargetFragment();
//        if (fragment instanceof FabContainer) {
//            ((FabContainer) fragment).fabMoveUp(height);
//        }
    }

    @Override
    public void fabRestorePosition() {
//        Object fragment = getCurrentSection().getTargetFragment();
//        if (fragment instanceof FabContainer) {
//            ((FabContainer) fragment).fabRestorePosition();
//        }
    }

}

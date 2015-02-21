/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.remote.ui.AppsListFragment;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;

public abstract class NavDrawerActivity extends MaterialNavigationDrawer {

    @Override
    public void init(Bundle savedInstanceState) {
        // don't open drawer on first run
        disableLearningPattern();

//        addMultiPaneSupport();

        // set the header image
        // create and set the header
        setDrawerHeaderImage(R.drawable.drawer_header);

        // create sections
        addSection(newSection(getString(R.string.nav_keys), R.drawable.ic_vpn_key_black_24dp, new KeyListFragment()));
        addSection(newSection(getString(R.string.nav_encrypt_decrypt), R.drawable.ic_lock_black_24dp, new EncryptDecryptOverviewFragment()));
        addSection(newSection(getString(R.string.title_api_registered_apps), R.drawable.ic_apps_black_24dp, new AppsListFragment()));

        // create bottom section
        addBottomSection(newSection(getString(R.string.menu_preferences), R.drawable.ic_settings_black_24dp, new Intent(this, SettingsActivity.class)));
        addBottomSection(newSection(getString(R.string.menu_help), R.drawable.ic_help_black_24dp, new Intent(this, HelpActivity.class)));
    }
}

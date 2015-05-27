/*
 * Copyright (C) 2012-2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.intents.OpenKeychainIntents;

import java.util.ArrayList;

public class EncryptFilesActivity extends EncryptActivity {

    // Intents
    public static final String ACTION_ENCRYPT_DATA = OpenKeychainIntents.ENCRYPT_DATA;

    // enables ASCII Armor for file encryption when uri is given
    public static final String EXTRA_ASCII_ARMOR = OpenKeychainIntents.ENCRYPT_EXTRA_ASCII_ARMOR;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        }, false);

        Intent intent = getIntent();
        String action = intent.getAction();
        Bundle extras = intent.getExtras();
        String type = intent.getType();
        ArrayList<Uri> uris = new ArrayList<>();

        if (extras == null) {
            extras = new Bundle();
        }

        if (intent.getData() != null) {
            uris.add(intent.getData());
        }

        // When sending to OpenKeychain Encrypt via share menu
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            // Files via content provider, override uri and action
            uris.clear();
            uris.add(intent.<Uri>getParcelableExtra(Intent.EXTRA_STREAM));
        }

        if (Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
        }

        boolean useArmor = extras.getBoolean(EXTRA_ASCII_ARMOR, false);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

            EncryptFilesFragment encryptFragment = EncryptFilesFragment.newInstance(uris, useArmor);
            transaction.replace(R.id.encrypt_file_container, encryptFragment);
            transaction.commit();
        }

    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.encrypt_files_activity);
    }

}

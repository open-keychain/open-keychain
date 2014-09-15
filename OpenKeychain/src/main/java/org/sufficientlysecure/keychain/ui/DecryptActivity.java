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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import org.sufficientlysecure.keychain.R;

public class DecryptActivity extends DrawerActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.decrypt_activity);

        activateDrawerNavigation(savedInstanceState);

        View actionFile = findViewById(R.id.decrypt_files);
        View actionFromClipboard = findViewById(R.id.decrypt_from_clipboard);

        actionFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent filesDecrypt = new Intent(DecryptActivity.this, DecryptFilesActivity.class);
                filesDecrypt.setAction(DecryptFilesActivity.ACTION_DECRYPT_DATA_OPEN);
                startActivity(filesDecrypt);
            }
        });

        actionFromClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent clipboardDecrypt = new Intent(DecryptActivity.this, DecryptTextActivity.class);
                clipboardDecrypt.setAction(DecryptTextActivity.ACTION_DECRYPT_FROM_CLIPBOARD);
                startActivity(clipboardDecrypt);
            }
        });
    }
}

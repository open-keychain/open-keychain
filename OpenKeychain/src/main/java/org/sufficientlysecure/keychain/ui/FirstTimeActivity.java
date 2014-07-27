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
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.Window;
import android.widget.Button;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.util.Log;

public class FirstTimeActivity extends ActionBarActivity {

    Button mCreateKey;
    Button mImportKey;
    Button mSkipSetup;

    public static final int REQUEST_CODE_CREATE_OR_IMPORT_KEY = 0x00007012;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.first_time_activity);

        mCreateKey = (Button) findViewById(R.id.first_time_create_key);
        mImportKey = (Button) findViewById(R.id.first_time_import_key);
        mSkipSetup = (Button) findViewById(R.id.first_time_cancel);

        mSkipSetup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishSetup();
            }
        });

        mImportKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FirstTimeActivity.this, ImportKeysActivity.class);
                intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_FILE_AND_RETURN);
                startActivityForResult(intent, REQUEST_CODE_CREATE_OR_IMPORT_KEY);
            }
        });

        mCreateKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(FirstTimeActivity.this, CreateKeyActivity.class);
                startActivityForResult(intent, REQUEST_CODE_CREATE_OR_IMPORT_KEY);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CREATE_OR_IMPORT_KEY) {
            if (resultCode == RESULT_OK) {
                finishSetup();
            }
        } else {
            Log.e(Constants.TAG, "No valid request code!");
        }
    }

    private void finishSetup() {
        Preferences prefs = Preferences.getPreferences(this);
        prefs.setFirstTime(false);
        Intent intent = new Intent(FirstTimeActivity.this, KeyListActivity.class);
        startActivity(intent);
        finish();
    }
}

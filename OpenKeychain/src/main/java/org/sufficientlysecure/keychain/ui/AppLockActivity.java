/*
 * Copyright (C) 2016 Alex Fong Jie Wen <alexfongg@gmail.com>
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
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.passphrasedialog.PassphraseDialogActivity;

public class AppLockActivity extends AppCompatActivity {
    private static final int REQUEST_FOR_PASSPHRASE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.applock_activity);
    }

    @Override
    protected void onStart() {
        super.onStart();
        askForPassphrase();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void askForPassphrase() {
        Intent intent = new Intent(this, PassphraseDialogActivity.class);
        intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT,
                RequiredInputParcel.createRequiredAppLockPassphrase());
        startActivityForResult(intent, REQUEST_FOR_PASSPHRASE);

    }

    @Override
    public void onBackPressed() {
        ActivityCompat.finishAffinity(this);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_FOR_PASSPHRASE: {
                if (resultCode == RESULT_OK) {
                    finish();
                } else {
                    ActivityCompat.finishAffinity(this);
                }
                break;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }
}

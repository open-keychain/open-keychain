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
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.MenuItem;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.passphrasedialog.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.util.Passphrase;

public class SettingsMasterPassphraseActivity extends BaseActivity {
    private static final int REQUEST_ASK_MASTER_PASSPHRASE = 2;
    private Passphrase mPassphrase;
    private boolean mFinishedCollectingPassphrase;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        askForMasterPassphrase();
    }

    private void askForMasterPassphrase() {
        Intent intent = new Intent(this, PassphraseDialogActivity.class);
        RequiredInputParcel requiredInput =
                RequiredInputParcel.createRequiredAppLockPassphrase();
        requiredInput.mSkipCaching = true;
        intent.putExtra(PassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
        startActivityForResult(intent, REQUEST_ASK_MASTER_PASSPHRASE);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.settings_change_master_passphrase);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ASK_MASTER_PASSPHRASE: {
                if (resultCode != RESULT_OK) {
                    this.finish();
                    return;
                }
                CryptoInputParcel cryptoResult =
                        data.getParcelableExtra(PassphraseDialogActivity.RESULT_CRYPTO_INPUT);
                mPassphrase = cryptoResult.getPassphrase();
                mFinishedCollectingPassphrase = true;
                break;
            }
            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (mFinishedCollectingPassphrase) {
            showSetPassphraseFragment(mPassphrase);
        }

        mFinishedCollectingPassphrase = false;
    }

    public void showSetPassphraseFragment(Passphrase passphrase) {
        Fragment frag = SetMasterPassphraseFragment.newInstance(true, passphrase);
        FragmentManager fragMan = getSupportFragmentManager();
        fragMan.beginTransaction()
                .setCustomAnimations(0, 0)
                .add(R.id.settings_master_passphrase_fragment, frag)
                .commit();
    }
}

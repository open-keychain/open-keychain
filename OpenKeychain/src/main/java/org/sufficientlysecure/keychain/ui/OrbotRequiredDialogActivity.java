/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Adithya Abraham Philip <adithyaphilip@gmail.com>
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
import android.support.v4.app.FragmentActivity;

import org.sufficientlysecure.keychain.compatibility.DialogFragmentWorkaround;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.orbot.OrbotHelper;

/**
 * Simply encapsulates a dialog. If orbot is not installed, it shows an install dialog, else a
 * dialog to enable orbot.
 */
public class OrbotRequiredDialogActivity extends FragmentActivity
        implements OrbotHelper.DialogActions {

    // if suppplied and true will start Orbot directly without showing dialog
    public static final String EXTRA_START_ORBOT = "start_orbot";

    // to provide any previous crypto input into which proxy preference is merged
    public static final String EXTRA_CRYPTO_INPUT = "extra_crypto_input";

    public static final String RESULT_CRYPTO_INPUT = "result_crypto_input";

    private CryptoInputParcel mCryptoInputParcel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mCryptoInputParcel = getIntent().getParcelableExtra(EXTRA_CRYPTO_INPUT);
        if (mCryptoInputParcel == null) {
            // compatibility with usages that don't use a CryptoInputParcel
            mCryptoInputParcel = new CryptoInputParcel();
        }

        boolean startOrbotDirect = getIntent().getBooleanExtra(EXTRA_START_ORBOT, false);
        if (startOrbotDirect) {
            OrbotHelper.bestPossibleOrbotStart(this, this);
        } else {
            showDialog();
        }
    }

    /**
     * Displays an install or start orbot dialog (or silent orbot start) depending on orbot's
     * presence and state
     */
    public void showDialog() {
        DialogFragmentWorkaround.INTERFACE.runnableRunDelayed(new Runnable() {
            public void run() {

                if (OrbotHelper.putOrbotInRequiredState(OrbotRequiredDialogActivity.this,
                        OrbotRequiredDialogActivity.this)) {
                    // no action required after all
                    onOrbotStarted();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case OrbotHelper.START_TOR_RESULT: {
                onOrbotStarted(); // assumption that orbot was started, no way to tell for sure
            }
        }
    }

    @Override
    public void onOrbotStarted() {
        Intent intent = new Intent();
        // send back unmodified CryptoInputParcel for a retry
        intent.putExtra(RESULT_CRYPTO_INPUT, mCryptoInputParcel);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onNeutralButton() {
        Intent intent = new Intent();
        mCryptoInputParcel.addParcelableProxy(ParcelableProxy.getForNoProxy());
        intent.putExtra(RESULT_CRYPTO_INPUT, mCryptoInputParcel);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onCancel() {
        finish();
    }
}
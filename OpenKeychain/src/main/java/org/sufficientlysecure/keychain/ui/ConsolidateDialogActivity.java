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
import android.support.v4.app.FragmentActivity;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ConsolidateResult;
import org.sufficientlysecure.keychain.service.ConsolidateInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;

/**
 * We can not directly create a dialog on the application context.
 * This activity encapsulates a DialogFragment to emulate a dialog.
 */
public class ConsolidateDialogActivity extends FragmentActivity
        implements CryptoOperationHelper.Callback<ConsolidateInputParcel, ConsolidateResult> {

    public static final String EXTRA_CONSOLIDATE_RECOVERY = "consolidate_recovery";

    private CryptoOperationHelper<ConsolidateInputParcel, ConsolidateResult> mConsolidateOpHelper;
    private boolean mRecovery;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this activity itself has no content view (see manifest)

        boolean recovery = getIntent().getBooleanExtra(EXTRA_CONSOLIDATE_RECOVERY, false);

        consolidateRecovery(recovery);
    }

    private void consolidateRecovery(boolean recovery) {

        mRecovery = recovery;

        mConsolidateOpHelper = new CryptoOperationHelper<>(this, this, R.string.progress_importing);
        mConsolidateOpHelper.cryptoOperation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mConsolidateOpHelper != null) {
            mConsolidateOpHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public ConsolidateInputParcel createOperationInput() {
        return new ConsolidateInputParcel(mRecovery);
    }

    @Override
    public void onCryptoOperationSuccess(ConsolidateResult result) {
        // don't care about result (for now?)
        ConsolidateDialogActivity.this.finish();
    }

    @Override
    public void onCryptoOperationCancelled() {

    }

    @Override
    public void onCryptoOperationError(ConsolidateResult result) {
        // don't care about result (for now?)
        ConsolidateDialogActivity.this.finish();
    }
}

/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.support.v4.app.FragmentActivity;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ChangePassphraseWorkflowResult;
import org.sufficientlysecure.keychain.service.ChangePassphraseWorkflowParcel.RevertChangeWorkflowParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.util.Preferences;

public class RevertChangeWorkflowDialogActivity extends FragmentActivity implements
        CryptoOperationHelper.Callback<RevertChangeWorkflowParcel, ChangePassphraseWorkflowResult> {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this activity itself has no content view (see manifest)

        CryptoOperationHelper<RevertChangeWorkflowParcel, ChangePassphraseWorkflowResult> mOperationHelper
                = new CryptoOperationHelper<>(1, this, this, R.string.progress_revert_change_workflow);
        mOperationHelper.cryptoOperation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public RevertChangeWorkflowParcel createOperationInput() {
        return new RevertChangeWorkflowParcel();
    }

    @Override
    public void onCryptoOperationSuccess(ChangePassphraseWorkflowResult result) {
        Preferences.getPreferences(this).setMidwayChangingPassphraseWorkflow(false);
        finish();
    }

    @Override
    public void onCryptoOperationCancelled() {
        finish();
    }

    @Override
    public void onCryptoOperationError(ChangePassphraseWorkflowResult result) {
        finish();
    }

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return false;
    }
}

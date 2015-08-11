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
package org.sufficientlysecure.keychain.ui.dialog;


import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.widget.FeedbackIndicatorView;

public class NFCUnlockDialog extends UnlockDialog {
    RelativeLayout mNfcFeedbackLayout;
    LinearLayout mUnlockTipLayout;
    TextView mUnlockTip;
    FeedbackIndicatorView mUnlockUserFeedback;
    ProgressBar mProgressBar;
    RelativeLayout mUnlockInputLayout;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // if the dialog is displayed from the application class, design is missing
        // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(getActivity(), R.style.KeychainTheme);

        CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);
        View view = LayoutInflater.from(theme).inflate(R.layout.unlock_nfc_fragment, null);
        alert.setView(view);
        setCancelable(false);

        mNfcFeedbackLayout = (RelativeLayout) view.findViewById(R.id.nfcFeedbackLayout);
        mUnlockTipLayout = (LinearLayout) view.findViewById(R.id.unlockTipLayout);
        mUnlockTip = (TextView) view.findViewById(R.id.unlockTip);
        mUnlockUserFeedback = (FeedbackIndicatorView) view.findViewById(R.id.unlockUserFeedback);
        mProgressBar = (ProgressBar) view.findViewById(R.id.progressBar);
        mUnlockInputLayout = (RelativeLayout) view.findViewById(R.id.unlockInputLayout);

        alert.setPositiveButton(getString(R.string.unlock_caps), null);
        alert.setNegativeButton(android.R.string.cancel, null);

        mAlertDialog = alert.show();

        return mAlertDialog;
    }
}

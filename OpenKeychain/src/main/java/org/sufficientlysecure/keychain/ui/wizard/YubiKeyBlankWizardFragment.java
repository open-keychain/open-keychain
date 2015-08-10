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
package org.sufficientlysecure.keychain.ui.wizard;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;

public class YubiKeyBlankWizardFragment extends WizardFragment {

    /**
     * Creates new instance of this fragment
     */
    public static YubiKeyBlankWizardFragment newInstance() {
        return new YubiKeyBlankWizardFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.wizard_yubi_blank_fragment, container, false);
    }

    @Override
    public boolean onBackClicked() {
        final Activity activity = getActivity();
        if (activity.getFragmentManager().getBackStackEntryCount() == 0) {
            activity.setResult(Activity.RESULT_CANCELED);
            activity.finish();
            return false;
        }
        return true;
    }

    @Override
    public boolean onNextClicked() {
        mWizardFragmentListener.setUseYubiKey();
        return true;
    }
}

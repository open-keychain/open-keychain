/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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

import androidx.lifecycle.ViewModelProviders;
import android.os.Bundle;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;
import org.sufficientlysecure.keychain.ui.keyview.UnifiedKeyInfoViewModel;
import timber.log.Timber;


public class CertifyFingerprintActivity extends BaseActivity {
    public static final String EXTRA_MASTER_KEY_ID = "master_key_id";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle extras = getIntent().getExtras();
        if (extras == null || !extras.containsKey(EXTRA_MASTER_KEY_ID)) {
            Timber.e("Missing required extra master_key_id!");
            finish();
            return;
        }

        setFullScreenDialogClose(v -> finish());

        long masterKeyId = extras.getLong(EXTRA_MASTER_KEY_ID);
        UnifiedKeyInfoViewModel viewModel = ViewModelProviders.of(this).get(UnifiedKeyInfoViewModel.class);
        viewModel.setMasterKeyId(masterKeyId);

        if (savedInstanceState == null) {
            startFragment();
        }
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.certify_fingerprint_activity);
    }

    private void startFragment() {
        CertifyFingerprintFragment frag = CertifyFingerprintFragment.newInstance();
        getSupportFragmentManager().beginTransaction().replace(R.id.certify_fingerprint_fragment, frag).commit();
    }

}

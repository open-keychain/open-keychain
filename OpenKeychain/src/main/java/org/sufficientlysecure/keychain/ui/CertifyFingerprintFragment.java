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
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.ui.keyview.UnifiedKeyInfoViewModel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


public class CertifyFingerprintFragment extends Fragment {
    static final int REQUEST_CERTIFY = 1;

    private TextView vFingerprint;

    private UnifiedKeyInfoViewModel viewModel;

    public static CertifyFingerprintFragment newInstance() {
        return new CertifyFingerprintFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.certify_fingerprint_fragment, viewGroup, false);

        vFingerprint = view.findViewById(R.id.certify_fingerprint_fingerprint);

        view.findViewById(R.id.certify_fingerprint_button_no).setOnClickListener(v -> requireActivity().finish());
        view.findViewById(R.id.certify_fingerprint_button_yes).setOnClickListener(v -> startCertifyActivity());

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        viewModel = ViewModelProviders.of(requireActivity()).get(UnifiedKeyInfoViewModel.class);
        viewModel.getUnifiedKeyInfoLiveData(requireContext()).observe(this, this::onLoadUnifiedKeyInfo);
    }

    private void onLoadUnifiedKeyInfo(UnifiedKeyInfo unifiedKeyInfo) {
        if (unifiedKeyInfo == null) {
            return;
        }

        String fingerprint = KeyFormattingUtils.convertFingerprintToHex(unifiedKeyInfo.fingerprint());
        vFingerprint.setText(KeyFormattingUtils.formatFingerprint(fingerprint));
    }

    private void startCertifyActivity() {
        Intent certifyIntent = new Intent(getActivity(), CertifyKeyActivity.class);
        certifyIntent.putExtras(requireActivity().getIntent());
        certifyIntent.putExtra(CertifyKeyActivity.EXTRA_KEY_IDS, new long[] { viewModel.getMasterKeyId() });
        startActivityForResult(certifyIntent, REQUEST_CERTIFY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CERTIFY) {
            FragmentActivity activity = requireActivity();
            activity.setResult(resultCode, data);
            activity.finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

}

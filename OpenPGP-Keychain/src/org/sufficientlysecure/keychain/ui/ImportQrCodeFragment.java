/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.sufficientlysecure.keychain.R;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentIntegratorSupportV4;
import com.google.zxing.integration.android.IntentResult;

public class ImportQrCodeFragment extends Fragment {

    private ImportKeysActivity mImportActivity;
    private Button mButton;

    /**
     * Creates new instance of this fragment
     */
    public static ImportQrCodeFragment newInstance() {
        ImportQrCodeFragment frag = new ImportQrCodeFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.import_keys_qr_code_fragment, container, false);

        mButton = (Button) view.findViewById(R.id.import_qrcode_button);
        mButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // scan using xzing's Barcode Scanner
                new IntentIntegratorSupportV4(ImportQrCodeFragment.this).initiateScan();
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImportActivity = (ImportKeysActivity) getActivity();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case IntentIntegrator.REQUEST_CODE: {
            IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode,
                    data);
            if (scanResult != null && scanResult.getFormatName() != null) {

                // mScannedContent = scanResult.getContents();

                mImportActivity.loadCallback(scanResult.getContents().getBytes(), null);

                // mImportData = scanResult.getContents().getBytes();
                // mImportFilename = null;

                // mContentView.setText(mScannedContent);
                // String[] bits = scanResult.getContents().split(",");
                // if (bits.length != 2) {
                // return; // dont know how to handle this. Not a valid code
                // }
                //
                // long keyId = Long.parseLong(bits[0]);
                // String expectedFingerprint = bits[1];

                // importAndSign(keyId, expectedFingerprint);
            }

            break;
        }

        default:
            super.onActivityResult(requestCode, resultCode, data);

            break;
        }
    }

}

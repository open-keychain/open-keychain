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

import java.util.ArrayList;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegratorSupportV4;
import com.google.zxing.integration.android.IntentResult;

public class ImportKeysQrCodeFragment extends Fragment {

    private ImportKeysActivity mImportActivity;
    private Button mButton;
    private TextView mText;
    private ProgressBar mProgress;

    private String[] scannedContent;

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysQrCodeFragment newInstance() {
        ImportKeysQrCodeFragment frag = new ImportKeysQrCodeFragment();

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
        mText = (TextView) view.findViewById(R.id.import_qrcode_text);
        mProgress = (ProgressBar) view.findViewById(R.id.import_qrcode_progress);

        mButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // scan using xzing's Barcode Scanner
                new IntentIntegratorSupportV4(ImportKeysQrCodeFragment.this).initiateScan();
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
        case IntentIntegratorSupportV4.REQUEST_CODE: {
            IntentResult scanResult = IntentIntegratorSupportV4.parseActivityResult(requestCode,
                    resultCode, data);
            if (scanResult != null && scanResult.getFormatName() != null) {

                Log.d(Constants.TAG, scanResult.getContents());

                String[] parts = scanResult.getContents().split(",");

                if (parts.length != 3) {
                    Toast.makeText(getActivity(), R.string.import_qr_code_wrong, Toast.LENGTH_LONG)
                            .show();
                    return;
                }

                int counter = Integer.valueOf(parts[0]);
                int size = Integer.valueOf(parts[1]);
                String content = parts[2];

                Log.d(Constants.TAG, "" + counter);
                Log.d(Constants.TAG, "" + size);
                Log.d(Constants.TAG, "" + content);

                // first qr code -> setup
                if (counter == 0) {
                    scannedContent = new String[size];
                    mProgress.setMax(size);
                }

                // save scanned content
                scannedContent[counter] = content;

                // get missing numbers
                ArrayList<Integer> missing = new ArrayList<Integer>();
                for (int i = 0; i < scannedContent.length; i++) {
                    if (scannedContent[i] == null) {
                        missing.add(i);
                    }
                }

                // update progress and text
                mProgress.setProgress(scannedContent.length - missing.size());
                String missingString = "";
                for (int m : missing) {
                    if (!missingString.equals(""))
                        missingString += ", ";
                    missingString += String.valueOf(m + 1);
                }
                mText.setText(getString(R.string.import_qr_code_missing, missingString));

                // finished!
                if (missing.size() == 0) {
                    mText.setText(R.string.import_qr_code_finished);
                    String result = "";
                    for (String in : scannedContent) {
                        result += in;
                    }
                    mImportActivity.loadCallback(result.getBytes(), null);
                }
            }

            break;
        }

        default:
            super.onActivityResult(requestCode, resultCode, data);

            break;
        }
    }
}

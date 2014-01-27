/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import java.util.Locale;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.IntentIntegratorSupportV4;
import org.sufficientlysecure.keychain.util.Log;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.google.zxing.integration.android.IntentResult;

public class ImportKeysQrCodeFragment extends Fragment {

    private ImportKeysActivity mImportActivity;
    private BootstrapButton mButton;
    private TextView mText;
    private ProgressBar mProgress;

    private String[] mScannedContent;

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

        mButton = (BootstrapButton) view.findViewById(R.id.import_qrcode_button);
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
        switch (requestCode & 0xFFFF) {
        case IntentIntegratorSupportV4.REQUEST_CODE: {
            IntentResult scanResult = IntentIntegratorSupportV4.parseActivityResult(requestCode,
                    resultCode, data);
            if (scanResult != null && scanResult.getFormatName() != null) {

                Log.d(Constants.TAG, "scanResult content: " + scanResult.getContents());

                // look if it's fingerprint only
                if (scanResult.getContents().toLowerCase(Locale.ENGLISH).startsWith("openpgp4fpr")) {
                    importFingerprint(scanResult.getContents().toLowerCase(Locale.ENGLISH));
                    return;
                }

                // look if it is the whole key
                String[] parts = scanResult.getContents().split(",");
                if (parts.length == 3) {
                    importParts(parts);
                    return;
                }

                // fail...
                Toast.makeText(getActivity(), R.string.import_qr_code_wrong, Toast.LENGTH_LONG)
                        .show();
            }

            break;
        }

        default:
            super.onActivityResult(requestCode, resultCode, data);

            break;
        }
    }

    private void importFingerprint(String uri) {
        String fingerprint = uri.split(":")[1];

        Log.d(Constants.TAG, "fingerprint: " + fingerprint);

        if (fingerprint.length() < 16) {
            Toast.makeText(getActivity(), R.string.import_qr_code_too_short_fingerprint,
                    Toast.LENGTH_LONG).show();
            return;
        }

        Intent queryIntent = new Intent(getActivity(), KeyServerQueryActivity.class);
        queryIntent.setAction(KeyServerQueryActivity.ACTION_LOOK_UP_KEY_ID);
        queryIntent.putExtra(KeyServerQueryActivity.EXTRA_FINGERPRINT, fingerprint);
        startActivity(queryIntent);
    }

    private void importParts(String[] parts) {
        int counter = Integer.valueOf(parts[0]);
        int size = Integer.valueOf(parts[1]);
        String content = parts[2];

        Log.d(Constants.TAG, "" + counter);
        Log.d(Constants.TAG, "" + size);
        Log.d(Constants.TAG, "" + content);

        // first qr code -> setup
        if (counter == 0) {
            mScannedContent = new String[size];
            mProgress.setMax(size);
            mProgress.setVisibility(View.VISIBLE);
            mText.setVisibility(View.VISIBLE);
        }

        if (mScannedContent == null || counter > mScannedContent.length) {
            Toast.makeText(getActivity(), R.string.import_qr_code_start_with_one, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        // save scanned content
        mScannedContent[counter] = content;

        // get missing numbers
        ArrayList<Integer> missing = new ArrayList<Integer>();
        for (int i = 0; i < mScannedContent.length; i++) {
            if (mScannedContent[i] == null) {
                missing.add(i);
            }
        }

        // update progress and text
        int alreadyScanned = mScannedContent.length - missing.size();
        mProgress.setProgress(alreadyScanned);

        String missingString = "";
        for (int m : missing) {
            if (!missingString.equals("")) {
                missingString += ", ";
            }
            missingString += String.valueOf(m + 1);
        }

        String missingText = getResources().getQuantityString(R.plurals.import_qr_code_missing,
                missing.size(), missingString);
        mText.setText(missingText);

        // finished!
        if (missing.size() == 0) {
            mText.setText(R.string.import_qr_code_finished);
            String result = "";
            for (String in : mScannedContent) {
                result += in;
            }
            mImportActivity.loadCallback(result.getBytes(), null);
        }
    }

}

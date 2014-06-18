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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.zxing.integration.android.IntentResult;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.IntentIntegratorSupportV4;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Notify;

import java.util.ArrayList;
import java.util.Locale;

public class ImportKeysQrCodeFragment extends Fragment {
    private ImportKeysActivity mImportActivity;
    private View mNfcButton;

    private View mQrCodeButton;
    private TextView mQrCodeText;
    private ProgressBar mQrCodeProgress;

    private String[] mQrCodeContent;

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysFileFragment newInstance() {
        ImportKeysFileFragment frag = new ImportKeysFileFragment();

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

        mNfcButton = view.findViewById(R.id.import_nfc_button);
        mNfcButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // show nfc help
                Intent intent = new Intent(getActivity(), HelpActivity.class);
                intent.putExtra(HelpActivity.EXTRA_SELECTED_TAB, HelpActivity.TAB_NFC);
                startActivityForResult(intent, 0);
            }
        });

        mQrCodeButton = view.findViewById(R.id.import_qrcode_button);
        mQrCodeText = (TextView) view.findViewById(R.id.import_qrcode_text);
        mQrCodeProgress = (ProgressBar) view.findViewById(R.id.import_qrcode_progress);

        mQrCodeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // scan using xzing's Barcode Scanner
                new IntentIntegratorSupportV4(ImportKeysQrCodeFragment.this).initiateScan();
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mImportActivity = (ImportKeysActivity) activity;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode & 0xFFFF) {
            case IntentIntegratorSupportV4.REQUEST_CODE: {
                IntentResult scanResult = IntentIntegratorSupportV4.parseActivityResult(requestCode,
                        resultCode, data);
                if (scanResult != null && scanResult.getFormatName() != null) {
                    String scannedContent = scanResult.getContents();

                    Log.d(Constants.TAG, "scannedContent: " + scannedContent);

                    // look if it's fingerprint only
                    if (scannedContent.toLowerCase(Locale.ENGLISH).startsWith(Constants.FINGERPRINT_SCHEME)) {
                        importFingerprint(Uri.parse(scanResult.getContents()));
                        return;
                    }

                    // look if it is the whole key
                    String[] parts = scannedContent.split(",");
                    if (parts.length == 3) {
                        importParts(parts);
                        return;
                    }

                    // is this a full key encoded as qr code?
                    if (scannedContent.startsWith("-----BEGIN PGP")) {
                        mImportActivity.loadCallback(scannedContent.getBytes(), null, null, null, null);
                        return;
                    }

                    // fail...
                    Notify.showNotify(getActivity(), R.string.import_qr_code_wrong, Notify.Style.ERROR);
                }

                break;
            }

            default:
                super.onActivityResult(requestCode, resultCode, data);

                break;
        }
    }


    public void importFingerprint(Uri dataUri) {
        mImportActivity.loadFromFingerprintUri(null, dataUri);
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
            mQrCodeContent = new String[size];
            mQrCodeProgress.setMax(size);
            mQrCodeProgress.setVisibility(View.VISIBLE);
            mQrCodeText.setVisibility(View.VISIBLE);
        }

        if (mQrCodeContent == null || counter > mQrCodeContent.length) {
            Notify.showNotify(getActivity(), R.string.import_qr_code_start_with_one, Notify.Style.ERROR);
            return;
        }

        // save scanned content
        mQrCodeContent[counter] = content;

        // get missing numbers
        ArrayList<Integer> missing = new ArrayList<Integer>();
        for (int i = 0; i < mQrCodeContent.length; i++) {
            if (mQrCodeContent[i] == null) {
                missing.add(i);
            }
        }

        // update progress and text
        int alreadyScanned = mQrCodeContent.length - missing.size();
        mQrCodeProgress.setProgress(alreadyScanned);

        String missingString = "";
        for (int m : missing) {
            if (!missingString.equals("")) {
                missingString += ", ";
            }
            missingString += String.valueOf(m + 1);
        }

        String missingText = getResources().getQuantityString(R.plurals.import_qr_code_missing,
                missing.size(), missingString);
        mQrCodeText.setText(missingText);

        // finished!
        if (missing.size() == 0) {
            mQrCodeText.setText(R.string.import_qr_code_finished);
            String result = "";
            for (String in : mQrCodeContent) {
                result += in;
            }
            mImportActivity.loadCallback(result.getBytes(), null, null, null, null);
        }
    }
}

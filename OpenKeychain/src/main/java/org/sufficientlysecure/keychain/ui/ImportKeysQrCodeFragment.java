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

import com.google.zxing.integration.android.IntentResult;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.IntentIntegratorSupportV4;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Notify;

import java.util.Locale;

public class ImportKeysQrCodeFragment extends Fragment {
    private ImportKeysActivity mImportActivity;

    private View mNfcButton;
    private View mQrCodeButton;

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

                    // is this a full key encoded as qr code?
                    if (scannedContent.startsWith("-----BEGIN PGP")) {
                        mImportActivity.loadCallback(new ImportKeysListFragment.BytesLoaderState(scannedContent.getBytes(), null));
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

}

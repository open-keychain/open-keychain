/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import java.util.ArrayList;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.QrCodeUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockDialogFragment;

public class ShareQrCodeDialogFragment extends SherlockDialogFragment {
    private static final String ARG_URI = "uri";
    private static final String ARG_FINGERPRINT_ONLY = "fingerprint_only";

    private ImageView mImage;
    private TextView mText;

    private ArrayList<String> mContentList;
    private int mCounter;

    private static final int QR_CODE_SIZE = 1000;

    /**
     * Creates new instance of this dialog fragment
     * 
     * @param content
     *            Content to be shared via QR Codes
     * @return
     */
    public static ShareQrCodeDialogFragment newInstance(Uri dataUri, boolean fingerprintOnly) {
        ShareQrCodeDialogFragment frag = new ShareQrCodeDialogFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, dataUri);
        args.putBoolean(ARG_FINGERPRINT_ONLY, fingerprintOnly);

        frag.setArguments(args);

        return frag;
    }

    /**
     * Creates dialog
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();

        Uri dataUri = getArguments().getParcelable(ARG_URI);
        boolean fingerprintOnly = getArguments().getBoolean(ARG_FINGERPRINT_ONLY);

        AlertDialog.Builder alert = new AlertDialog.Builder(activity);

        alert.setTitle(R.string.share_qr_code_dialog_title);

        LayoutInflater inflater = activity.getLayoutInflater();
        View view = inflater.inflate(R.layout.share_qr_code_dialog, null);
        alert.setView(view);

        mImage = (ImageView) view.findViewById(R.id.share_qr_code_dialog_image);
        mText = (TextView) view.findViewById(R.id.share_qr_code_dialog_text);

        // TODO
        long masterKeyId = ProviderHelper.getMasterKeyId(getActivity(), dataUri);

        String content = null;
        if (fingerprintOnly) {
            content = "openpgp4fpr:";

            String fingerprint = PgpKeyHelper.convertKeyToHex(masterKeyId);

            mText.setText(getString(R.string.share_qr_code_dialog_fingerprint_text) + " "
                    + fingerprint);

            content = content + fingerprint;

            Log.d(Constants.TAG, "content: " + content);

            alert.setPositiveButton(R.string.btn_okay, null);
        } else {
            mText.setText(R.string.share_qr_code_dialog_start);

            // get public keyring as ascii armored string
            ArrayList<String> keyringArmored = ProviderHelper.getKeyRingsAsArmoredString(
                    getActivity(), dataUri, new long[] { masterKeyId });

            // TODO: binary?

            content = keyringArmored.get(0);

            // OnClickListener are set in onResume to prevent automatic dismissing of Dialogs
            // http://stackoverflow.com/questions/2620444/how-to-prevent-a-dialog-from-closing-when-a-button-is-clicked
            alert.setPositiveButton(R.string.btn_next, null);
            alert.setNegativeButton(android.R.string.cancel, null);
        }

        mContentList = splitString(content, 1000);

        // start with first
        mCounter = 0;
        updateQrCode();

        return alert.create();
    }

    @Override
    public void onResume() {
        super.onResume();
        AlertDialog alertDialog = (AlertDialog) getDialog();
        final Button backButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        final Button nextButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCounter > 0) {
                    mCounter--;
                    updateQrCode();
                    updateDialog(backButton, nextButton);
                } else {
                    dismiss();
                }
            }
        });
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mCounter < mContentList.size() - 1) {
                    mCounter++;
                    updateQrCode();
                    updateDialog(backButton, nextButton);
                } else {
                    dismiss();
                }
            }
        });
    }

    private void updateQrCode() {
        // Content: <counter>,<size>,<content>
        mImage.setImageBitmap(QrCodeUtils.getQRCodeBitmap(mCounter + "," + mContentList.size()
                + "," + mContentList.get(mCounter), QR_CODE_SIZE));
    }

    private void updateDialog(Button backButton, Button nextButton) {
        if (mCounter == 0) {
            backButton.setText(android.R.string.cancel);
        } else {
            backButton.setText(R.string.btn_back);
        }
        if (mCounter == mContentList.size() - 1) {
            nextButton.setText(android.R.string.ok);
        } else {
            nextButton.setText(R.string.btn_next);
        }

        mText.setText(getResources().getString(R.string.share_qr_code_dialog_progress,
                mCounter + 1, mContentList.size()));
    }

    /**
     * Split String by number of characters
     * 
     * @param text
     * @param size
     * @return
     */
    private ArrayList<String> splitString(String text, int size) {
        ArrayList<String> strings = new ArrayList<String>();
        int index = 0;
        while (index < text.length()) {
            strings.add(text.substring(index, Math.min(index + size, text.length())));
            index += size;
        }

        return strings;
    }
}

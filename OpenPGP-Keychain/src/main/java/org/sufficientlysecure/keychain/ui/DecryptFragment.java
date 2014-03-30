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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.beardedhen.androidbootstrap.BootstrapButton;
import com.devspark.appmsg.AppMsg;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.PgpKeyHelper;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.ui.dialog.PassphraseDialogFragment;
import org.sufficientlysecure.keychain.util.Log;

public class DecryptFragment extends Fragment {
    private static final int RESULT_CODE_LOOKUP_KEY = 0x00007006;

    protected long mSignatureKeyId = 0;

    protected RelativeLayout mSignatureLayout = null;
    protected ImageView mSignatureStatusImage = null;
    protected TextView mUserId = null;
    protected TextView mUserIdRest = null;

    protected BootstrapButton mLookupKey = null;


    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSignatureLayout = (RelativeLayout) getView().findViewById(R.id.signature);
        mSignatureStatusImage = (ImageView) getView().findViewById(R.id.ic_signature_status);
        mUserId = (TextView) getView().findViewById(R.id.mainUserId);
        mUserIdRest = (TextView) getView().findViewById(R.id.mainUserIdRest);
        mLookupKey = (BootstrapButton) getView().findViewById(R.id.lookup_key);
        mLookupKey.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                lookupUnknownKey(mSignatureKeyId);
            }
        });
        mSignatureLayout.setVisibility(View.GONE);
        mSignatureLayout.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                lookupUnknownKey(mSignatureKeyId);
            }
        });
    }

    private void lookupUnknownKey(long unknownKeyId) {
        Intent intent = new Intent(getActivity(), ImportKeysActivity.class);
        intent.setAction(ImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER);
        intent.putExtra(ImportKeysActivity.EXTRA_KEY_ID, unknownKeyId);
        startActivityForResult(intent, RESULT_CODE_LOOKUP_KEY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            // this request is returned after LookupUnknownKeyDialogFragment started
            // ImportKeysActivity and user looked uo key
            case RESULT_CODE_LOOKUP_KEY: {
                Log.d(Constants.TAG, "Returning from Lookup Key...");
                if (resultCode == Activity.RESULT_OK) {
                    // decrypt again
//                    decryptStart();
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);

                break;
            }
        }
    }

    protected void onSignatureResult(OpenPgpSignatureResult signatureResult) {
        mSignatureKeyId = 0;
        mSignatureLayout.setVisibility(View.GONE);
        if (signatureResult != null) {

            mSignatureKeyId = signatureResult.getKeyId();

            String userId = signatureResult.getUserId();
            String[] userIdSplit = PgpKeyHelper.splitUserId(userId);
            if (userIdSplit[0] != null) {
                mUserId.setText(userId);
            } else {
                mUserId.setText(R.string.user_id_no_name);
            }
            if (userIdSplit[1] != null) {
                mUserIdRest.setText(userIdSplit[1]);
            } else {
                mUserIdRest.setText(getString(R.string.label_key_id) + ": "
                        + PgpKeyHelper.convertKeyIdToHex(mSignatureKeyId));
            }

            switch (signatureResult.getStatus()) {
                case OpenPgpSignatureResult.SIGNATURE_SUCCESS_UNCERTIFIED: {
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_ok);
                    mLookupKey.setVisibility(View.GONE);
                    break;
                }

                // TODO!
//                            case OpenPgpSignatureResult.SIGNATURE_SUCCESS_CERTIFIED: {
//                                break;
//                            }

                case OpenPgpSignatureResult.SIGNATURE_UNKNOWN_PUB_KEY: {
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                    mLookupKey.setVisibility(View.VISIBLE);
                    AppMsg.makeText(getActivity(),
                            R.string.unknown_signature,
                            AppMsg.STYLE_ALERT).show();
                    break;
                }

                default: {
                    mSignatureStatusImage.setImageResource(R.drawable.overlay_error);
                    mLookupKey.setVisibility(View.GONE);
                    break;
                }
            }
            mSignatureLayout.setVisibility(View.VISIBLE);
        }
    }

    protected void showPassphraseDialog(long keyId) {
        // Message is received after passphrase is cached
        Handler returnHandler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                if (message.what == PassphraseDialogFragment.MESSAGE_OKAY) {
                    String passphrase =
                            message.getData().getString(PassphraseDialogFragment.MESSAGE_DATA_PASSPHRASE);
                    decryptStart(passphrase);
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(returnHandler);

        try {
            PassphraseDialogFragment passphraseDialog = PassphraseDialogFragment.newInstance(getActivity(),
                    messenger, keyId);

            passphraseDialog.show(getActivity().getSupportFragmentManager(), "passphraseDialog");
        } catch (PgpGeneralException e) {
            Log.d(Constants.TAG, "No passphrase for this secret key, encrypt directly!");
            // send message to handler to start encryption directly
            returnHandler.sendEmptyMessage(PassphraseDialogFragment.MESSAGE_OKAY);
        }
    }

    protected void decryptStart(String passphrase) {

    }

}

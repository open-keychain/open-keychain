/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.linked;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.LinkedVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.pgp.linked.LinkedIdentity;
import org.sufficientlysecure.keychain.pgp.linked.resources.DnsResource;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class LinkedIdCreateDnsStep2Fragment extends Fragment {

    private static final int REQUEST_CODE_OUTPUT = 0x00007007;
    private static final int REQUEST_CODE_PASSPHRASE = 0x00007008;

    public static final String DOMAIN = "domain", NONCE = "nonce", TEXT = "text";

    LinkedIdWizard mLinkedIdWizard;

    TextView mTextView;
    ImageView mVerifyImage;
    View mVerifyProgress;
    TextView mVerifyStatus;

    String mResourceDomain;
    String mResourceNonce, mResourceString;

    // This is a resource, set AFTER it has been verified
    DnsResource mVerifiedResource = null;

    /**
     * Creates new instance of this fragment
     */
    public static LinkedIdCreateDnsStep2Fragment newInstance
            (String uri, String proofNonce, String proofText) {

        LinkedIdCreateDnsStep2Fragment frag = new LinkedIdCreateDnsStep2Fragment();

        Bundle args = new Bundle();
        args.putString(DOMAIN, uri);
        args.putString(NONCE, proofNonce);
        args.putString(TEXT, proofText);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.linked_create_dns_fragment_step2, container, false);

        mResourceDomain = getArguments().getString(DOMAIN);
        mResourceNonce = getArguments().getString(NONCE);
        mResourceString = getArguments().getString(TEXT);

        view.findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startCertify();
            }
        });

        view.findViewById(R.id.back_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLinkedIdWizard.loadFragment(null, null, LinkedIdWizard.FRAG_ACTION_TO_LEFT);
            }
        });

        mVerifyImage = (ImageView) view.findViewById(R.id.verify_image);
        mVerifyProgress = view.findViewById(R.id.verify_progress);
        mVerifyStatus = (TextView) view.findViewById(R.id.verify_status);

        view.findViewById(R.id.button_send).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofSend();
            }
        });

        view.findViewById(R.id.button_save).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofSave();
            }
        });

        view.findViewById(R.id.button_verify).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofVerify();
            }
        });

        mTextView = (TextView) view.findViewById(R.id.linked_create_dns_text);
        mTextView.setText(mResourceString);

        setVerifyProgress(false, null);
        mVerifyStatus.setText(R.string.linked_verify_pending);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLinkedIdWizard = (LinkedIdWizard) getActivity();
    }

    public void setVerifyProgress(boolean on, Boolean success) {
        mVerifyProgress.setVisibility(on ? View.VISIBLE : View.GONE);
        mVerifyImage.setVisibility(on ? View.GONE : View.VISIBLE);
        if (success == null) {
            mVerifyStatus.setText(R.string.linked_verifying);
            mVerifyImage.setImageResource(R.drawable.status_signature_unverified_cutout_24px);
            mVerifyImage.setColorFilter(getResources().getColor(R.color.tertiary_text_light),
                    PorterDuff.Mode.SRC_IN);
        } else if (success) {
            mVerifyStatus.setText(R.string.linked_verify_success);
            mVerifyImage.setImageResource(R.drawable.status_signature_verified_cutout_24px);
            mVerifyImage.setColorFilter(getResources().getColor(R.color.android_green_dark),
                    PorterDuff.Mode.SRC_IN);
        } else {
            mVerifyStatus.setText(R.string.linked_verify_error);
            mVerifyImage.setImageResource(R.drawable.status_signature_unknown_cutout_24px);
            mVerifyImage.setColorFilter(getResources().getColor(R.color.android_red_dark),
                    PorterDuff.Mode.SRC_IN);
        }
    }

    private void proofSend () {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, mResourceString);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    private void proofSave () {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Notify.showNotify(getActivity(), "External storage not available!", Style.ERROR);
            return;
        }

        String targetName = "pgpkey.txt";

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            File targetFile = new File(Constants.Path.APP_DIR, targetName);
            FileHelper.saveFile(this, getString(R.string.title_decrypt_to_file),
                    getString(R.string.specify_file_to_decrypt_to), targetFile, REQUEST_CODE_OUTPUT);
        } else {
            FileHelper.saveDocument(this, "text/plain", targetName, REQUEST_CODE_OUTPUT);
        }
    }

    private void saveFile(Uri uri) {
        try {
            PrintWriter out =
                    new PrintWriter(getActivity().getContentResolver().openOutputStream(uri));
            out.print(mResourceString);
            if (out.checkError()) {
                Notify.showNotify(getActivity(), "Error writing file!", Style.ERROR);
            }
        } catch (FileNotFoundException e) {
            Notify.showNotify(getActivity(), "File could not be opened for writing!", Style.ERROR);
            e.printStackTrace();
        }
    }

    public void proofVerify() {
        setVerifyProgress(true, null);

        final DnsResource resource = DnsResource.createNew(mResourceDomain);

        new AsyncTask<Void,Void,LinkedVerifyResult>() {

            @Override
            protected LinkedVerifyResult doInBackground(Void... params) {
                return resource.verify(mLinkedIdWizard.mFingerprint, mResourceNonce);
            }

            @Override
            protected void onPostExecute(LinkedVerifyResult result) {
                super.onPostExecute(result);
                if (result.success()) {
                    setVerifyProgress(false, true);
                    mVerifiedResource = resource;
                } else {
                    setVerifyProgress(false, false);
                    // on error, show error message
                    result.createNotify(getActivity()).show();
                }
            }
        }.execute();

    }

    public void startCertify() {

        if (mVerifiedResource == null) {
            Notify.showNotify(getActivity(), R.string.linked_need_verify, Style.ERROR);
            return;
        }

        Intent intent = new Intent(getActivity(), PassphraseDialogActivity.class);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, mLinkedIdWizard.mMasterKeyId);
        startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);

    }

    public void certifyLinkedIdentity (String passphrase) {
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(
                getActivity(),
                getString(R.string.progress_saving),
                ProgressDialog.STYLE_HORIZONTAL,
                true) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {

                    // get returned data bundle
                    Bundle returnData = message.getData();
                    if (returnData == null) {
                        return;
                    }
                    final OperationResult result =
                            returnData.getParcelable(OperationResult.EXTRA_RESULT);
                    if (result == null) {
                        return;
                    }

                    // if bad -> display here!
                    if (!result.success()) {
                        result.createNotify(getActivity()).show();
                        return;
                    }

                    result.createNotify(getActivity()).show();

                    // if good -> finish, return result to showkey and display there!
                    // Intent intent = new Intent();
                    // intent.putExtra(OperationResult.EXTRA_RESULT, result);
                    // getActivity().setResult(EditKeyActivity.RESULT_OK, intent);

                    // AffirmationCreateHttpsStep3Fragment frag =
                    // AffirmationCreateHttpsStep3Fragment.newInstance(
                    // mResourceDomain, mResourceNonce, mResourceString);

                    // mAffirmationWizard.loadFragment(null, frag, AffirmationWizard.FRAG_ACTION_TO_RIGHT);

                }
            }
        };

        SaveKeyringParcel skp =
                new SaveKeyringParcel(mLinkedIdWizard.mMasterKeyId, mLinkedIdWizard.mFingerprint);

        WrappedUserAttribute ua =
                LinkedIdentity.fromResource(mVerifiedResource, mResourceNonce).toUserAttribute();

        skp.mAddUserAttribute.add(ua);

        // Send all information needed to service to import key in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_EDIT_KEYRING);

        // fill values for this action
        Bundle data = new Bundle();
        data.putString(KeychainIntentService.EDIT_KEYRING_PASSPHRASE, passphrase);
        data.putParcelable(KeychainIntentService.EDIT_KEYRING_PARCEL, skp);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);

    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            // For saving a file
            case REQUEST_CODE_OUTPUT:
                if (data == null) {
                    return;
                }
                Uri uri = data.getData();
                saveFile(uri);
                break;
            case REQUEST_CODE_PASSPHRASE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String passphrase =
                            data.getStringExtra(PassphraseDialogActivity.MESSAGE_DATA_PASSPHRASE);
                    certifyLinkedIdentity(passphrase);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

}

package org.sufficientlysecure.keychain.ui.linked;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.LinkedVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;
import org.sufficientlysecure.keychain.pgp.linked.LinkedIdentity;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.util.Notify;

public abstract class LinkedIdCreateFinalFragment extends Fragment {

    protected static final int REQUEST_CODE_PASSPHRASE = 0x00007008;

    protected LinkedIdWizard mLinkedIdWizard;

    private ImageView mVerifyImage;
    private TextView mVerifyStatus;
    private ViewAnimator mVerifyAnimator;

    // This is a resource, set AFTER it has been verified
    LinkedCookieResource mVerifiedResource = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLinkedIdWizard = (LinkedIdWizard) getActivity();
    }

    protected abstract View newView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = newView(inflater, container, savedInstanceState);

        view.findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                startCertify();
            }
        });

        view.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLinkedIdWizard.loadFragment(null, null, LinkedIdWizard.FRAG_ACTION_TO_LEFT);
            }
        });

        mVerifyImage = (ImageView) view.findViewById(R.id.verify_image);
        mVerifyAnimator = (ViewAnimator) view.findViewById(R.id.verify_progress);
        mVerifyStatus = (TextView) view.findViewById(R.id.verify_status);

        view.findViewById(R.id.button_verify).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofVerify();
            }
        });

        setVerifyProgress(false, null);
        mVerifyStatus.setText(R.string.linked_verify_pending);

        return view;
    }


    abstract LinkedCookieResource getResource();

    private void setVerifyProgress(boolean on, Boolean success) {
        if (success == null) {
            mVerifyStatus.setText(R.string.linked_verifying);
        } else if (success) {
            mVerifyStatus.setText(R.string.linked_verify_success);
            mVerifyImage.setImageResource(R.drawable.status_signature_verified_cutout_24dp);
            mVerifyImage.setColorFilter(getResources().getColor(R.color.android_green_dark),
                    PorterDuff.Mode.SRC_IN);
        } else {
            mVerifyStatus.setText(R.string.linked_verify_error);
            mVerifyImage.setImageResource(R.drawable.status_signature_unknown_cutout_24dp);
            mVerifyImage.setColorFilter(getResources().getColor(R.color.android_red_dark),
                    PorterDuff.Mode.SRC_IN);
        }
        mVerifyAnimator.setDisplayedChild(on ? 1 : 0);
    }

    protected void proofVerify() {
        setVerifyProgress(true, null);

        new AsyncTask<Void,Void,LinkedVerifyResult>() {

            @Override
            protected LinkedVerifyResult doInBackground(Void... params) {
                LinkedCookieResource resource = getResource();
                LinkedVerifyResult result = resource.verify(mLinkedIdWizard.mFingerprint);
                if (result.success()) {
                    mVerifiedResource = resource;
                }
                return result;
            }

            @Override
            protected void onPostExecute(LinkedVerifyResult result) {
                super.onPostExecute(result);
                if (result.success()) {
                    setVerifyProgress(false, true);
                } else {
                    setVerifyProgress(false, false);
                    // on error, show error message
                    result.createNotify(getActivity()).show();
                }
            }
        }.execute();

    }

    private void startCertify() {

        if (mVerifiedResource == null) {
            Notify.showNotify(getActivity(), R.string.linked_need_verify, Notify.Style.ERROR);
            return;
        }

        Intent intent = new Intent(getActivity(), PassphraseDialogActivity.class);
        intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, mLinkedIdWizard.mMasterKeyId);
        startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);

    }

    private void certifyLinkedIdentity (String passphrase) {
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

                    getActivity().finish();

                }
            }
        };

        SaveKeyringParcel skp =
                new SaveKeyringParcel(mLinkedIdWizard.mMasterKeyId, mLinkedIdWizard.mFingerprint);

        WrappedUserAttribute ua =
                LinkedIdentity.fromResource(mVerifiedResource).toUserAttribute();

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
            case REQUEST_CODE_PASSPHRASE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String passphrase =
                            data.getStringExtra(PassphraseDialogActivity.MESSAGE_DATA_PASSPHRASE);
                    certifyLinkedIdentity(passphrase);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

}

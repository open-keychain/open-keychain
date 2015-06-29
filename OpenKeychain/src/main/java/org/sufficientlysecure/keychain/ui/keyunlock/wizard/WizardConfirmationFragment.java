package org.sufficientlysecure.keychain.ui.keyunlock.wizard;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.ServiceProgressHandler;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity;
import org.sufficientlysecure.keychain.ui.EditKeyActivity;
import org.sufficientlysecure.keychain.ui.dialog.ProgressDialogFragment;
import org.sufficientlysecure.keychain.ui.keyunlock.base.WizardFragment;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

/**
 * Confirmation fragment before creating the key
 */
public class WizardConfirmationFragment extends WizardFragment {
    public static final int REQUEST_EDIT_KEY = 0x00008007;

    private TextView mName;
    private TextView mEmail;
    private CheckBox mCreateKeyUpload;
    private TextView mCreateKeyEditText;
    private TextView mCreateKeyEditButton;

    private WizardConfirmationFragmentViewModel mWizardConfirmationFragmentViewModel;

    public static WizardConfirmationFragment newInstance() {
        return new WizardConfirmationFragment();
    }

    public WizardConfirmationFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mWizardConfirmationFragmentViewModel = new WizardConfirmationFragmentViewModel();
        mWizardConfirmationFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.wizard_confirmation_fragment_, container, false);
        mCreateKeyEditButton = (TextView) view.findViewById(R.id.create_key_edit_button);
        mCreateKeyEditText = (TextView) view.findViewById(R.id.create_key_edit_text);
        mCreateKeyUpload = (CheckBox) view.findViewById(R.id.create_key_upload);
        mEmail = (TextView) view.findViewById(R.id.email);
        mName = (TextView) view.findViewById(R.id.name);

        mName.setText(mWizardFragmentListener.getName());
        mEmail.setText(mWizardConfirmationFragmentViewModel.
                generateAdditionalEmails(mWizardFragmentListener.getEmail(),
                        mWizardFragmentListener.getAdditionalEmails()));


        mCreateKeyEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent edit = new Intent(getActivity(), EditKeyActivity.class);
                edit.putExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL,
                        mWizardConfirmationFragmentViewModel.getSaveKeyringParcel());
                startActivityForResult(edit, REQUEST_EDIT_KEY);
            }
        });
        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_EDIT_KEY: {
                if (resultCode == Activity.RESULT_OK) {
                    mWizardConfirmationFragmentViewModel.setSaveKeyringParcel((SaveKeyringParcel)
                            data.getParcelableExtra(EditKeyActivity.EXTRA_SAVE_KEYRING_PARCEL));
                    mCreateKeyEditText.setText(R.string.create_key_custom);
                }
                break;
            }
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mWizardConfirmationFragmentViewModel.setEmail(mWizardFragmentListener.getEmail().toString());
        mWizardConfirmationFragmentViewModel.setName(mWizardFragmentListener.getName().toString());
        mWizardConfirmationFragmentViewModel.setAdditionalEmails(mWizardFragmentListener.getAdditionalEmails());
        mWizardConfirmationFragmentViewModel.setPassphrase(mWizardFragmentListener.getPassphrase());

        mWizardConfirmationFragmentViewModel.prepareKeyRingData();
        if (mWizardConfirmationFragmentViewModel.isUseSmartCardSettings()) {
            mCreateKeyEditText.setText(R.string.create_key_custom);
        }
    }


    /**
     * Creates the key
     */
    private void createKey() {
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_EDIT_KEYRING);

        ServiceProgressHandler saveHandler = new ServiceProgressHandler(
                getActivity(),
                getString(R.string.progress_building_key),
                ProgressDialog.STYLE_HORIZONTAL,
                ProgressDialogFragment.ServiceType.KEYCHAIN_INTENT) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // get returned data bundle
                    Bundle returnData = message.getData();
                    if (returnData == null) {
                        return;
                    }
                    final EditKeyResult result =
                            returnData.getParcelable(OperationResult.EXTRA_RESULT);
                    if (result == null) {
                        Log.e(Constants.TAG, "result == null");
                        return;
                    }

                    if (result.mMasterKeyId != null && mCreateKeyUpload.isChecked()) {
                        // result will be displayed after upload
                        uploadKey(result);
                    } else {
                        Intent data = new Intent();
                        data.putExtra(OperationResult.EXTRA_RESULT, result);
                        getActivity().setResult(Activity.RESULT_OK, data);
                        getActivity().finish();
                    }
                }
            }
        };

        // fill values for this action
        Bundle data = new Bundle();
        // get selected key entries
        data.putParcelable(KeychainIntentService.EDIT_KEYRING_PARCEL,
                mWizardConfirmationFragmentViewModel.getSaveKeyringParcel());

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        saveHandler.showProgressDialog(getActivity());

        getActivity().startService(intent);
    }

    // TODO move into EditKeyOperation
    private void uploadKey(final EditKeyResult saveKeyResult) {
        // Send all information needed to service to upload key in other thread
        final Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        intent.setAction(KeychainIntentService.ACTION_UPLOAD_KEYRING);

        // set data uri as path to keyring
        Uri blobUri = KeychainContract.KeyRings.buildUnifiedKeyRingUri(
                saveKeyResult.mMasterKeyId);
        intent.setData(blobUri);

        // fill values for this action
        Bundle data = new Bundle();

        // upload to favorite keyserver
        String keyserver = Preferences.getPreferences(getActivity()).getPreferredKeyserver();
        data.putString(KeychainIntentService.UPLOAD_KEY_SERVER, keyserver);

        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        ServiceProgressHandler saveHandler = new ServiceProgressHandler(
                getActivity(),
                getString(R.string.progress_uploading),
                ProgressDialog.STYLE_HORIZONTAL,
                ProgressDialogFragment.ServiceType.KEYCHAIN_INTENT) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // TODO: upload operation needs a result!
                    // TODO: then combine these results
                    //if (result.getResult() == OperationResultParcel.RESULT_OK) {
                    //Notify.create(getActivity(), R.string.key_send_success,
                    //Notify.Style.OK).show();

                    Intent data = new Intent();
                    data.putExtra(OperationResult.EXTRA_RESULT, saveKeyResult);
                    getActivity().setResult(Activity.RESULT_OK, data);
                    getActivity().finish();
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        saveHandler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);
    }

    @Override
    public boolean onNextClicked() {
        createKey();
        return false;
    }
}

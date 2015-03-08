package org.sufficientlysecure.keychain.ui.linked;

import java.io.IOException;
import java.util.Arrays;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.LinkedVerifyResult;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;
import org.sufficientlysecure.keychain.pgp.linked.LinkedIdentity;
import org.sufficientlysecure.keychain.pgp.linked.LinkedResource;
import org.sufficientlysecure.keychain.pgp.linked.RawLinkedIdentity;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.adapter.LinkedIdsAdapter.ViewHolder;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.widget.CertifyKeySpinner;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;


public class LinkedIdViewFragment extends Fragment {

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;

    private static final String ARG_ENCODED_LID = "encoded_lid";
    private static final String ARG_VERIFIED = "verified";
    private static final String ARG_FINGERPRINT = "fingerprint";

    private RawLinkedIdentity mLinkedId;
    private LinkedCookieResource mLinkedResource;
    private Integer mVerified;

    private CardView vLinkedIdsCard;
    private Context mContext;
    private byte[] mFingerprint;
    private LayoutInflater mInflater;
    private LinearLayout vLinkedCerts;

    private View mCurrentCert;
    private boolean mInProgress;
    private ViewAnimator mButtonSwitcher;
    private CertifyKeySpinner vKeySpinner;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        byte[] data = args.getByteArray(ARG_ENCODED_LID);

        try {
            mLinkedId = LinkedIdentity.fromAttributeData(data);
        } catch (IOException e) {
            // TODO um…
            e.printStackTrace();
            throw new AssertionError("reconstruction of user attribute must succeed!");
        }

        if (mLinkedId instanceof LinkedIdentity) {
            LinkedResource res = ((LinkedIdentity) mLinkedId).mResource;
            mLinkedResource = (LinkedCookieResource) res;
        }

        mVerified = args.containsKey(ARG_VERIFIED) ? args.getInt(ARG_VERIFIED) : null;
        mFingerprint = args.getByteArray(ARG_FINGERPRINT);

        mContext = getActivity();
        mInflater = getLayoutInflater(savedInstanceState);

    }

    public static Fragment newInstance(RawLinkedIdentity id,
            Integer isVerified, byte[] fingerprint) throws IOException {
        LinkedIdViewFragment frag = new LinkedIdViewFragment();

        Bundle args = new Bundle();
        args.putByteArray(ARG_ENCODED_LID, id.toUserAttribute().getEncoded());
        if (isVerified != null) {
            args.putInt(ARG_VERIFIED, isVerified);
        }
        args.putByteArray(ARG_FINGERPRINT, fingerprint);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.linked_id_view_fragment, null);

        vLinkedIdsCard = (CardView) root.findViewById(R.id.card_linked_ids);
        vLinkedCerts = (LinearLayout) root.findViewById(R.id.linked_id_certs);
        vKeySpinner = (CertifyKeySpinner) root.findViewById(R.id.cert_key_spinner);

        View back = root.findViewById(R.id.back_button);
        back.setClickable(true);
        back.findViewById(R.id.back_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });

        ViewHolder holder = new ViewHolder(root);

        if (mVerified != null) {
            holder.vVerified.setVisibility(View.VISIBLE);
            switch (mVerified) {
                case Certs.VERIFIED_SECRET:
                    KeyFormattingUtils.setStatusImage(mContext, holder.vVerified,
                            null, State.VERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
                case Certs.VERIFIED_SELF:
                    KeyFormattingUtils.setStatusImage(mContext, holder.vVerified,
                            null, State.UNVERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
                default:
                    KeyFormattingUtils.setStatusImage(mContext, holder.vVerified,
                            null, State.INVALID, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
            }
        } else {
            holder.vVerified.setVisibility(View.GONE);
        }

        holder.setData(mContext, mLinkedId);

        // no resource, nothing further we can do…
        if (mLinkedResource == null) {
            root.findViewById(R.id.button_view).setVisibility(View.GONE);
            root.findViewById(R.id.button_verify).setVisibility(View.GONE);
            return root;
        }

        View button_view = root.findViewById(R.id.button_view);
        if (mLinkedResource.isViewable()) {
            button_view.setVisibility(View.VISIBLE);
            button_view.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = mLinkedResource.getViewIntent();
                    if (intent == null) {
                        return;
                    }
                    getActivity().startActivity(intent);
                }
            });
        } else {
            button_view.setVisibility(View.GONE);
        }

        mButtonSwitcher = (ViewAnimator) root.findViewById(R.id.button_animator);

        View button_verify = root.findViewById(R.id.button_verify);
        button_verify.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyResource();
            }
        });

        View button_retry = root.findViewById(R.id.button_retry);
        button_retry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyResource();
            }
        });

        View button_confirm = root.findViewById(R.id.button_confirm);
        button_confirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateCertifying();
            }
        });

        return root;
    }

    static class ViewHolderCert {
        final ViewAnimator vProgress;
        final ImageView vIcon;
        final TextView vText;

        ViewHolderCert(View view) {
            vProgress = (ViewAnimator) view.findViewById(R.id.linked_cert_progress);
            vIcon = (ImageView) view.findViewById(R.id.linked_cert_icon);
            vText = (TextView) view.findViewById(R.id.linked_cert_text);
        }
        void setShowProgress(boolean show) {
            vProgress.setDisplayedChild(show ? 0 : 1);
        }
    }

    void showButton(int which) {
        if (mButtonSwitcher.getDisplayedChild() == which) {
            return;
        }
        mButtonSwitcher.setDisplayedChild(which);
    }

    void verifyResource() {

        // only one at a time
        synchronized (this) {
            if (mInProgress) {
                return;
            }
            mInProgress = true;
        }

        // is there a current certification? if not create a new one
        final ViewHolderCert holder;
        if (mCurrentCert == null) {
            mCurrentCert = mInflater.inflate(R.layout.linked_id_cert, null);
            holder = new ViewHolderCert(mCurrentCert);
            mCurrentCert.setTag(holder);
            vLinkedCerts.addView(mCurrentCert);
        } else {
            holder = (ViewHolderCert) mCurrentCert.getTag();
        }

        vKeySpinner.setVisibility(View.GONE);
        holder.setShowProgress(true);
        holder.vText.setText("Verifying…");

        new AsyncTask<Void,Void,LinkedVerifyResult>() {
            @Override
            protected LinkedVerifyResult doInBackground(Void... params) {
                long timer = System.currentTimeMillis();
                LinkedVerifyResult result = mLinkedResource.verify(mFingerprint, mLinkedId.mNonce);

                // ux flow: this operation should take at last a second
                timer = System.currentTimeMillis() -timer;
                if (timer < 1000) try {
                    Thread.sleep(1000 -timer);
                } catch (InterruptedException e) {
                    // never mind
                }

                return result;
            }

            @Override
            protected void onPostExecute(LinkedVerifyResult result) {
                holder.setShowProgress(false);
                if (result.success()) {
                    holder.vText.setText("Ok");
                    setupForConfirmation();
                } else {
                    showButton(1);
                    holder.vText.setText("Error");
                }
                mInProgress = false;
            }
        }.execute();

    }

    void setupForConfirmation() {

        // button is 'confirm'
        showButton(2);

        vKeySpinner.setVisibility(View.VISIBLE);

    }

    private void initiateCertifying() {
        // get the user's passphrase for this key (if required)
        String passphrase;
        long certifyKeyId = vKeySpinner.getSelectedItemId();
        try {
            passphrase = PassphraseCacheService.getCachedPassphrase(
                    getActivity(), certifyKeyId, certifyKeyId);
        } catch (PassphraseCacheService.KeyNotFoundException e) {
            Log.e(Constants.TAG, "Key not found!", e);
            getActivity().finish();
            return;
        }
        if (passphrase == null) {
            Intent intent = new Intent(getActivity(), PassphraseDialogActivity.class);
            intent.putExtra(PassphraseDialogActivity.EXTRA_SUBKEY_ID, certifyKeyId);
            startActivityForResult(intent, REQUEST_CODE_PASSPHRASE);
            // bail out; need to wait until the user has entered the passphrase before trying again
        } else {
            certifyResource(certifyKeyId, "");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_PASSPHRASE: {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String passphrase = data.getStringExtra(
                            PassphraseDialogActivity.MESSAGE_DATA_PASSPHRASE);
                    long certifyKeyId = data.getLongExtra(PassphraseDialogActivity.EXTRA_KEY_ID, 0L);
                    if (certifyKeyId == 0L) {
                        throw new AssertionError("key id must not be 0");
                    }
                    certifyResource(certifyKeyId, passphrase);
                }
                return;
            }

            default: {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
    }

    private void certifyResource(long certifyKeyId, String passphrase) {

        Bundle data = new Bundle();
        {

            long masterKeyId = KeyFormattingUtils.convertFingerprintToKeyId(mFingerprint);
            CertifyAction action = new CertifyAction(masterKeyId, null,
                    Arrays.asList(mLinkedId.toUserAttribute()));

            // fill values for this action
            CertifyActionsParcel parcel = new CertifyActionsParcel(certifyKeyId);
            parcel.mCertifyActions.addAll(Arrays.asList(action));

            data.putParcelable(KeychainIntentService.CERTIFY_PARCEL, parcel);
            /* if (mUploadKeyCheckbox.isChecked()) {
                String keyserver = Preferences.getPreferences(getActivity()).getPreferredKeyserver();
                data.putString(KeychainIntentService.UPLOAD_KEY_SERVER, keyserver);
            } */
        }

        // Send all information needed to service to sign key in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);
        intent.setAction(KeychainIntentService.ACTION_CERTIFY_KEYRING);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Message is received after signing is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity(),
                getString(R.string.progress_certifying), ProgressDialog.STYLE_SPINNER, false) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    Bundle data = message.getData();
                    CertifyResult result = data.getParcelable(CertifyResult.EXTRA_RESULT);

                    result.createNotify(getActivity()).show();
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

}

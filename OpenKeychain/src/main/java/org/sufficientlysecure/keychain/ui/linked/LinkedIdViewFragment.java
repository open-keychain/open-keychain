package org.sufficientlysecure.keychain.ui.linked;

import java.io.IOException;
import java.util.Arrays;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.LinkedVerifyResult;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;
import org.sufficientlysecure.keychain.pgp.linked.LinkedIdentity;
import org.sufficientlysecure.keychain.pgp.linked.LinkedResource;
import org.sufficientlysecure.keychain.pgp.linked.RawLinkedIdentity;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.provider.KeychainDatabase.Tables;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.service.PassphraseCacheService;
import org.sufficientlysecure.keychain.ui.PassphraseDialogActivity;
import org.sufficientlysecure.keychain.ui.adapter.LinkedIdsAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.linked.LinkedIdViewFragment.ViewHolder.VerifyState;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.widget.CertListWidget;
import org.sufficientlysecure.keychain.ui.widget.CertifyKeySpinner;
import org.sufficientlysecure.keychain.util.Log;


public class LinkedIdViewFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>, OnBackStackChangedListener {

    public static final int REQUEST_CODE_PASSPHRASE = 0x00008001;

    private static final String ARG_DATA_URI = "data_uri";
    private static final String ARG_LID_RANK = "rank";
    private static final String ARG_IS_SECRET = "verified";
    private static final String ARG_FINGERPRINT = "fingerprint";
    private static final int LOADER_ID_LINKED_ID = 1;

    private RawLinkedIdentity mLinkedId;
    private LinkedCookieResource mLinkedResource;
    private boolean mIsSecret;

    private Context mContext;
    private byte[] mFingerprint;

    private AsyncTask mInProgress;

    private Uri mDataUri;
    private ViewHolder mViewHolder;
    private int mLidRank;
    private OnIdentityLoadedListener mIdLoadedListener;

    public static LinkedIdViewFragment newInstance(Uri dataUri, int rank,
            boolean isSecret, byte[] fingerprint) throws IOException {
        LinkedIdViewFragment frag = new LinkedIdViewFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_DATA_URI, dataUri);
        args.putInt(ARG_LID_RANK, rank);
        args.putBoolean(ARG_IS_SECRET, isSecret);
        args.putByteArray(ARG_FINGERPRINT, fingerprint);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mDataUri = args.getParcelable(ARG_DATA_URI);
        mLidRank = args.getInt(ARG_LID_RANK);

        mIsSecret = args.getBoolean(ARG_IS_SECRET);
        mFingerprint = args.getByteArray(ARG_FINGERPRINT);

        mContext = getActivity();

        getLoaderManager().initLoader(LOADER_ID_LINKED_ID, null, this);

    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case LOADER_ID_LINKED_ID:
                return new CursorLoader(getActivity(), mDataUri,
                        UserIdsAdapter.USER_PACKETS_PROJECTION,
                        Tables.USER_PACKETS + "." + UserPackets.RANK
                                + " = " + Integer.toString(mLidRank), null, null);
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_ID_LINKED_ID:

                if (!cursor.moveToFirst()) {
                    Notify.createNotify(getActivity(), "Error loading identity!",
                            Notify.LENGTH_LONG, Style.ERROR).show();
                    finishFragment();
                    break;
                }

                try {
                    int certStatus = cursor.getInt(UserIdsAdapter.INDEX_VERIFIED);

                    byte[] data = cursor.getBlob(UserIdsAdapter.INDEX_ATTRIBUTE_DATA);
                    RawLinkedIdentity linkedId = LinkedIdentity.fromAttributeData(data);

                    loadIdentity(linkedId, certStatus);

                    if (mIdLoadedListener != null) {
                        mIdLoadedListener.onIdentityLoaded();
                        mIdLoadedListener = null;
                    }

                } catch (IOException e) {
                    Log.e(Constants.TAG, "error parsing identity", e);
                    Notify.createNotify(getActivity(), "Error parsing identity!",
                            Notify.LENGTH_LONG, Style.ERROR).show();
                    finishFragment();
                }

                break;
        }
    }

    public void finishFragment() {
        FragmentManager manager = getFragmentManager();
        manager.removeOnBackStackChangedListener(this);
        manager.popBackStack("linked_id", FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    public interface OnIdentityLoadedListener {
        public void onIdentityLoaded();
    }

    public void setOnIdentityLoadedListener(OnIdentityLoadedListener listener) {
        mIdLoadedListener = listener;
    }

    private void loadIdentity(RawLinkedIdentity linkedId, int certStatus) {
        mLinkedId = linkedId;

        setShowVerifying(false);

        if (mLinkedId instanceof LinkedIdentity) {
            LinkedResource res = ((LinkedIdentity) mLinkedId).mResource;
            mLinkedResource = (LinkedCookieResource) res;
        }

        if (!mIsSecret) {
            mViewHolder.mLinkedIdHolder.vVerified.setVisibility(View.VISIBLE);

            switch (certStatus) {
                case Certs.VERIFIED_SECRET:
                    KeyFormattingUtils.setStatusImage(mContext, mViewHolder.mLinkedIdHolder.vVerified,
                            null, State.VERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
                case Certs.VERIFIED_SELF:
                    KeyFormattingUtils.setStatusImage(mContext, mViewHolder.mLinkedIdHolder.vVerified,
                            null, State.UNVERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
                default:
                    KeyFormattingUtils.setStatusImage(mContext, mViewHolder.mLinkedIdHolder.vVerified,
                            null, State.INVALID, KeyFormattingUtils.DEFAULT_COLOR);
                    break;
            }
        } else {
            mViewHolder.mLinkedIdHolder.vVerified.setVisibility(View.GONE);
        }

        mViewHolder.mLinkedIdHolder.setData(mContext, mLinkedId);

        // no resource, nothing further we can do…
        if (mLinkedResource == null) {
            mViewHolder.vButtonView.setVisibility(View.GONE);
            mViewHolder.vButtonVerify.setVisibility(View.GONE);
            return;
        }

        if (mLinkedResource.isViewable()) {
            mViewHolder.vButtonView.setVisibility(View.VISIBLE);
            mViewHolder.vButtonView.setOnClickListener(new OnClickListener() {
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
            mViewHolder.vButtonView.setVisibility(View.GONE);
        }

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    static class ViewHolder {
        private final View vButtonView;
        private final ViewAnimator vVerifyingContainer;
        private final ViewAnimator vItemCertified;
        LinkedIdsAdapter.ViewHolder mLinkedIdHolder;

        private ViewAnimator vButtonSwitcher;
        private CertListWidget vLinkedCerts;
        private CertifyKeySpinner vKeySpinner;
        private final View vButtonVerify;
        private final View vButtonRetry;
        private final View vButtonConfirm;
        private final View vButtonBack;

        private final ViewAnimator vProgress;
        private final TextView vText;

        ViewHolder(View root) {
            vLinkedCerts = (CertListWidget) root.findViewById(R.id.linked_id_certs);
            vKeySpinner = (CertifyKeySpinner) root.findViewById(R.id.cert_key_spinner);
            vButtonSwitcher = (ViewAnimator) root.findViewById(R.id.button_animator);

            mLinkedIdHolder = new LinkedIdsAdapter.ViewHolder(root);

            vButtonBack = root.findViewById(R.id.back_button);
            vButtonVerify = root.findViewById(R.id.button_verify);
            vButtonRetry = root.findViewById(R.id.button_retry);
            vButtonConfirm = root.findViewById(R.id.button_confirm);
            vButtonView = root.findViewById(R.id.button_view);

            vVerifyingContainer = (ViewAnimator) root.findViewById(R.id.linked_verify_container);
            vItemCertified = (ViewAnimator) root.findViewById(R.id.linked_id_certified);

            vProgress = (ViewAnimator) root.findViewById(R.id.linked_cert_progress);
            vText = (TextView) root.findViewById(R.id.linked_cert_text);
        }

        enum VerifyState {
            VERIFYING, VERIFY_OK, VERIFY_ERROR, CERTIFYING
        }

        void setVerifyingState(VerifyState state, boolean isSecret) {
            switch (state) {
                case VERIFYING:
                    vProgress.setDisplayedChild(0);
                    vText.setText("Verifying…");
                    vKeySpinner.setVisibility(View.GONE);
                    break;

                case VERIFY_OK:
                    vProgress.setDisplayedChild(1);
                    if (!isSecret) {
                        showButton(2);
                        vKeySpinner.setVisibility(View.VISIBLE);
                    } else {
                        showButton(1);
                        vKeySpinner.setVisibility(View.GONE);
                    }
                    break;

                case VERIFY_ERROR:
                    showButton(1);
                    vProgress.setDisplayedChild(2);
                    vText.setText("Error");
                    vKeySpinner.setVisibility(View.GONE);
                    break;

                case CERTIFYING:
                    vProgress.setDisplayedChild(0);
                    vText.setText("Confirming…");
                    vKeySpinner.setVisibility(View.GONE);
                    break;
            }
        }

        void showVerifyingContainer(boolean show) {
            if (vVerifyingContainer.getDisplayedChild() == (show ? 1 : 0)) {
                return;
            }

            vVerifyingContainer.setDisplayedChild(show ? 1 : 0);
            vItemCertified.setDisplayedChild(show ? 1 : 0);
        }

        void showButton(int which) {
            if (vButtonSwitcher.getDisplayedChild() == which) {
                return;
            }
            vButtonSwitcher.setDisplayedChild(which);
        }

    }

    private boolean mVerificationState = false;
    /** Switches between the 'verifying' ui bit and certificate status. This method
     * must behave correctly in all states, showing or hiding the appropriate views
     * and cancelling pending operations where necessary.
     *
     * This method also handles back button functionality in combination with
     * onBackStateChanged.
     */
    void setShowVerifying(boolean show) {
        if (!show) {
            if (mInProgress != null) {
                mInProgress.cancel(false);
                mInProgress = null;
            }
            getFragmentManager().removeOnBackStackChangedListener(this);
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    getFragmentManager().popBackStack("verification",
                            FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }
            });

            if (!mVerificationState) {
                return;
            }
            mVerificationState = false;

            mViewHolder.showButton(0);
            mViewHolder.vKeySpinner.setVisibility(View.GONE);
            mViewHolder.showVerifyingContainer(false);
            return;
        }

        if (mVerificationState) {
            return;
        }
        mVerificationState = true;

        FragmentManager manager = getFragmentManager();
        manager.beginTransaction().addToBackStack("verification").commit();
        manager.executePendingTransactions();
        manager.addOnBackStackChangedListener(this);
        mViewHolder.showVerifyingContainer(true);

    }

    @Override
    public void onBackStackChanged() {
        setShowVerifying(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.linked_id_view_fragment, null);

        mViewHolder = new ViewHolder(root);
        root.setTag(mViewHolder);

        ((ImageView) root.findViewById(R.id.status_icon_verified))
                .setColorFilter(mContext.getResources().getColor(R.color.android_green_light),
                        PorterDuff.Mode.SRC_IN);
        ((ImageView) root.findViewById(R.id.status_icon_invalid))
                .setColorFilter(mContext.getResources().getColor(R.color.android_red_light),
                        PorterDuff.Mode.SRC_IN);



        mViewHolder.vButtonBack.setClickable(true);
        mViewHolder.vButtonBack.findViewById(R.id.back_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });

        mViewHolder.vButtonVerify.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyResource();
            }
        });
        mViewHolder.vButtonRetry.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyResource();
            }
        });
        mViewHolder.vButtonConfirm.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                initiateCertifying();
            }
        });

        {
            Bundle args = new Bundle();
            args.putParcelable(CertListWidget.ARG_URI, Certs.buildLinkedIdCertsUri(mDataUri, mLidRank));
            args.putBoolean(CertListWidget.ARG_IS_SECRET, mIsSecret);
            getLoaderManager().initLoader(CertListWidget.LOADER_ID_LINKED_CERTS,
                    args, mViewHolder.vLinkedCerts);
        }

        return root;
    }

    void verifyResource() {

        // only one at a time (no sync needed, mInProgress is only touched in ui thread)
        if (mInProgress != null) {
            return;
        }

        setShowVerifying(true);

        mViewHolder.vKeySpinner.setVisibility(View.GONE);
        mViewHolder.setVerifyingState(VerifyState.VERIFYING, mIsSecret);

        mInProgress = new AsyncTask<Void,Void,LinkedVerifyResult>() {
            @Override
            protected LinkedVerifyResult doInBackground(Void... params) {
                long timer = System.currentTimeMillis();
                LinkedVerifyResult result = mLinkedResource.verify(mFingerprint);

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
                if (isCancelled()) {
                    return;
                }
                if (result.success()) {
                    mViewHolder.vText.setText(mLinkedResource.getVerifiedText());
                    mViewHolder.setVerifyingState(VerifyState.VERIFY_OK, mIsSecret);
                } else {
                    mViewHolder.setVerifyingState(VerifyState.VERIFY_ERROR, mIsSecret);
                    result.createNotify(getActivity()).show();
                }
                mInProgress = null;
            }
        }.execute();

    }

    private void initiateCertifying() {

        if (mIsSecret) {
            return;
        }

        // get the user's passphrase for this key (if required)
        String passphrase;
        long certifyKeyId = mViewHolder.vKeySpinner.getSelectedItemId();
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

        if (mIsSecret) {
            return;
        }

        mViewHolder.setVerifyingState(VerifyState.CERTIFYING, false);

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
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity()) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                Bundle data = message.getData();

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    CertifyResult result = data.getParcelable(CertifyResult.EXTRA_RESULT);
                    result.createNotify(getActivity()).show();
                }

            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // start service with intent
        getActivity().startService(intent);

    }

}

package org.sufficientlysecure.keychain.ui.linked;


import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.linked.LinkedAttribute;
import org.sufficientlysecure.keychain.linked.LinkedTokenResource;
import org.sufficientlysecure.keychain.operations.results.LinkedVerifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;


public abstract class LinkedIdCreateFinalFragment extends CryptoOperationFragment {

    protected LinkedIdWizard mLinkedIdWizard;

    private ImageView mVerifyImage;
    private TextView mVerifyStatus;
    private ViewAnimator mVerifyAnimator;

    // This is a resource, set AFTER it has been verified
    LinkedTokenResource mVerifiedResource = null;
    private ViewAnimator mVerifyButtonAnimator;

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
                cryptoOperation();
            }
        });

        view.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLinkedIdWizard.loadFragment(null, null, LinkedIdWizard.FRAG_ACTION_TO_LEFT);
            }
        });

        mVerifyAnimator = (ViewAnimator) view.findViewById(R.id.verify_progress);
        mVerifyImage = (ImageView) view.findViewById(R.id.verify_image);
        mVerifyStatus = (TextView) view.findViewById(R.id.verify_status);
        mVerifyButtonAnimator = (ViewAnimator) view.findViewById(R.id.verify_buttons);

        view.findViewById(R.id.button_verify).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofVerify();
            }
        });

        view.findViewById(R.id.button_retry).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                proofVerify();
            }
        });

        setVerifyProgress(false, null);
        mVerifyStatus.setText(R.string.linked_verify_pending);

        return view;
    }

    abstract LinkedTokenResource getResource(OperationLog log);

    private void setVerifyProgress(boolean on, Boolean success) {
        if (success == null) {
            mVerifyStatus.setText(R.string.linked_verifying);
            displayButton(on ? 2 : 0);
        } else if (success) {
            mVerifyStatus.setText(R.string.linked_verify_success);
            mVerifyImage.setImageResource(R.drawable.status_signature_verified_cutout_24dp);
            mVerifyImage.setColorFilter(getResources().getColor(R.color.android_green_dark),
                    PorterDuff.Mode.SRC_IN);
            displayButton(2);
        } else {
            mVerifyStatus.setText(R.string.linked_verify_error);
            mVerifyImage.setImageResource(R.drawable.status_signature_unknown_cutout_24dp);
            mVerifyImage.setColorFilter(getResources().getColor(R.color.android_red_dark),
                    PorterDuff.Mode.SRC_IN);
            displayButton(1);
        }
        mVerifyAnimator.setDisplayedChild(on ? 1 : 0);
    }

    public void displayButton(int button) {
        if (mVerifyButtonAnimator.getDisplayedChild() == button) {
            return;
        }
        mVerifyButtonAnimator.setDisplayedChild(button);
    }

    protected void proofVerify() {
        setVerifyProgress(true, null);

        new AsyncTask<Void,Void,LinkedVerifyResult>() {

            @Override
            protected LinkedVerifyResult doInBackground(Void... params) {
                long timer = System.currentTimeMillis();

                OperationLog log = new OperationLog();
                LinkedTokenResource resource = getResource(log);
                if (resource == null) {
                    return new LinkedVerifyResult(LinkedVerifyResult.RESULT_ERROR, log);
                }

                LinkedVerifyResult result = resource.verify(getActivity(), mLinkedIdWizard.mFingerprint);

                // ux flow: this operation should take at last a second
                timer = System.currentTimeMillis() -timer;
                if (timer < 1000) try {
                    Thread.sleep(1000 -timer);
                } catch (InterruptedException e) {
                    // never mind
                }

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
                    result.createNotify(getActivity()).show(LinkedIdCreateFinalFragment.this);
                }
            }
        }.execute();

    }

    @Override
    protected void cryptoOperation() {
        if (mVerifiedResource == null) {
            Notify.create(getActivity(), R.string.linked_need_verify, Notify.Style.ERROR)
                    .show(LinkedIdCreateFinalFragment.this);
            return;
        }

        super.cryptoOperation();
    }

    @Override
    protected void cryptoOperation(CryptoInputParcel cryptoInput) {
        if (mVerifiedResource == null) {
            Notify.create(getActivity(), R.string.linked_need_verify, Notify.Style.ERROR)
                    .show(LinkedIdCreateFinalFragment.this);
            return;
        }

        super.cryptoOperation(cryptoInput);
    }

    @Nullable
    @Override
    public Parcelable createOperationInput() {
        SaveKeyringParcel skp =
                new SaveKeyringParcel(mLinkedIdWizard.mMasterKeyId, mLinkedIdWizard.mFingerprint);

        WrappedUserAttribute ua =
                LinkedAttribute.fromResource(mVerifiedResource).toUserAttribute();

        skp.mAddUserAttribute.add(ua);

        return skp;
    }

    @Override
    public void onCryptoOperationSuccess(OperationResult result) {
        // if bad -> display here!
        if (!result.success()) {
            result.createNotify(getActivity()).show(LinkedIdCreateFinalFragment.this);
            return;
        }

        getActivity().finish();
    }

    @Override
    public void onCryptoOperationError(OperationResult result) {

    }

}

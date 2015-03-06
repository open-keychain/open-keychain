package org.sufficientlysecure.keychain.ui.linked;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.LinkedVerifyResult;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;
import org.sufficientlysecure.keychain.pgp.linked.LinkedIdentity;
import org.sufficientlysecure.keychain.pgp.linked.LinkedResource;
import org.sufficientlysecure.keychain.pgp.linked.RawLinkedIdentity;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.ui.adapter.LinkedIdsAdapter.ViewHolder;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;


public class LinkedIdViewFragment extends Fragment {

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
                Notify.createNotify(getActivity(), "confirmed!", Notify.LENGTH_LONG, Style.INFO).show();
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

    public void verifyResource() {

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

        holder.setShowProgress(true);
        holder.vText.setText("Verifying…");

        new AsyncTask<Void,Void,LinkedVerifyResult>() {
            @Override
            protected LinkedVerifyResult doInBackground(Void... params) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // nvm
                }
                return mLinkedResource.verify(mFingerprint, mLinkedId.mNonce);
            }

            @Override
            protected void onPostExecute(LinkedVerifyResult result) {
                holder.setShowProgress(false);
                if (result.success()) {
                    mButtonSwitcher.setDisplayedChild(2);
                    holder.vText.setText("Ok");
                } else {
                    mButtonSwitcher.setDisplayedChild(1);
                    holder.vText.setText("Error");
                }
                mInProgress = false;
            }
        }.execute();

    }

}

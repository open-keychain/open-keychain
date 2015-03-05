package org.sufficientlysecure.keychain.ui.linked;

import java.io.IOException;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.linked.LinkedCookieResource;
import org.sufficientlysecure.keychain.pgp.linked.LinkedIdentity;
import org.sufficientlysecure.keychain.pgp.linked.LinkedResource;
import org.sufficientlysecure.keychain.pgp.linked.RawLinkedIdentity;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.ui.adapter.LinkedIdsAdapter.ViewHolder;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;


public class LinkedIdViewFragment extends Fragment {

    private static final String EXTRA_ENCODED_LID = "encoded_lid";
    private static final String EXTRA_VERIFIED = "verified";

    private RawLinkedIdentity mLinkedId;
    private LinkedCookieResource mLinkedResource;
    private Integer mVerified;

    private CardView vLinkedIdsCard;
    private Context mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        byte[] data = args.getByteArray(EXTRA_ENCODED_LID);

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

        mVerified = args.containsKey(EXTRA_VERIFIED) ? args.getInt(EXTRA_VERIFIED) : null;

        mContext = getActivity();
    }

    public static Fragment newInstance(RawLinkedIdentity id, Integer isVerified) throws IOException {
        LinkedIdViewFragment frag = new LinkedIdViewFragment();

        Bundle args = new Bundle();
        args.putByteArray(EXTRA_ENCODED_LID, id.toUserAttribute().getEncoded());
        if (isVerified != null) {
            args.putInt(EXTRA_VERIFIED, isVerified);
        }
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.linked_id_view_fragment, null);

        vLinkedIdsCard = (CardView) root.findViewById(R.id.card_linked_ids);

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

        View button_verify = root.findViewById(R.id.button_verify);
        button_verify.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                verifyResource();
            }
        });

        return root;
    }

    public void verifyResource() {

        // TODO

    }

}

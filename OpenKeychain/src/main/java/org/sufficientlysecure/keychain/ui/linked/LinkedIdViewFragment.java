package org.sufficientlysecure.keychain.ui.linked;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.linked.RawLinkedIdentity;


public class LinkedIdViewFragment extends Fragment {

    private CardView mLinkedIdsCard;

    public static Fragment newInstance(RawLinkedIdentity id) {
        LinkedIdViewFragment frag = new LinkedIdViewFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.linked_id_view_fragment, null);

        mLinkedIdsCard = (CardView) root.findViewById(R.id.card_linked_ids);

        root.findViewById(R.id.back_button).setClickable(true);
        root.findViewById(R.id.back_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().popBackStack();
            }
        });

        return root;
    }

}

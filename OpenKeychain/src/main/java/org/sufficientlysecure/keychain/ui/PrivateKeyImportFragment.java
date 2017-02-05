package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;

public class PrivateKeyImportFragment extends Fragment {
    private Activity mActivity;
    private boolean mConnected;

    public static PrivateKeyImportFragment newInstance() {
        PrivateKeyImportFragment frag = new PrivateKeyImportFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = (Activity) context;
    }

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.private_key_import_fragment, c, false);

        final View ipLayout = view.findViewById(R.id.private_key_import_ip_layout);
        EditText ipAddressEdit = (EditText) view.findViewById(R.id.private_key_import_ip_address);
        EditText portEdit = (EditText) view.findViewById(R.id.private_key_import_port);

        final View sentenceLayout = view.findViewById(R.id.private_key_import_sentence_layout);
        EditText sentenceEdit = (EditText) view.findViewById(R.id.private_key_import_sentence);

        Button okButtion = (Button) view.findViewById(R.id.private_key_import_ok);
        okButtion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mConnected) {
                    mConnected = true;
                    ipLayout.setVisibility(View.GONE);
                    sentenceLayout.setVisibility(View.VISIBLE);
                } else {
                    mActivity.finish();
                }
            }
        });


        return view;
    }

}

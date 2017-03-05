package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cryptolib.SecureDataSocket;
import com.cryptolib.SecureDataSocketException;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.Notify;

import java.io.IOException;

public class PrivateKeyImportFragment extends Fragment {
    public static final String EXTRA_RECEIVED_KEYRING = "received_keyring";

    private Activity mActivity;
    private int mPort;
    private String mIpAddress;

    private Button mOkButton;
    private TextView mSentenceText;
    private View mIpLayout;
    private View mSentenceLayout;
    private View mButtonLayout;

    private SecureDataSocket mSecureDataSocket;
    boolean mPhrasesMatched;

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

        mIpLayout = view.findViewById(R.id.private_key_import_ip_layout);
        EditText ipAddressEdit = (EditText) view.findViewById(R.id.private_key_import_ip_address);
        EditText portEdit = (EditText) view.findViewById(R.id.private_key_import_port);

        mSentenceLayout = view.findViewById(R.id.private_key_import_sentence_layout);
        mSentenceText = (TextView) view.findViewById(R.id.private_key_import_sentence);

        mOkButton = (Button) view.findViewById(R.id.private_key_import_ok);
        mOkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new ConnectTask().execute();
                mOkButton.setEnabled(false);
            }
        });

        ipAddressEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mIpAddress = charSequence.toString();
                mOkButton.setEnabled(mPort > 0 && mIpAddress.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        portEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mPort = Integer.valueOf(charSequence.toString());
                mOkButton.setEnabled(mPort > 0 && mIpAddress.length() > 0);
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        mButtonLayout = view.findViewById(R.id.private_key_import_button_layout);
        Button buttonSentenceNotMatched = (Button) view.findViewById(R.id.private_key_import_sentence_not_matched_button);
        Button buttonSentenceMatched = (Button) view.findViewById(R.id.private_key_import_sentence_matched_button);

        buttonSentenceNotMatched.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPhrasesMatched = false;
                new Thread(mReceiveKey).start();
            }
        });

        buttonSentenceMatched.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPhrasesMatched = true;
                new Thread(mReceiveKey).start();
            }
        });


        return view;
    }

    private Runnable mReceiveKey = new Runnable() {
        @Override
        public void run() {
            try {
                mSecureDataSocket.comparedPhrases(mPhrasesMatched);
            } catch (SecureDataSocketException e) {
                e.printStackTrace();
            }

            if (mPhrasesMatched) {
                byte[] keyRing = null;

                try {
                    keyRing = mSecureDataSocket.read();
                } catch (SecureDataSocketException e) {
                    e.printStackTrace();
                }

                mSecureDataSocket.close();

                if (keyRing != null) {
                    Intent intent = new Intent();
                    intent.putExtra(EXTRA_RECEIVED_KEYRING, keyRing);

                    mActivity.setResult(Activity.RESULT_OK, intent);
                }
            }

            mActivity.finish();
        }
    };

    private class ConnectTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            mSecureDataSocket = new SecureDataSocket(mPort);

            String connectionDetails = mIpAddress + ":" + mPort;
            String comparePhrase = null;
            try {
                comparePhrase = mSecureDataSocket.setupClientNoCamera(connectionDetails);
            } catch (SecureDataSocketException e) {
                e.printStackTrace();
            }

            return comparePhrase;
        }

        @Override
        protected void onPostExecute(String comparePhrase) {
            if (comparePhrase == null) {
                Notify.create(getActivity(), R.string.private_key_import_error, Notify.Style.ERROR).show();
                mOkButton.setEnabled(true);
                return;
            }

            mSentenceText.setText(comparePhrase);

            mOkButton.setVisibility(View.GONE);
            mIpLayout.setVisibility(View.GONE);
            mSentenceLayout.setVisibility(View.VISIBLE);
            mButtonLayout.setVisibility(View.VISIBLE);
        }
    }
}

package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
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

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.network.KeyImportSocket;
import org.sufficientlysecure.keychain.network.NetworkReceiver;
import org.sufficientlysecure.keychain.ui.util.Notify;

public class PrivateKeyImportFragment extends Fragment implements KeyImportSocket.KeyImportListener{
    public static final String EXTRA_RECEIVED_KEYRING = "received_keyring";

    private Activity mActivity;
    private int mPort;
    private String mIpAddress;

    private Button mOkButton;
    private TextView mSentenceText;
    private View mIpLayout;
    private View mSentenceLayout;
    private View mButtonLayout;

    private KeyImportSocket mSocket;

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
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSocket = KeyImportSocket.getInstance(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mActivity.isFinishing()) {
            mSocket.close();
        }
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
                if (NetworkReceiver.isConnectedTypeWifi(mActivity)) {
                    mSocket.startImport(mIpAddress, mPort);
                    mOkButton.setEnabled(false);
                } else {
                    Notify.create(mActivity, R.string.private_key_error_wifi, Notify.Style.ERROR).show();
                }
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
                try {
                    mPort = Integer.valueOf(charSequence.toString());
                    mOkButton.setEnabled(mPort > 0 && mIpAddress.length() > 0);
                } catch (NumberFormatException e) {
                    mPort = 0;
                }
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
                mSocket.importPhrasesMatched(false);
                mActivity.finish();
            }
        });

        buttonSentenceMatched.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.importPhrasesMatched(true);
            }
        });


        return view;
    }

    @Override
    public void showPhrase(String phrase) {
        if (phrase == null) {
            Notify.create(getActivity(), R.string.private_key_import_error_connection, Notify.Style.ERROR).show();
            mOkButton.setEnabled(true);
            return;
        }

        mSentenceText.setText(phrase);
        switchMode(true);
    }

    @Override
    public void importKey(byte[] keyRing) {
        if (keyRing == null) {
            Notify.create(getActivity(), R.string.private_key_import_error_key, Notify.Style.ERROR).show();
            switchMode(false);

            // socket is already closed, initialize a new one
            mSocket = KeyImportSocket.getInstance(this);

            return;
        }

        Intent keyIntent = new Intent();
        keyIntent.putExtra(EXTRA_RECEIVED_KEYRING, keyRing);

        mActivity.setResult(Activity.RESULT_OK, keyIntent);
        mActivity.finish();
    }

    private void switchMode(boolean switchToManual) {
        mOkButton.setEnabled(true);
        mOkButton.setVisibility(switchToManual ? View.GONE : View.VISIBLE);
        mIpLayout.setVisibility(switchToManual ? View.GONE : View.VISIBLE);
        mSentenceLayout.setVisibility(switchToManual ? View.VISIBLE : View.GONE);
        mButtonLayout.setVisibility(switchToManual ? View.VISIBLE : View.GONE);
    }
}

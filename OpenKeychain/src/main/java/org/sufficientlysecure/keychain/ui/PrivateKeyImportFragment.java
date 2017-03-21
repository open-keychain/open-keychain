package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import org.sufficientlysecure.keychain.ui.widget.ToolableViewAnimator;

public class PrivateKeyImportFragment extends Fragment implements KeyImportSocket.KeyImportListener{
    public static final String EXTRA_RECEIVED_KEY = "received_key";

    private static final String ARG_PHRASE = "phrase";
    private static final String ARG_CURRENT_STATE = "current_state";

    private enum ImportState {
        STATE_UNINITIALIZED, STATE_ENTER_INFO, STATE_CONNECTING, STATE_PHRASE
    }

    private Activity mActivity;
    private int mPort;
    private String mIpAddress;

    private ToolableViewAnimator mTitleAnimator, mContentAnimator, mButtonAnimator;
    private TextView mPhraseText;

    private KeyImportSocket mSocket;
    private ImportState mCurrentState = ImportState.STATE_UNINITIALIZED;
    private String mPhrase;

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
        mActivity.setTitle(R.string.title_import_private_key);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSocket = KeyImportSocket.getInstance(this);

        if (savedInstanceState != null) {
            mPhrase = savedInstanceState.getString(ARG_PHRASE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_PHRASE, mPhrase);
        outState.putInt(ARG_CURRENT_STATE, mCurrentState.ordinal());
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

        mTitleAnimator = (ToolableViewAnimator) view.findViewById(R.id.private_key_import_title_animator);
        mContentAnimator = (ToolableViewAnimator) view.findViewById(R.id.private_key_import_animator);
        mButtonAnimator = (ToolableViewAnimator) view.findViewById(R.id.private_key_import_button_bar_animator);


        EditText ipAddressEdit = (EditText) view.findViewById(R.id.private_key_import_ip_address);
        EditText portEdit = (EditText) view.findViewById(R.id.private_key_import_port);

        mPhraseText = (TextView) view.findViewById(R.id.private_key_import_phrase);
        Button okButton = (Button) view.findViewById(R.id.private_key_import_ok);
        Button buttonSentenceNotMatched = (Button) view.findViewById(R.id.private_key_import_phrase_not_matched_button);
        Button buttonSentenceMatched = (Button) view.findViewById(R.id.private_key_import_phrase_matched_button);

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPort <= 0 || mIpAddress == null || mIpAddress.length() <= 0) {
                    Notify.create(mActivity, R.string.private_key_import_error_ip_port, Notify.Style.ERROR).show();
                } else if (!NetworkReceiver.isConnectedTypeWifi(mActivity)) {
                    Notify.create(mActivity, R.string.private_key_error_wifi, Notify.Style.ERROR).show();
                } else {
                    switchState(ImportState.STATE_CONNECTING, true);
                    mSocket.startImport(mIpAddress, mPort);
                }
            }
        });

        ipAddressEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                mIpAddress = charSequence.toString();
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
                } catch (NumberFormatException e) {
                    mPort = 0;
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });

        buttonSentenceNotMatched.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.phrasesMatched(false);
                mActivity.finish();
            }
        });

        buttonSentenceMatched.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.phrasesMatched(true);
            }
        });


        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            ImportState savedState = ImportState.values()[savedInstanceState.getInt(ARG_CURRENT_STATE)];
            switchState(savedState, false);

            mPhraseText.setText(mPhrase);
        } else if (mCurrentState == ImportState.STATE_UNINITIALIZED) {
            switchState(ImportState.STATE_ENTER_INFO, true);
        }
    }

    void switchState(ImportState state, boolean animate) {

        switch (state) {
            case STATE_UNINITIALIZED:
                throw new AssertionError("can't switch to uninitialized state, this is a bug!");

            case STATE_ENTER_INFO:
                mTitleAnimator.setDisplayedChild(0, animate);
                mContentAnimator.setDisplayedChild(0, animate);
                mButtonAnimator.setDisplayedChild(0, animate);
                break;

            case STATE_CONNECTING:
                mTitleAnimator.setDisplayedChild(1, animate);
                mContentAnimator.setDisplayedChild(1, animate);
                mButtonAnimator.setDisplayedChild(1, animate);
                break;

            case STATE_PHRASE:
                mTitleAnimator.setDisplayedChild(2, animate);
                mContentAnimator.setDisplayedChild(2, animate);
                mButtonAnimator.setDisplayedChild(2, animate);
                break;
        }

        mCurrentState = state;
    }

    @Override
    public void showPhrase(String phrase) {
        if (phrase == null) {
            Notify.create(getActivity(), R.string.private_key_import_error_connection, Notify.Style.ERROR).show();
            switchState(ImportState.STATE_ENTER_INFO, false);
            return;
        }

        mPhrase = phrase;
        mPhraseText.setText(phrase);
        switchState(ImportState.STATE_PHRASE, true);
    }

    @Override
    public void importKey(byte[] key) {
        if (key == null) {
            Notify.create(getActivity(), R.string.private_key_import_error_key, Notify.Style.ERROR).show();
            switchState(ImportState.STATE_ENTER_INFO, false);

            // socket is already closed, initialize a new one
            mSocket = KeyImportSocket.getInstance(this);

            return;
        }

        Intent keyIntent = new Intent();
        keyIntent.putExtra(EXTRA_RECEIVED_KEY, key);

        mActivity.setResult(Activity.RESULT_OK, keyIntent);
        mActivity.finish();
    }
}

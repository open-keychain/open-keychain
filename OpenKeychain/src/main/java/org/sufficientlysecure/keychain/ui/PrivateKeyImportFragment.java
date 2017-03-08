package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.PrivateKeyImportExportService;
import org.sufficientlysecure.keychain.ui.util.Notify;

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

    private LocalBroadcastManager mBroadcaster;

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

        mBroadcaster = LocalBroadcastManager.getInstance(mActivity);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PrivateKeyImportExportService.IMPORT_ACTION_SHOW_PHRASE);
        filter.addAction(PrivateKeyImportExportService.IMPORT_ACTION_KEY);

        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mReceiver);

        if (mActivity.isFinishing()) {
            Intent intent = new Intent(PrivateKeyImportExportService.ACTION_STOP);
            mBroadcaster.sendBroadcast(intent);
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
                Intent intent = new Intent(mActivity, PrivateKeyImportExportService.class);
                intent.putExtra(PrivateKeyImportExportService.EXTRA_EXPORT_KEY, false);
                intent.putExtra(PrivateKeyImportExportService.EXTRA_IMPORT_IP_ADDRESS, mIpAddress);
                intent.putExtra(PrivateKeyImportExportService.EXTRA_IMPORT_PORT, mPort);
                mActivity.startService(intent);

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
                broadcastImport(false);
                mActivity.finish();
            }
        });

        buttonSentenceMatched.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                broadcastImport(true);
            }
        });


        return view;
    }

    private void broadcastImport(boolean phrasesMatched) {
        Intent intent = new Intent(PrivateKeyImportExportService.IMPORT_ACTION_PHRASES_MATCHED);
        intent.putExtra(PrivateKeyImportExportService.IMPORT_EXTRA, phrasesMatched);

        mBroadcaster.sendBroadcast(intent);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case PrivateKeyImportExportService.IMPORT_ACTION_SHOW_PHRASE:
                    String phrase = intent.getStringExtra(PrivateKeyImportExportService.IMPORT_EXTRA);

                    if (phrase == null) {
                        Notify.create(getActivity(), R.string.private_key_import_error, Notify.Style.ERROR).show();
                        mOkButton.setEnabled(true);
                        return;
                    }

                    mSentenceText.setText(phrase);

                    mOkButton.setVisibility(View.GONE);
                    mIpLayout.setVisibility(View.GONE);
                    mSentenceLayout.setVisibility(View.VISIBLE);
                    mButtonLayout.setVisibility(View.VISIBLE);
                    break;
                case PrivateKeyImportExportService.IMPORT_ACTION_KEY:
                    byte[] keyRing = intent.getByteArrayExtra(PrivateKeyImportExportService.IMPORT_EXTRA);

                    Intent keyIntent = new Intent();
                    keyIntent.putExtra(EXTRA_RECEIVED_KEYRING, keyRing);

                    mActivity.setResult(Activity.RESULT_OK, keyIntent);
                    mActivity.finish();
                    break;
            }
        }
    };
}

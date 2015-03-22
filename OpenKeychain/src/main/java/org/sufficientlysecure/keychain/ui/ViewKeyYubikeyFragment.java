package org.sufficientlysecure.keychain.ui;


import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.PromoteKeyResult;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


public class ViewKeyYubikeyFragment extends Fragment {


    public static final String ARG_FINGERPRINT = "fingerprint";
    public static final String ARG_USER_ID = "user_id";
    public static final String ARG_AID = "aid";
    private byte[] mFingerprints;
    private String mUserId;
    private byte[] mAid;

    public static ViewKeyYubikeyFragment newInstance(byte[] fingerprints, String userId, byte[] aid) {

        ViewKeyYubikeyFragment frag = new ViewKeyYubikeyFragment();

        Bundle args = new Bundle();
        args.putByteArray(ARG_FINGERPRINT, fingerprints);
        args.putString(ARG_USER_ID, userId);
        args.putByteArray(ARG_AID, aid);
        frag.setArguments(args);

        return frag;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mFingerprints = args.getByteArray(ARG_FINGERPRINT);
        mUserId = args.getString(ARG_USER_ID);
        mAid = args.getByteArray(ARG_AID);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_yubikey, null);

        TextView vSerNo = (TextView) view.findViewById(R.id.yubikey_serno);
        TextView vUserId = (TextView) view.findViewById(R.id.yubikey_userid);

        String serno = Hex.toHexString(mAid, 10, 4);
        vSerNo.setText("Serial N° " + serno);

        if (!mUserId.isEmpty()) {
            vUserId.setText("Key holder: " + mUserId);
        } else {
            vUserId.setText("Key holder: " + "<unset>");
        }

        view.findViewById(R.id.button_import).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                promoteToSecretKey();
            }
        });


        return view;
    }

    public void promoteToSecretKey() {

        // Message is received after decrypting is done in KeychainIntentService
        KeychainIntentServiceHandler saveHandler = new KeychainIntentServiceHandler(getActivity()) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == MessageStatus.OKAY.ordinal()) {
                    // get returned data bundle
                    Bundle returnData = message.getData();

                    PromoteKeyResult result =
                            returnData.getParcelable(DecryptVerifyResult.EXTRA_RESULT);

                    result.createNotify(getActivity()).show();
                }

            }
        };

        // Send all information needed to service to decrypt in other thread
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);

        // fill values for this action

        intent.setAction(KeychainIntentService.ACTION_PROMOTE_KEYRING);

        long masterKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(mFingerprints);

        Bundle data = new Bundle();
        data.putLong(KeychainIntentService.PROMOTE_MASTER_KEY_ID, masterKeyId);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // start service with intent
        getActivity().startService(intent);

    }

}

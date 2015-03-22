package org.sufficientlysecure.keychain.ui;


import java.nio.ByteBuffer;
import java.util.Arrays;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.operations.results.PromoteKeyResult;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


public class ViewKeyYubikeyFragment extends Fragment
        implements LoaderCallbacks<Cursor> {

    public static final String ARG_FINGERPRINT = "fingerprint";
    public static final String ARG_USER_ID = "user_id";
    public static final String ARG_CARD_AID = "aid";
    private byte[][] mFingerprints;
    private String mUserId;
    private byte[] mCardAid;
    private long mMasterKeyId;
    private Button vButton;
    private TextView vStatus;

    public static ViewKeyYubikeyFragment newInstance(byte[] fingerprints, String userId, byte[] aid) {

        ViewKeyYubikeyFragment frag = new ViewKeyYubikeyFragment();

        Bundle args = new Bundle();
        args.putByteArray(ARG_FINGERPRINT, fingerprints);
        args.putString(ARG_USER_ID, userId);
        args.putByteArray(ARG_CARD_AID, aid);
        frag.setArguments(args);

        return frag;

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        ByteBuffer buf = ByteBuffer.wrap(args.getByteArray(ARG_FINGERPRINT));
        mFingerprints = new byte[buf.remaining()/40][];
        for (int i = 0; i < mFingerprints.length; i++) {
            mFingerprints[i] = new byte[20];
            buf.get(mFingerprints[i]);
        }
        mUserId = args.getString(ARG_USER_ID);
        mCardAid = args.getByteArray(ARG_CARD_AID);

        mMasterKeyId = KeyFormattingUtils.getKeyIdFromFingerprint(mFingerprints[0]);

        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_yubikey, null);

        TextView vSerNo = (TextView) view.findViewById(R.id.yubikey_serno);
        TextView vUserId = (TextView) view.findViewById(R.id.yubikey_userid);

        String serno = Hex.toHexString(mCardAid, 10, 4);
        vSerNo.setText(getString(R.string.yubikey_serno, serno));

        if (!mUserId.isEmpty()) {
            vUserId.setText(getString(R.string.yubikey_key_holder, mUserId));
        } else {
            vUserId.setText(getString(R.string.yubikey_key_holder_unset));
        }

        vButton = (Button) view.findViewById(R.id.button_bind);
        vButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                promoteToSecretKey();
            }
        });

        vStatus = (TextView) view.findViewById(R.id.yubikey_status);

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

        Bundle data = new Bundle();
        data.putLong(KeychainIntentService.PROMOTE_MASTER_KEY_ID, mMasterKeyId);
        data.putByteArray(KeychainIntentService.PROMOTE_CARD_AID, mCardAid);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(saveHandler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // start service with intent
        getActivity().startService(intent);

    }

    public static final String[] PROJECTION = new String[]{
            Keys._ID,
            Keys.KEY_ID,
            Keys.RANK,
            Keys.HAS_SECRET,
            Keys.FINGERPRINT
    };
    private static final int INDEX_KEY_ID = 1;
    private static final int INDEX_RANK = 2;
    private static final int INDEX_HAS_SECRET = 3;
    private static final int INDEX_FINGERPRINT = 4;

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), Keys.buildKeysUri(mMasterKeyId),
                PROJECTION, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (!data.moveToFirst()) {
            // wut?
            return;
        }

        boolean allBound = true;
        boolean noneBound = true;

        do {
            SecretKeyType keyType = SecretKeyType.fromNum(data.getInt(INDEX_HAS_SECRET));
            byte[] fingerprint = data.getBlob(INDEX_FINGERPRINT);
            Integer index = naiveIndexOf(mFingerprints, fingerprint);
            if (index == null) {
                continue;
            }
            if (keyType == SecretKeyType.DIVERT_TO_CARD) {
                noneBound = false;
            } else {
                allBound = false;
            }
        } while (data.moveToNext());

        if (allBound) {
            vButton.setVisibility(View.GONE);
            vStatus.setText(R.string.yubikey_status_bound);
        } else {
            vButton.setVisibility(View.VISIBLE);
            vStatus.setText(noneBound
                    ? R.string.yubikey_status_unbound
                    : R.string.yubikey_status_partly);
        }

    }

    public Integer naiveIndexOf(byte[][] haystack, byte[] needle) {
        for (int i = 0; i < haystack.length; i++) {
            if (Arrays.equals(needle, haystack[i])) {
                return i;
            }
        }
        return null;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }
}

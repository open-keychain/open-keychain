package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.network.KeyExportSocket;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.ui.widget.ToolableViewAnimator;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;

public class PrivateKeyExportFragment extends CryptoOperationFragment<BackupKeyringParcel, ExportResult> implements KeyExportSocket.ExportKeyListener, FragmentManager.OnBackStackChangedListener {
    public static final String ARG_MASTER_KEY_IDS = "master_key_ids";

    private static final String ARG_CONNECTION_DETAILS = "connection_details";
    private static final String ARG_IP_ADDRESS = "ip_address";
    private static final String ARG_PHRASE = "phrase";
    private static final String ARG_BACK_STACK = "back_stack";
    private static final String ARG_CURRENT_STATE = "current_state";

    private static final String BACK_STACK_INPUT = "state_display";
    private static final int REQUEST_CONNECTION = 23;

    private enum ExportState {
        STATE_UNINITIALIZED, STATE_QR, STATE_INFO, STATE_PHRASE
    }

    private ToolableViewAnimator mTitleAnimator, mContentAnimator, mButtonAnimator;
    private ImageView mQrCode;
    private TextView mPhraseText;
    private TextView mPortText;

    private Activity mActivity;
    private String mIpAddress;
    private String mConnectionDetails;
    private long mMasterKeyId;
    private Uri mCachedUri;
    private String mPhrase;

    private KeyExportSocket mSocket;
    private ExportState mCurrentState = ExportState.STATE_UNINITIALIZED;
    private Integer mBackStackLevel;

    public static PrivateKeyExportFragment newInstance(long masterKeyId) {
        PrivateKeyExportFragment frag = new PrivateKeyExportFragment();

        Bundle args = new Bundle();
        args.putLong(ARG_MASTER_KEY_IDS, masterKeyId);
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        mActivity = (Activity) context;
        mIpAddress = getIPAddress(true);

        mActivity.setTitle(R.string.title_export_private_key);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSocket = KeyExportSocket.getInstance(this);

        if (savedInstanceState != null) {
            mConnectionDetails = savedInstanceState.getString(ARG_CONNECTION_DETAILS);
            mIpAddress = savedInstanceState.getString(ARG_IP_ADDRESS);
            mPhrase = savedInstanceState.getString(ARG_PHRASE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(ARG_CONNECTION_DETAILS, mConnectionDetails);
        outState.putString(ARG_IP_ADDRESS, mIpAddress);
        outState.putString(ARG_PHRASE, mPhrase);

        outState.putInt(ARG_CURRENT_STATE, mCurrentState.ordinal());
        outState.putInt(ARG_BACK_STACK, mBackStackLevel == null ? -1 : mBackStackLevel);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mActivity.isFinishing()) {
            mSocket.close(false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.private_key_export_fragment, c, false);

        Bundle args = getArguments();
        mMasterKeyId = args.getLong(ARG_MASTER_KEY_IDS);

        mTitleAnimator = (ToolableViewAnimator) view.findViewById(R.id.private_key_export_title_animator);
        mContentAnimator = (ToolableViewAnimator) view.findViewById(R.id.private_key_export_animator);
        mButtonAnimator = (ToolableViewAnimator) view.findViewById(R.id.private_key_export_button_bar_animator);

        mQrCode = (ImageView) view.findViewById(R.id.private_key_export_qr_image);
        Button doesntWorkButton = (Button) view.findViewById(R.id.private_key_export_button);
        Button helpButton = (Button) view.findViewById(R.id.private_key_export_help_button);

        TextView ipText = (TextView) view.findViewById(R.id.private_key_export_ip);
        mPortText = (TextView) view.findViewById(R.id.private_key_export_port);
        mPhraseText = (TextView) view.findViewById(R.id.private_key_export_phrase);
        Button noButton = (Button) view.findViewById(R.id.private_key_export_sentence_not_matched_button);
        Button yesButton = (Button) view.findViewById(R.id.private_key_export_sentence_matched_button);

        mQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showQrCodeDialog();
            }
        });

        doesntWorkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.close(true);
                switchState(ExportState.STATE_INFO, true);
            }
        });

        helpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                HelpActivity.startHelpActivity(mActivity, HelpActivity.TAB_FAQ, R.string.help_tab_faq_headline_transfer_key);
            }
        });

        ipText.setText(mIpAddress);

        noButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.phrasesMatched(false);
            }
        });

        yesButton.setOnClickListener(new View.OnClickListener() {
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
            int savedBackStack = savedInstanceState.getInt(ARG_BACK_STACK);
            if (savedBackStack >= 0) {
                mBackStackLevel = savedBackStack;
                // unchecked use, we know that this one is available in onViewCreated
                getFragmentManager().addOnBackStackChangedListener(this);
            }
            ExportState savedState = ExportState.values()[savedInstanceState.getInt(ARG_CURRENT_STATE)];
            switchState(savedState, false);

            mPhraseText.setText(mPhrase);
            if (savedState == ExportState.STATE_QR) {
                loadQrCode(mConnectionDetails);
            }
        } else if (mCurrentState == ExportState.STATE_UNINITIALIZED) {
            switchState(ExportState.STATE_QR, true);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONNECTION) {
            KeyExportSocket.setListener(this);
            if (resultCode == RESULT_OK) {
                loadKey();
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    void switchState(ExportState state, boolean animate) {

        switch (state) {
            case STATE_UNINITIALIZED:
                throw new AssertionError("can't switch to uninitialized state, this is a bug!");

            case STATE_QR:
                mTitleAnimator.setDisplayedChild(0, animate);
                mContentAnimator.setDisplayedChild(0, animate);
                mButtonAnimator.setDisplayedChild(0, false);
                mButtonAnimator.setVisibility(View.VISIBLE);
                break;

            case STATE_INFO:
                mPortText.setText(String.valueOf(mSocket.getPort()));

                mTitleAnimator.setDisplayedChild(1, animate);
                mContentAnimator.setDisplayedChild(1, animate);
                mButtonAnimator.setVisibility(View.GONE);

                pushBackStackEntry();

                break;

            case STATE_PHRASE:
                mTitleAnimator.setDisplayedChild(2, animate);
                mContentAnimator.setDisplayedChild(2, animate);
                mButtonAnimator.setDisplayedChild(2, false);
                mButtonAnimator.setVisibility(View.VISIBLE);

                popBackStackNoAction();

                break;
        }

        mCurrentState = state;
    }

    private void showQrCodeDialog() {
        Intent qrCodeIntent = new Intent(mActivity, QrCodeViewActivity.class);

        // create the transition animation - the images in the layouts
        // of both activities are defined with android:transitionName="qr_code"
        Bundle opts = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ActivityOptions options = ActivityOptions
                    .makeSceneTransitionAnimation(mActivity, mQrCode, "qr_code");
            opts = options.toBundle();
        }

        qrCodeIntent.putExtra(QrCodeViewActivity.EXTRA_QR_CODE_CONTENT, mConnectionDetails);
        qrCodeIntent.putExtra(QrCodeViewActivity.EXTRA_TITLE_RES_ID, R.string.title_export_private_key);
        qrCodeIntent.putExtra(QrCodeViewActivity.EXTRA_EXPORT_PRIVATE_KEY, true);
        qrCodeIntent.putExtra(QrCodeViewActivity.EXTRA_WHITE_TOOLBAR, true);
        startActivityForResult(qrCodeIntent, REQUEST_CONNECTION, opts);
    }

    /**
     * Load QR Code asynchronously and with a fade in animation
     */
    private void loadQrCode(String connectionDetails) {
        mConnectionDetails = connectionDetails;
        mQrCode.setImageBitmap(null);

        AsyncTask<Void, Void, Bitmap> loadTask =
                new AsyncTask<Void, Void, Bitmap>() {
                    protected Bitmap doInBackground(Void... unused) {
                        // render with minimal size
                        return QrCodeUtils.getQRCodeBitmap(mConnectionDetails, 0);
                    }

                    protected void onPostExecute(Bitmap qrCode) {
                        // scale the image up to our actual size. we do this in code rather
                        // than let the ImageView do this because we don't require filtering.
                        Bitmap scaled = Bitmap.createScaledBitmap(qrCode,
                                mQrCode.getHeight(), mQrCode.getHeight(),
                                false);
                        mQrCode.setImageBitmap(scaled);

                        // simple fade-in animation
                        AlphaAnimation anim = new AlphaAnimation(0.0f, 1.0f);
                        anim.setDuration(200);
                        mQrCode.startAnimation(anim);
                    }
                };

        loadTask.execute();
    }

    private void createExport() {
        String date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        String filename = Constants.FILE_ENCRYPTED_BACKUP_PREFIX + date
                + Constants.FILE_EXTENSION_ENCRYPTED_BACKUP_SECRET;

        if (mCachedUri == null) {
            mCachedUri = TemporaryFileProvider.createFile(mActivity, filename,
                    Constants.MIME_TYPE_ENCRYPTED_ALTERNATE);

            cryptoOperation(new CryptoInputParcel());
            return;
        }

        mSocket.writeKey(mActivity, mCachedUri);
    }

    /**
     * from: http://stackoverflow.com/a/13007325
     *
     * Get IP address from first non-localhost interface
     * @param useIPv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    private static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    @Nullable
    @Override
    public BackupKeyringParcel createOperationInput() {
        return new BackupKeyringParcel(new long[] {mMasterKeyId}, true, false, false, mCachedUri);
    }

    @Override
    public void onCryptoOperationSuccess(ExportResult result) {
        createExport();
    }

    @Override
    public void onCryptoOperationError(ExportResult result) {
        result.createNotify(getActivity()).show();
        mCachedUri = null;
    }

    @Override
    public void onCryptoOperationCancelled() {
        mCachedUri = null;
    }

    @Override
    public void showConnectionDetails(String connectionDetails) {
        loadQrCode(connectionDetails);
    }

    @Override
    public void loadKey() {
        createExport();
    }

    @Override
    public void keyExported() {
        mActivity.finish();
    }

    @Override
    public void showPhrase(String phrase) {
        mPhrase = phrase;
        mPhraseText.setText(phrase);

        switchState(ExportState.STATE_PHRASE, true);
    }

    private void pushBackStackEntry() {
        if (mBackStackLevel != null) {
            return;
        }
        FragmentManager fragMan = getFragmentManager();
        mBackStackLevel = fragMan.getBackStackEntryCount();
        fragMan.beginTransaction().addToBackStack(BACK_STACK_INPUT).commit();
        fragMan.addOnBackStackChangedListener(this);
    }

    private void popBackStackNoAction() {
        FragmentManager fragMan = getFragmentManager();
        fragMan.removeOnBackStackChangedListener(this);
        fragMan.popBackStackImmediate(BACK_STACK_INPUT, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        mBackStackLevel = null;
    }

    @Override
    public void onBackStackChanged() {
        FragmentManager fragMan = getFragmentManager();
        if (mBackStackLevel != null && fragMan.getBackStackEntryCount() == mBackStackLevel) {
            fragMan.removeOnBackStackChangedListener(this);
            switchState(ExportState.STATE_QR, true);
            mBackStackLevel = null;

            // restart socket
            mSocket.close(false);
            mSocket = KeyExportSocket.getInstance(this);
        }
    }
}

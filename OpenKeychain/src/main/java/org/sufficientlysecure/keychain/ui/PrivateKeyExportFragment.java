package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.PrivateKeyImportExportService;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PrivateKeyExportFragment extends CryptoOperationFragment<BackupKeyringParcel, ExportResult> {
    public static final String ARG_MASTER_KEY_IDS = "master_key_ids";
    public static final int PORT = 5891;

    private ImageView mQrCode;
    private TextView mSentenceText;
    private TextView mSentenceHeadlineText;
    private Button mNoButton;
    private Button mYesButton;

    private Activity mActivity;
    private String mIpAddress;
    private String mConnectionDetails;
    private long mMasterKeyId;
    private Uri mCachedUri;

    private LocalBroadcastManager mBroadcaster;

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
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mBroadcaster = LocalBroadcastManager.getInstance(mActivity);

        IntentFilter filter = new IntentFilter();
        filter.addAction(PrivateKeyImportExportService.EXPORT_ACTION_SHOW_PHRASE);
        filter.addAction(PrivateKeyImportExportService.EXPORT_ACTION_UPDATE_CONNECTION_DETAILS);
        filter.addAction(PrivateKeyImportExportService.EXPORT_ACTION_KEY);
        filter.addAction(PrivateKeyImportExportService.EXPORT_ACTION_FINISHED);

        LocalBroadcastManager.getInstance(mActivity).registerReceiver(mReceiver, filter);

        Intent intent = new Intent(mActivity, PrivateKeyImportExportService.class);
        intent.putExtra(PrivateKeyImportExportService.EXTRA_EXPORT_KEY, true);
        mActivity.startService(intent);
    }

    @Override
    public void onStop() {
        super.onStop();

        System.out.println("---> Export onStop");

        // stop import/export service
        Intent intent = new Intent(PrivateKeyImportExportService.ACTION_STOP);
        mBroadcaster.sendBroadcast(intent);

        LocalBroadcastManager.getInstance(mActivity).unregisterReceiver(mReceiver);
    }

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle savedInstanceState) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.private_key_export_fragment, c, false);

        Bundle args = getArguments();
        mMasterKeyId = args.getLong(ARG_MASTER_KEY_IDS);

        final View qrLayout = view.findViewById(R.id.private_key_export_qr_layout);
        mQrCode = (ImageView) view.findViewById(R.id.private_key_export_qr_image);
        final Button button = (Button) view.findViewById(R.id.private_key_export_button);

        final View infoLayout = view.findViewById(R.id.private_key_export_info_layout);
        TextView ipText = (TextView) view.findViewById(R.id.private_key_export_ip);
        TextView portText = (TextView) view.findViewById(R.id.private_key_export_port);
        mSentenceHeadlineText = (TextView) view.findViewById(R.id.private_key_export_sentence_headline);
        mSentenceText = (TextView) view.findViewById(R.id.private_key_export_sentence);
        mNoButton = (Button) view.findViewById(R.id.private_key_export_sentence_not_matched_button);
        mYesButton = (Button) view.findViewById(R.id.private_key_export_sentence_matched_button);

        mQrCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showQrCodeDialog();
            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                broadcastExport(PrivateKeyImportExportService.EXPORT_ACTION_MANUAL_MODE, null);

                qrLayout.setVisibility(View.GONE);
                button.setVisibility(View.GONE);

                infoLayout.setVisibility(View.VISIBLE);
            }
        });

        ipText.setText(mIpAddress);
        portText.setText(String.valueOf(PORT));

        mNoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                broadcastExport(PrivateKeyImportExportService.EXPORT_ACTION_PHRASES_MATCHED, false);
            }
        });

        mYesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                broadcastExport(PrivateKeyImportExportService.EXPORT_ACTION_PHRASES_MATCHED, true);
            }
        });

        loadQrCode();

        return view;
    }

    private void broadcastExport(String action, Uri extraUri) {
        Intent intent = new Intent(action);

        if (extraUri != null) {
            intent.putExtra(PrivateKeyImportExportService.EXPORT_EXTRA, extraUri);
        }

        mBroadcaster.sendBroadcast(intent);
    }

    private void broadcastExport(String action, boolean extraBoolean) {
        Intent intent = new Intent(action);
        intent.putExtra(PrivateKeyImportExportService.EXPORT_EXTRA, extraBoolean);

        mBroadcaster.sendBroadcast(intent);
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
        ActivityCompat.startActivity(mActivity, qrCodeIntent, opts);
    }

    /**
     * Load QR Code asynchronously and with a fade in animation
     */
    private void loadQrCode() {
        if (mQrCode == null || mConnectionDetails == null) {
            return;
        }

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

        broadcastExport(PrivateKeyImportExportService.EXPORT_ACTION_CACHED_URI, mCachedUri);
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
        return new BackupKeyringParcel(new long[] {mMasterKeyId}, true, false, mCachedUri);
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


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            switch (action) {
                case PrivateKeyImportExportService.EXPORT_ACTION_UPDATE_CONNECTION_DETAILS:
                    mConnectionDetails = intent.getStringExtra(PrivateKeyImportExportService.EXPORT_EXTRA);
                    loadQrCode();
                    break;
                case PrivateKeyImportExportService.EXPORT_ACTION_SHOW_PHRASE:
                    String phrase = intent.getStringExtra(PrivateKeyImportExportService.EXPORT_EXTRA);

                    mSentenceHeadlineText.setVisibility(View.VISIBLE);
                    mSentenceText.setVisibility(View.VISIBLE);
                    mNoButton.setVisibility(View.VISIBLE);
                    mYesButton.setVisibility(View.VISIBLE);

                    mSentenceText.setText(phrase);
                    break;
                case PrivateKeyImportExportService.EXPORT_ACTION_KEY:
                    createExport();
                    break;
                case PrivateKeyImportExportService.EXPORT_ACTION_FINISHED:
                    mActivity.finish();
                    break;
            }
        }
    };
}

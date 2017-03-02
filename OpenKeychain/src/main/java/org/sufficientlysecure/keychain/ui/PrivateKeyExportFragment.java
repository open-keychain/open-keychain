package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cryptolib.SecureDataSocket;
import com.cryptolib.UnverifiedException;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ExportResult;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.service.BackupKeyringParcel;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import org.sufficientlysecure.keychain.util.FileHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.content.Context.WIFI_SERVICE;

public class PrivateKeyExportFragment extends CryptoOperationFragment<BackupKeyringParcel, ExportResult> {
    public static final String ARG_MASTER_KEY_IDS = "master_key_ids";

    private static final int PORT = 5891;

    private ImageView mQrCode;

    private Activity mActivity;
    private SecureDataSocket mSecureDataSocket;
    private String mIpAddress;
    private String mConnectionDetails;
    private long mMasterKeyId;
    private Uri mCachedBackupUri;

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
    public void onResume() {
        super.onResume();

        mInitSocketTask.execute();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mInitSocketTask != null) {
            mInitSocketTask.cancel(true);
        }
        if (mSecureDataSocket != null) {
            mSecureDataSocket.close();
        }
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
        TextView sentenceText = (TextView) view.findViewById(R.id.private_key_export_sentence);
        final Button okButton = (Button) view.findViewById(R.id.private_key_export_ok_button);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                qrLayout.setVisibility(View.GONE);
                button.setVisibility(View.GONE);

                infoLayout.setVisibility(View.VISIBLE);
                okButton.setVisibility(View.VISIBLE);
            }
        });

        ipText.setText(mIpAddress);
        portText.setText(String.valueOf(PORT));
        sentenceText.setText("This is a sentence.");

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.finish();
            }
        });

        loadQrCode();

        return view;
    }

    private AsyncTask<Void, Void, String> mInitSocketTask = new AsyncTask<Void, Void, String>() {
        protected String doInBackground(Void... unused) {
            String connectionDetails = null;

            try {
                mSecureDataSocket = new SecureDataSocket(PORT);
                connectionDetails = mSecureDataSocket.prepareServerWithClientCamera();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return connectionDetails;
        }

        protected void onPostExecute(String connectionDetails) {
            if (isCancelled()  ||isRemoving()) {
                return;
            }

            mConnectionDetails = connectionDetails;
            loadQrCode();
            new Thread(mSecureConnection).start();
        }
    };

    private Runnable mSecureConnection = new Runnable() {
        @Override
        public void run() {
            try {
                mSecureDataSocket.setupServerWithClientCamera();
                createExport();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };


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
                        Uri uri = new Uri.Builder()
                                .path(mConnectionDetails)
                                .build();
                        // render with minimal size
                        return QrCodeUtils.getQRCodeBitmap(uri, 0);
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

        if (mCachedBackupUri == null) {
            mCachedBackupUri = TemporaryFileProvider.createFile(mActivity, filename,
                    Constants.MIME_TYPE_ENCRYPTED_ALTERNATE);

            cryptoOperation(new CryptoInputParcel());
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] exportData = FileHelper.readBytesFromUri(mActivity, mCachedBackupUri);
                    mSecureDataSocket.write(exportData);
                } catch (IOException | UnverifiedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
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
        return new BackupKeyringParcel(new long[] {mMasterKeyId}, true, false, mCachedBackupUri);
    }

    @Override
    public void onCryptoOperationSuccess(ExportResult result) {

    }

    @Override
    public void onCryptoOperationError(ExportResult result) {
        result.createNotify(getActivity()).show();
        mCachedBackupUri = null;
    }

    @Override
    public void onCryptoOperationCancelled() {
        mCachedBackupUri = null;
    }
}

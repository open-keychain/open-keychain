package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;

public class PrivateKeyExportFragment extends Fragment {
    private ImageView mQrCode;

    private Activity mActivity;

    public static PrivateKeyExportFragment newInstance() {
        PrivateKeyExportFragment frag = new PrivateKeyExportFragment();

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
        View view = LayoutInflater.from(getContext()).inflate(R.layout.private_key_export_fragment, c, false);

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

        ipText.setText("192.168.0.56");
        portText.setText("4865");
        sentenceText.setText("This is a sentence.");

        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mActivity.finish();
            }
        });

        loadQrCode("Test 123");

        return view;
    }

    /**
     * Load QR Code asynchronously and with a fade in animation
     */
    private void loadQrCode(final String path) {
        AsyncTask<Void, Void, Bitmap> loadTask =
                new AsyncTask<Void, Void, Bitmap>() {
                    protected Bitmap doInBackground(Void... unused) {
                        Uri uri = new Uri.Builder()
                                .path(path)
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
}

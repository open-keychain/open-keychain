/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.ui.transfer.view;


import android.graphics.Bitmap;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.transfer.presenter.TransferPresenter;
import org.sufficientlysecure.keychain.ui.transfer.presenter.TransferPresenter.TransferMvpView;


@RequiresApi(api = VERSION_CODES.LOLLIPOP)
public class TransferFragment extends Fragment implements TransferMvpView {
    public static final int VIEW_WAITING = 0;
    public static final int VIEW_CONNECTED = 1;


    private ImageView vQrCodeImage;
    private View vScanButton;
    private TransferPresenter presenter;
    private ViewAnimator vTransferAnimator;
    private TextView vConnectionStatusText;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.transfer_fragment, container, false);

        vTransferAnimator = (ViewAnimator) view.findViewById(R.id.transfer_animator);

        vConnectionStatusText = (TextView) view.findViewById(R.id.connection_status);

        vQrCodeImage = (ImageView) view.findViewById(R.id.qr_code_image);
        vScanButton = view.findViewById(R.id.button_scan);
        vScanButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (presenter != null) {
                    presenter.onClickScan();
                }
            }
        });

        return view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        presenter = new TransferPresenter(getContext(), this);
    }

    @Override
    public void onStart() {
        super.onStart();

        presenter.startServer();
    }


    @Override
    public void onStop() {
        super.onStop();

        presenter.onDestroy();
    }

    @Override
    public void showWaitingForConnection() {
        vTransferAnimator.setDisplayedChild(VIEW_WAITING);
    }

    @Override
    public void showConnectionEstablished(String hostname) {
        vTransferAnimator.setDisplayedChild(VIEW_CONNECTED);
        vConnectionStatusText.setText("Connected to: " + hostname);
    }

    @Override
    public void setQrImage(final Bitmap qrCode) {
        vQrCodeImage.getViewTreeObserver().addOnGlobalLayoutListener(
                new OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        // create actual bitmap in display dimensions
                        Bitmap scaled = Bitmap.createScaledBitmap(qrCode,
                                vQrCodeImage.getWidth(), vQrCodeImage.getWidth(), false);
                        vQrCodeImage.setImageBitmap(scaled);
                    }
                });
        vQrCodeImage.requestLayout();
    }
}

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


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentManager.OnBackStackChangedListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.google.zxing.client.android.Intents;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.ui.QrCodeCaptureActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper.Callback;
import org.sufficientlysecure.keychain.ui.transfer.presenter.TransferPresenter;
import org.sufficientlysecure.keychain.ui.transfer.presenter.TransferPresenter.TransferMvpView;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;


@RequiresApi(api = VERSION_CODES.LOLLIPOP)
public class TransferFragment extends Fragment implements TransferMvpView {
    public static final int VIEW_WAITING = 0;
    public static final int VIEW_CONNECTED = 1;
    public static final int VIEW_RECEIVING = 2;
    public static final int VIEW_NO_WIFI = 3;

    public static final int REQUEST_CODE_SCAN = 1;
    public static final int LOADER_ID = 1;


    private ImageView vQrCodeImage;
    private TransferPresenter presenter;
    private ViewAnimator vTransferAnimator;
    private TextView vConnectionStatusText1;
    private TextView vConnectionStatusText2;
    private RecyclerView vTransferKeyList;
    private RecyclerView vReceivedKeyList;

    private CryptoOperationHelper currentCryptoOperationHelper;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (WifiManager.NETWORK_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                if (networkInfo != null && networkInfo.isConnected()) {
                    presenter.onWifiConnected();
                }
            }
        }
    };


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.transfer_fragment, container, false);

        vTransferAnimator = (ViewAnimator) view.findViewById(R.id.transfer_animator);

        vConnectionStatusText1 = (TextView) view.findViewById(R.id.connection_status_1);
        vConnectionStatusText2 = (TextView) view.findViewById(R.id.connection_status_2);
        vTransferKeyList = (RecyclerView) view.findViewById(R.id.transfer_key_list);
        vReceivedKeyList = (RecyclerView) view.findViewById(R.id.received_key_list);

        vQrCodeImage = (ImageView) view.findViewById(R.id.qr_code_image);

        view.findViewById(R.id.button_scan).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (presenter != null) {
                    presenter.onUiClickScan();
                }
            }
        });

        presenter = new TransferPresenter(getContext(), getLoaderManager(), LOADER_ID, this);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        presenter.onUiStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        getContext().registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();

        getContext().unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();

        presenter.onUiStop();
    }

    @Override
    public void showNotOnWifi() {
        vTransferAnimator.setDisplayedChild(VIEW_NO_WIFI);
    }

    @Override
    public void showWaitingForConnection() {
        vTransferAnimator.setDisplayedChild(VIEW_WAITING);
    }

    @Override
    public void showConnectionEstablished(String hostname) {
        vConnectionStatusText1.setText("Connected to: " + hostname);
        vConnectionStatusText2.setText("Connected to: " + hostname);
        vTransferAnimator.setDisplayedChild(VIEW_CONNECTED);
    }

    @Override
    public void showReceivingKeys() {
        vTransferAnimator.setDisplayedChild(VIEW_RECEIVING);
    }

    @Override
    public void showViewDisconnected() {
        vConnectionStatusText1.setText("Disconnected!");
        vConnectionStatusText2.setText("Disconnected!");
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

    @Override
    public void scanQrCode() {
        Intent intent = new Intent(getActivity(), QrCodeCaptureActivity.class);
        startActivityForResult(intent, REQUEST_CODE_SCAN);
    }

    @Override
    public void setSecretKeyAdapter(Adapter adapter) {
        vTransferKeyList.setAdapter(adapter);
    }

    @Override
    public void setReceivedKeyAdapter(Adapter adapter) {
        vReceivedKeyList.setAdapter(adapter);
    }

    @Override
    public <T extends Parcelable, S extends OperationResult> CryptoOperationHelper<T,S> createCryptoOperationHelper(Callback<T, S> callback) {
        CryptoOperationHelper<T,S> cryptoOperationHelper = new CryptoOperationHelper<>(1, this, callback, null);
        currentCryptoOperationHelper = cryptoOperationHelper;
        return cryptoOperationHelper;
    }

    @Override
    public void releaseCryptoOperationHelper() {
        currentCryptoOperationHelper = null;
    }

    @Override
    public void showErrorBadKey() {
        Notify.create(getActivity(), "Failed reading incoming key!", Style.ERROR).show();
    }

    @Override
    public void showErrorConnectionFailed() {
        Notify.create(getActivity(), "Connection failed!", Style.ERROR).show();
    }

    @Override
    public void showErrorListenFailed() {
        Notify.create(getActivity(), "Error setting up server!", Style.ERROR).show();
    }

    @Override
    public void showResultNotification(ImportKeyResult result) {
        result.createNotify(getActivity()).show();
    }

    @Override
    public void addFakeBackStackItem(final String tag) {
        FragmentManager fragmentManager = getFragmentManager();

        fragmentManager.beginTransaction()
                .addToBackStack(tag)
                .commitAllowingStateLoss();
        fragmentManager.executePendingTransactions();

        fragmentManager.addOnBackStackChangedListener(new OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                FragmentManager fragMan = getFragmentManager();
                fragMan.popBackStack(tag, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                fragMan.removeOnBackStackChangedListener(this);

                presenter.onUiBackStackPop();
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (currentCryptoOperationHelper != null &&
                currentCryptoOperationHelper.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }

        switch (requestCode) {
            case REQUEST_CODE_SCAN:
                if (resultCode == Activity.RESULT_OK) {
                    String qrCodeData = data.getStringExtra(Intents.Scan.RESULT);
                    presenter.onUiQrCodeScanned(qrCodeData);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }
}

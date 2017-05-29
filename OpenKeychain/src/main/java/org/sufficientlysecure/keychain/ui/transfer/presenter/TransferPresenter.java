/*
 * Copyright (C) 2017 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.ui.transfer.presenter;


import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.support.annotation.RequiresApi;

import org.sufficientlysecure.keychain.network.KeyTransferClientInteractor;
import org.sufficientlysecure.keychain.network.KeyTransferClientInteractor.KeyTransferClientCallback;
import org.sufficientlysecure.keychain.network.KeyTransferServerInteractor;
import org.sufficientlysecure.keychain.network.KeyTransferServerInteractor.KeyTransferServerCallback;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;


@RequiresApi(api = VERSION_CODES.LOLLIPOP)
public class TransferPresenter implements KeyTransferServerCallback, KeyTransferClientCallback {
    private final Context context;
    private final TransferMvpView view;

    private KeyTransferServerInteractor keyTransferServerInteractor;
    private KeyTransferClientInteractor keyTransferClientInteractor;

    public TransferPresenter(Context context, TransferMvpView view) {
        this.context = context;
        this.view = view;
    }

    public void onDestroy() {
        clearConnections();
    }

    public void onClickScan() {
        clearConnections();

        view.scanQrCode();
    }

    public void onQrCodeScanned(String qrCodeContent) {
        keyTransferClientInteractor = new KeyTransferClientInteractor();
        keyTransferClientInteractor.connectToServer(qrCodeContent, this);
    }

    private void clearConnections() {
        if (keyTransferServerInteractor != null) {
            keyTransferServerInteractor.stopServer();
            keyTransferServerInteractor = null;
        }
        if (keyTransferClientInteractor != null) {
            keyTransferClientInteractor.closeConnection();
            keyTransferClientInteractor = null;
        }
    }

    public void startServer() {
        keyTransferServerInteractor = new KeyTransferServerInteractor();
        keyTransferServerInteractor.startServer(this);
    }

    @Override
    public void onServerStarted(String qrCodeData) {
        Bitmap qrCodeBitmap = QrCodeUtils.getQRCodeBitmap(Uri.parse("pgp+transfer://" + qrCodeData));
        view.setQrImage(qrCodeBitmap);
    }

    @Override
    public void onConnectionEstablished(String otherName) {
        view.showConnectionEstablished(otherName);
    }

    @Override
    public void onConnectionLost() {
        view.showWaitingForConnection();
        startServer();
    }

    public interface TransferMvpView {
        void showConnectionEstablished(String hostname);
        void showWaitingForConnection();

        void setQrImage(Bitmap qrCode);

        void scanQrCode();
    }
}

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


import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;

import org.sufficientlysecure.keychain.network.KeyTransferInteractor;
import org.sufficientlysecure.keychain.network.KeyTransferInteractor.KeyTransferCallback;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.ui.transfer.loader.SecretKeyLoader.SecretKeyItem;
import org.sufficientlysecure.keychain.ui.transfer.view.TransferSecretKeyList.OnClickTransferKeyListener;
import org.sufficientlysecure.keychain.ui.transfer.view.TransferSecretKeyList.TransferKeyAdapter;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;


@RequiresApi(api = VERSION_CODES.LOLLIPOP)
public class TransferPresenter implements KeyTransferCallback, LoaderCallbacks<List<SecretKeyItem>>,
        OnClickTransferKeyListener {
    private final Context context;
    private final TransferMvpView view;
    private final LoaderManager loaderManager;
    private final int loaderId;

    private KeyTransferInteractor keyTransferClientInteractor;
    private KeyTransferInteractor keyTransferServerInteractor;
    private final TransferKeyAdapter secretKeyAdapter;

    public TransferPresenter(Context context, LoaderManager loaderManager, int loaderId, TransferMvpView view) {
        this.context = context;
        this.view = view;
        this.loaderManager = loaderManager;
        this.loaderId = loaderId;

        secretKeyAdapter = new TransferKeyAdapter(context, LayoutInflater.from(context), this);
        view.setSecretKeyAdapter(secretKeyAdapter);
    }

    public void onStart() {
        loaderManager.restartLoader(loaderId, null, this);

        startServer();
    }

    public void onStop() {
        clearConnections();
    }

    public void onClickScan() {
        clearConnections();

        view.scanQrCode();
    }

    public void onQrCodeScanned(String qrCodeContent) {
        keyTransferClientInteractor = new KeyTransferInteractor();
        keyTransferClientInteractor.connectToServer(qrCodeContent, this);
    }

    private void clearConnections() {
        if (keyTransferServerInteractor != null) {
            keyTransferServerInteractor.closeConnection();
            keyTransferServerInteractor = null;
        }
        if (keyTransferClientInteractor != null) {
            keyTransferClientInteractor.closeConnection();
            keyTransferClientInteractor = null;
        }
    }

    public void startServer() {
        keyTransferServerInteractor = new KeyTransferInteractor();
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

    @Override
    public Loader<List<SecretKeyItem>> onCreateLoader(int id, Bundle args) {
        return secretKeyAdapter.createLoader(context);
    }

    @Override
    public void onLoadFinished(Loader<List<SecretKeyItem>> loader, List<SecretKeyItem> data) {
        secretKeyAdapter.setData(data);
    }

    @Override
    public void onLoaderReset(Loader<List<SecretKeyItem>> loader) {
        secretKeyAdapter.setData(null);
    }

    @Override
    public void onClickTransferKey(long masterKeyId) {
        try {
            byte[] armoredSecretKey =
                    KeyRepository.createDatabaseInteractor(context).getSecretKeyRingAsArmoredData(masterKeyId);
            if (keyTransferClientInteractor != null) {
                keyTransferClientInteractor.sendData(armoredSecretKey);
            } else if (keyTransferServerInteractor != null) {
                keyTransferServerInteractor.sendData(armoredSecretKey);
            }
        } catch (IOException | NotFoundException | PgpGeneralException e) {
            e.printStackTrace();
        }
    }

    public interface TransferMvpView {
        void showWaitingForConnection();
        void showConnectionEstablished(String hostname);

        void scanQrCode();
        void setQrImage(Bitmap qrCode);

        void setSecretKeyAdapter(Adapter adapter);
    }
}

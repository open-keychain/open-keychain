/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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
import java.net.URISyntaxException;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.LayoutInflater;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.network.KeyTransferInteractor;
import org.sufficientlysecure.keychain.network.KeyTransferInteractor.KeyTransferCallback;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.KeyRepository;
import org.sufficientlysecure.keychain.provider.KeyRepository.NotFoundException;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper.Callback;
import org.sufficientlysecure.keychain.ui.transfer.loader.SecretKeyLoader.SecretKeyItem;
import org.sufficientlysecure.keychain.ui.transfer.view.ReceivedSecretKeyList.OnClickImportKeyListener;
import org.sufficientlysecure.keychain.ui.transfer.view.ReceivedSecretKeyList.ReceivedKeyAdapter;
import org.sufficientlysecure.keychain.ui.transfer.view.ReceivedSecretKeyList.ReceivedKeyItem;
import org.sufficientlysecure.keychain.ui.transfer.view.TransferSecretKeyList.OnClickTransferKeyListener;
import org.sufficientlysecure.keychain.ui.transfer.view.TransferSecretKeyList.TransferKeyAdapter;
import org.sufficientlysecure.keychain.ui.util.QrCodeUtils;
import timber.log.Timber;


@RequiresApi(api = VERSION_CODES.LOLLIPOP)
public class TransferPresenter implements KeyTransferCallback, LoaderCallbacks<List<SecretKeyItem>>,
        OnClickTransferKeyListener, OnClickImportKeyListener {
    private static final String DELIMITER_START = "-----BEGIN PGP PRIVATE KEY BLOCK-----";
    private static final String DELIMITER_END = "-----END PGP PRIVATE KEY BLOCK-----";
    private static final String BACKSTACK_TAG_TRANSFER = "transfer";

    private final Context context;
    private final TransferMvpView view;
    private final LoaderManager loaderManager;
    private final int loaderId;
    private final KeyRepository databaseInteractor;

    private final TransferKeyAdapter secretKeyAdapter;
    private final ReceivedKeyAdapter receivedKeyAdapter;


    private KeyTransferInteractor keyTransferClientInteractor;
    private KeyTransferInteractor keyTransferServerInteractor;

    private boolean wasConnected = false;
    private boolean sentData = false;
    private boolean waitingForWifi = false;
    private Long confirmingMasterKeyId;


    public TransferPresenter(Context context, LoaderManager loaderManager, int loaderId, TransferMvpView view) {
        this.context = context;
        this.view = view;
        this.loaderManager = loaderManager;
        this.loaderId = loaderId;
        this.databaseInteractor = KeyRepository.create(context);

        secretKeyAdapter = new TransferKeyAdapter(context, LayoutInflater.from(context), this);
        view.setSecretKeyAdapter(secretKeyAdapter);

        receivedKeyAdapter = new ReceivedKeyAdapter(context, LayoutInflater.from(context), this);
        view.setReceivedKeyAdapter(receivedKeyAdapter);
    }


    public void onUiInitFromIntentUri(final Uri initUri) {
        connectionStartConnect(initUri.toString());
    }

    public void onUiStart() {
        loaderManager.restartLoader(loaderId, null, this);

        if (keyTransferServerInteractor == null && keyTransferClientInteractor == null && !wasConnected) {
            checkWifiResetAndStartListen();
        }
    }

    public void onUiStop() {
        connectionClear();

        if (wasConnected) {
            view.showViewDisconnected();
            view.dismissConfirmationIfExists();
            secretKeyAdapter.setAllDisabled(true);
        }
    }

    public void onUiClickScan() {
        connectionClear();

        view.scanQrCode();
    }

    public void onUiClickScanAgain() {
        onUiClickScan();
    }

    public void onUiClickDone() {
        view.finishFragmentOrActivity();
    }

    public void onUiQrCodeScanned(String qrCodeContent) {
        connectionStartConnect(qrCodeContent);
    }

    public void onUiBackStackPop() {
        if (wasConnected) {
            checkWifiResetAndStartListen();
        }
    }

    @Override
    public void onUiClickTransferKey(long masterKeyId) {
        if (sentData) {
            prepareAndSendKey(masterKeyId);
        } else {
            confirmingMasterKeyId = masterKeyId;
            view.showConfirmSendDialog();
        }
    }

    public void onUiClickConfirmSend() {
        if (confirmingMasterKeyId == null) {
            return;
        }
        long masterKeyId = confirmingMasterKeyId;
        confirmingMasterKeyId = null;

        prepareAndSendKey(masterKeyId);
    }

    @Override
    public void onUiClickImportKey(final long masterKeyId, String keyData) {
        receivedKeyAdapter.focusItem(masterKeyId);

        final ImportKeyringParcel importKeyringParcel = ImportKeyringParcel.createImportKeyringParcel(
                ParcelableKeyRing.createFromEncodedBytes(keyData.getBytes()));

        CryptoOperationHelper<ImportKeyringParcel,ImportKeyResult> op =
                view.createCryptoOperationHelper(new Callback<ImportKeyringParcel,ImportKeyResult>() {
                    @Override
                    public ImportKeyringParcel createOperationInput() {
                        return importKeyringParcel;
                    }

                    @Override
                    public void onCryptoOperationSuccess(ImportKeyResult result) {
                        receivedKeyAdapter.focusItem(null);
                        receivedKeyAdapter.addToFinishedItems(masterKeyId);
                        view.releaseCryptoOperationHelper();
                        view.showResultNotification(result);
                    }

                    @Override
                    public void onCryptoOperationCancelled() {
                        view.releaseCryptoOperationHelper();
                        receivedKeyAdapter.focusItem(null);
                    }

                    @Override
                    public void onCryptoOperationError(ImportKeyResult result) {
                        receivedKeyAdapter.focusItem(null);
                        view.releaseCryptoOperationHelper();
                        view.showResultNotification(result);
                    }

                    @Override
                    public boolean onCryptoSetProgress(String msg, int progress, int max) {
                        return false;
                    }
                });

        op.cryptoOperation();
    }

    public void onWifiConnected() {
        if (waitingForWifi) {
            resetAndStartListen();
        }
    }

    @Override
    public void onServerStarted(String qrCodeData) {
        Bitmap qrCodeBitmap = QrCodeUtils.getQRCodeBitmap(Uri.parse(qrCodeData));
        view.setQrImage(qrCodeBitmap);
    }

    @Override
    public void onConnectionEstablished(String otherName) {
        wasConnected = true;

        secretKeyAdapter.clearFinishedItems();
        secretKeyAdapter.focusItem(null);
        secretKeyAdapter.setAllDisabled(false);
        receivedKeyAdapter.clear();

        view.showConnectionEstablished(otherName);
        view.setShowDoneIcon(true);
        view.addFakeBackStackItem(BACKSTACK_TAG_TRANSFER);
    }

    @Override
    public void onConnectionLost() {
        if (!wasConnected) {
            checkWifiResetAndStartListen();

            view.showErrorConnectionFailed();
        } else {
            connectionClear();

            view.dismissConfirmationIfExists();
            view.showViewDisconnected();
            secretKeyAdapter.setAllDisabled(true);
        }
    }

    @Override
    public void onDataReceivedOk(String receivedData) {
        if (sentData) {
            Timber.d("received data, but we already sent a key! race condition, or other side misbehaving?");
            return;
        }

        Timber.d("received data");
        UncachedKeyRing uncachedKeyRing;
        try {
            uncachedKeyRing = UncachedKeyRing.decodeFromData(receivedData.getBytes());
        } catch (PgpGeneralException | IOException | RuntimeException e) {
            Timber.e(e, "error parsing incoming key");
            view.showErrorBadKey();
            return;
        }

        String primaryUserId = uncachedKeyRing.getPublicKey().getPrimaryUserIdWithFallback();
        UserId userId = OpenPgpUtils.splitUserId(primaryUserId);

        ReceivedKeyItem receivedKeyItem = new ReceivedKeyItem(receivedData, uncachedKeyRing.getMasterKeyId(),
                uncachedKeyRing.getCreationTime(), userId.name, userId.email);
        receivedKeyAdapter.addItem(receivedKeyItem);

        view.showReceivingKeys();
    }

    @Override
    public void onDataSentOk(String passthrough) {
        Timber.d("data sent ok!");
        final long masterKeyId = Long.parseLong(passthrough);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                secretKeyAdapter.focusItem(null);
                secretKeyAdapter.addToFinishedItems(masterKeyId);
            }
        }, 750);
    }

    @Override
    public void onConnectionErrorConnect() {
        view.showWaitingForConnection();
        view.showErrorConnectionFailed();

        resetAndStartListen();
    }

    @Override
    public void onConnectionErrorNoRouteToHost(String wifiSsid) {
        connectionClear();

        String ownWifiSsid = getConnectedWifiSsid();
        if (!wifiSsid.equalsIgnoreCase(ownWifiSsid)) {
            view.showWifiError(wifiSsid);
        } else {
            view.showWaitingForConnection();
            view.showErrorConnectionFailed();

            resetAndStartListen();
        }
    }

    @Override
    public void onConnectionErrorListen() {
        view.showErrorListenFailed();
    }

    @Override
    public void onConnectionError(String errorMessage) {
        view.showErrorConnectionError(errorMessage);

        connectionClear();
        if (wasConnected) {
            view.showViewDisconnected();
            secretKeyAdapter.setAllDisabled(true);
        }
    }

    private void connectionStartConnect(String qrCodeContent) {
        connectionClear();

        view.showEstablishingConnection();

        keyTransferClientInteractor = new KeyTransferInteractor(DELIMITER_START, DELIMITER_END);
        try {
            keyTransferClientInteractor.connectToServer(qrCodeContent, TransferPresenter.this);
        } catch (URISyntaxException e) {
            view.showErrorConnectionFailed();
        }
    }

    private void checkWifiResetAndStartListen() {
        if (!isWifiConnected()) {
            waitingForWifi = true;
            view.showNotOnWifi();
            return;
        }

        resetAndStartListen();
    }

    private void resetAndStartListen() {
        waitingForWifi = false;
        wasConnected = false;
        sentData = false;
        connectionClear();

        String wifiSsid = getConnectedWifiSsid();

        keyTransferServerInteractor = new KeyTransferInteractor(DELIMITER_START, DELIMITER_END);
        keyTransferServerInteractor.startServer(this, wifiSsid);

        view.showWaitingForConnection();
        view.setShowDoneIcon(false);
    }

    private boolean isWifiConnected() {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetwork = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        return wifiNetwork.isConnected();
    }

    private String getConnectedWifiSsid() {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null) {
            return null;
        }
        WifiInfo info = wifiManager.getConnectionInfo();
        if (info == null) {
            return null;
        }
        // getSSID will return the ssid in quotes if it is valid utf-8. we only return it in that case.
        String ssid = info.getSSID();
        if (ssid == null || ssid.isEmpty() || ssid.charAt(0) != '"') {
            return null;
        }
        return ssid.substring(1, ssid.length() -1);
    }

    private void connectionClear() {
        if (keyTransferServerInteractor != null) {
            keyTransferServerInteractor.closeConnection();
            keyTransferServerInteractor = null;
        }
        if (keyTransferClientInteractor != null) {
            keyTransferClientInteractor.closeConnection();
            keyTransferClientInteractor = null;
        }
    }

    private void prepareAndSendKey(long masterKeyId) {
        try {
            byte[] armoredSecretKey = databaseInteractor.getSecretKeyRingAsArmoredData(masterKeyId);
            secretKeyAdapter.focusItem(masterKeyId);
            connectionSend(armoredSecretKey, Long.toString(masterKeyId));
        } catch (IOException | NotFoundException | PgpGeneralException e) {
            // TODO
            e.printStackTrace();
        }
    }

    private void connectionSend(byte[] armoredSecretKey, String passthrough) {
        sentData = true;
        if (keyTransferClientInteractor != null) {
            keyTransferClientInteractor.sendData(armoredSecretKey, passthrough);
        } else if (keyTransferServerInteractor != null) {
            keyTransferServerInteractor.sendData(armoredSecretKey, passthrough);
        }
    }


    @Override
    public Loader<List<SecretKeyItem>> onCreateLoader(int id, Bundle args) {
        return secretKeyAdapter.createLoader(context);
    }

    @Override
    public void onLoadFinished(Loader<List<SecretKeyItem>> loader, List<SecretKeyItem> data) {
        secretKeyAdapter.setData(data);
        view.setShowSecretKeyEmptyView(data.isEmpty());
    }

    @Override
    public void onLoaderReset(Loader<List<SecretKeyItem>> loader) {
        secretKeyAdapter.setData(null);
    }


    public interface TransferMvpView {
        void showNotOnWifi();
        void showWaitingForConnection();
        void showEstablishingConnection();
        void showConnectionEstablished(String hostname);

        void showWifiError(String wifiSsid);

        void showReceivingKeys();

        void showViewDisconnected();

        void scanQrCode();
        void setQrImage(Bitmap qrCode);

        void releaseCryptoOperationHelper();

        void showErrorBadKey();
        void showErrorConnectionFailed();
        void showErrorListenFailed();
        void showErrorConnectionError(String errorMessage);
        void showResultNotification(ImportKeyResult result);

        void setShowDoneIcon(boolean showDoneIcon);

        void setSecretKeyAdapter(Adapter adapter);
        void setShowSecretKeyEmptyView(boolean isEmpty);
        void setReceivedKeyAdapter(Adapter secretKeyAdapter);

        <T extends Parcelable, S extends OperationResult> CryptoOperationHelper<T,S> createCryptoOperationHelper(Callback<T, S> callback);

        void addFakeBackStackItem(String tag);

        void finishFragmentOrActivity();

        void showConfirmSendDialog();
        void dismissConfirmationIfExists();
    }
}

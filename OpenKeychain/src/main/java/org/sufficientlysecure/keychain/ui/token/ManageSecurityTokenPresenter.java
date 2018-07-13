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

package org.sufficientlysecure.keychain.ui.token;


import android.arch.lifecycle.LifecycleOwner;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;

import org.sufficientlysecure.keychain.operations.results.GenericOperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TokenType;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenContract.ManageSecurityTokenMvpPresenter;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenContract.ManageSecurityTokenMvpView;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenFragment.StatusLine;
import org.sufficientlysecure.keychain.ui.token.PublicKeyRetriever.KeyRetrievalResult;
import org.sufficientlysecure.keychain.ui.util.PermissionsUtil;


class ManageSecurityTokenPresenter implements ManageSecurityTokenMvpPresenter {
    private final Context context;

    private final ManageSecurityTokenViewModel viewModel;
    private final LifecycleOwner lifecycleOwner;


    private ManageSecurityTokenMvpView view;

    private boolean checkedKeyStatus;
    private boolean searchedLocally;
    private boolean searchedAtUri;
    private boolean searchedKeyservers;

    private byte[] importKeyData;
    private Long masterKeyId;

    private OperationLog log;
    private Uri selectedContentUri;

    ManageSecurityTokenPresenter(Context context, LifecycleOwner lifecycleOwner, ManageSecurityTokenViewModel viewModel) {
        this.context = context.getApplicationContext();
        this.lifecycleOwner = lifecycleOwner;
        this.viewModel = viewModel;

        this.log = new OperationLog();
        viewModel.resetLiveData(lifecycleOwner);
    }

    @Override
    public void setView(ManageSecurityTokenMvpView view) {
        this.view = view;
    }

    @Override
    public void detach() {
        this.view = null;

        viewModel.resetLiveData(lifecycleOwner);
    }

    @Override
    public void onActivityCreated() {
        if (!checkedKeyStatus || !searchedLocally || !searchedAtUri || !searchedKeyservers) {
            continueSearch();
        }
    }

    private void continueSearchAfterError() {
        view.statusLineError();
        continueSearch();
    }

    private void resetAndContinueSearch() {
        checkedKeyStatus = false;
        searchedLocally = false;
        searchedAtUri = false;
        searchedKeyservers = false;

        viewModel.resetLiveData(lifecycleOwner);

        view.hideAction();
        view.resetStatusLines();
        continueSearch();
    }

    private void continueSearch() {
        if (!checkedKeyStatus) {
            boolean keyIsLocked = viewModel.tokenInfo.getVerifyRetries() == 0;
            boolean keyIsEmpty = viewModel.tokenInfo.isEmpty();
            if (keyIsLocked || keyIsEmpty) {
                // the "checking key status" is fake: we only do it if we already know the key is locked
                view.statusLineAdd(StatusLine.CHECK_KEY);
                delayPerformKeyCheck();
                return;
            } else {
                checkedKeyStatus = true;
            }
        }

        if (!searchedLocally) {
            view.statusLineAdd(StatusLine.SEARCH_LOCAL);
            viewModel.getKeyRetrievalLocal(context).observe(lifecycleOwner, this::processLocalResult);
            return;
        }

        if (!searchedAtUri) {
            view.statusLineAdd(StatusLine.SEARCH_URI);
            viewModel.getKeyRetrievalUri(context).observe(lifecycleOwner, this::processUriResult);
            return;
        }

        if (!searchedKeyservers) {
            view.statusLineAdd(StatusLine.SEARCH_KEYSERVER);
            viewModel.getKeyRetrievalKeyserver(context).observe(lifecycleOwner, this::processKeyserverResult);
            return;
        }

        view.showActionRetryOrFromFile();
    }

    private void delayPerformKeyCheck() {
        new Handler().postDelayed(() -> {
            if (view == null) {
                return;
            }

            performKeyCheck();
        }, 1000);
    }

    private void performKeyCheck() {
        boolean keyIsEmpty = viewModel.tokenInfo.isEmpty();
        boolean putKeyIsSupported = viewModel.tokenInfo.isPutKeySupported();

        if (keyIsEmpty && !putKeyIsSupported) {
            view.statusLineOk();
            view.showActionUnsupportedToken();
            return;
        }

        if (keyIsEmpty) {
            boolean tokenIsAdminLocked = viewModel.tokenInfo.getVerifyAdminRetries() == 0;
            if (tokenIsAdminLocked) {
                view.statusLineError();
                view.showActionLocked(0);
                return;
            }

            view.statusLineOk();
            view.showActionEmptyToken();
            return;
        }

        boolean keyIsLocked = viewModel.tokenInfo.getVerifyRetries() == 0;
        if (keyIsLocked) {
            view.statusLineError();

            int unlockAttemptsLeft = viewModel.tokenInfo.getVerifyAdminRetries();
            view.showActionLocked(unlockAttemptsLeft);
            return;
        }

        view.statusLineOk();

        checkedKeyStatus = true;
        continueSearch();
    }

    @Override
    public void onClickUnlockToken() {
        view.showAdminPinDialog();
    }

    @Override
    public void onMenuClickChangePin() {
        if (!checkedKeyStatus) {
            return;
        }

        if (viewModel.tokenInfo.getVerifyAdminRetries() == 0) {
            view.showErrorCannotUnlock();
            return;
        }

        view.showAdminPinDialog();
    }

    @Override
    public void onInputAdminPin(String adminPin, String newPin) {
        view.operationChangePinSecurityToken(adminPin, newPin);
    }

    @Override
    public void onClickUnlockTokenImpossible() {
        view.showErrorCannotUnlock();
    }

    private void processLocalResult(KeyRetrievalResult result) {
        searchedLocally = true;
        processResult(result);
    }


    private void processUriResult(KeyRetrievalResult result) {
        searchedAtUri = true;
        processResult(result);
    }

    private void processKeyserverResult(KeyRetrievalResult result) {
        searchedKeyservers = true;
        processResult(result);
    }

    private void processContentUriResult(KeyRetrievalResult result) {
        processResult(result);
    }

    private void processResult(KeyRetrievalResult result) {
        log.add(result.getOperationResult(), 0);

        if (result.isSuccess()) {
            processResultSuccess(result);
        } else {
            continueSearchAfterError();
        }
    }

    private void processResultSuccess(KeyRetrievalResult result) {
        view.statusLineOk();

        byte[] importKeyData = result.getKeyData();
        Long masterKeyId = result.getMasterKeyId();
        if (importKeyData != null && masterKeyId != null) {
            view.showActionImport();
            this.importKeyData = importKeyData;
            this.masterKeyId = masterKeyId;
            return;
        }

        if (masterKeyId != null) {
            this.masterKeyId = masterKeyId;
            view.statusLineAdd(StatusLine.TOKEN_CHECK);

            promoteKeyWithTokenInfo(masterKeyId);
            return;
        }

        throw new IllegalArgumentException("Method can only be called with successful result!");
    }

    private void promoteKeyWithTokenInfo(Long masterKeyId) {
        view.operationPromote(masterKeyId, viewModel.tokenInfo.getAid(), viewModel.tokenInfo.getFingerprints());
    }

    @Override
    public void onClickImport() {
        view.statusLineAdd(StatusLine.IMPORT);
        view.hideAction();
        view.operationImportKey(importKeyData);
    }

    @Override
    public void onImportSuccess(OperationResult result) {
        log.add(result, 0);

        view.statusLineOk();
        view.statusLineAdd(StatusLine.TOKEN_PROMOTE);
        promoteKeyWithTokenInfo(masterKeyId);
    }

    @Override
    public void onImportError(OperationResult result) {
        log.add(result, 0);

        view.statusLineError();
    }

    @Override
    public void onPromoteSuccess(OperationResult result) {
        log.add(result, 0);

        view.statusLineOk();
        view.showActionViewKey();
    }

    @Override
    public void onPromoteError(OperationResult result) {
        log.add(result, 0);

        view.statusLineError();
    }

    @Override
    public void onClickRetry() {
        resetAndContinueSearch();
    }

    @Override
    public void onClickViewKey() {
        view.finishAndShowKey(masterKeyId);
    }

    @Override
    public void onClickResetToken() {
        if (!viewModel.tokenInfo.isResetSupported()) {
            TokenType tokenType = viewModel.tokenInfo.getTokenType();
            boolean isGnukOrNitrokeyStart = tokenType == TokenType.GNUK_OLD || tokenType == TokenType.NITROKEY_START_OLD;

            view.showErrorCannotReset(isGnukOrNitrokeyStart);
            return;
        }

        view.showConfirmResetDialog();
    }

    @Override
    public void onClickConfirmReset() {
        view.operationResetSecurityToken();
    }

    @Override
    public void onSecurityTokenResetSuccess(SecurityTokenInfo tokenInfo) {
        viewModel.setTokenInfo(context, tokenInfo);
        resetAndContinueSearch();
    }

    @Override
    public void onSecurityTokenResetCanceled(SecurityTokenInfo tokenInfo) {
        if (tokenInfo != null) {
            viewModel.setTokenInfo(context, tokenInfo);
            resetAndContinueSearch();
        }
    }

    @Override
    public void onClickSetupToken() {
        view.startCreateKeyForToken(viewModel.tokenInfo);
    }

    @Override
    public void onSecurityTokenChangePinSuccess(SecurityTokenInfo tokenInfo) {
        viewModel.setTokenInfo(context, tokenInfo);
        resetAndContinueSearch();
    }

    @Override
    public void onSecurityTokenChangePinCanceled(SecurityTokenInfo tokenInfo) {
        if (tokenInfo != null) {
            viewModel.setTokenInfo(context, tokenInfo);
            resetAndContinueSearch();
        }
    }

    @Override
    public void onClickLoadFile() {
        view.showFileSelectDialog();
    }

    @Override
    public void onFileSelected(Uri contentUri) {
        boolean hasReadPermission = PermissionsUtil.checkReadPermission(context, contentUri);
        if (!hasReadPermission) {
            selectedContentUri = contentUri;
            view.requestStoragePermission();
            return;
        }

        startLoadingFile(contentUri);
    }

    private void startLoadingFile(Uri contentUri) {
        view.resetStatusLines();
        view.statusLineAdd(StatusLine.SEARCH_CONTENT_URI);

        viewModel.getKeyRetrievalContentUri(context, contentUri).observe(lifecycleOwner, this::processContentUriResult);
    }

    @Override
    public void onStoragePermissionGranted() {
        Uri contentUri = selectedContentUri;
        selectedContentUri = null;
        startLoadingFile(contentUri);
    }

    @Override
    public void onStoragePermissionDenied() {
        selectedContentUri = null;
    }

    @Override
    public void onMenuClickViewLog() {
        OperationResult result = new GenericOperationResult(GenericOperationResult.RESULT_OK, log);
        view.showDisplayLogActivity(result);
    }
}

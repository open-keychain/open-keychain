/*
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
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


import java.util.List;

import android.net.Uri;

import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenFragment.StatusLine;


class ManageSecurityTokenContract {
    interface ManageSecurityTokenMvpPresenter {
        void setView(ManageSecurityTokenMvpView createSecurityTokenImportFragment);
        void detach();
        void onActivityCreated();

        void onClickRetry();
        void onClickViewKey();
        void onMenuClickViewLog();

        void onClickImport();
        void onImportSuccess(OperationResult result);
        void onImportError(OperationResult result);

        void onPromoteSuccess(OperationResult result);
        void onPromoteError(OperationResult result);


        void onSecurityTokenChangePinSuccess(SecurityTokenInfo tokenInfo);

        void onSecurityTokenChangePinCanceled(SecurityTokenInfo tokenInfo);

        void onClickLoadFile();
        void onFileSelected(Uri fileUri);
        void onStoragePermissionGranted();
        void onStoragePermissionDenied();

        void onClickResetToken();
        void onClickConfirmReset();
        void onSecurityTokenResetSuccess(SecurityTokenInfo tokenInfo);
        void onSecurityTokenResetCanceled(SecurityTokenInfo tokenInfo);

        void onClickSetupToken();

        void onClickUnlockToken();
        void onMenuClickChangePin();
        void onInputAdminPin(String adminPin, String newPin);

        void onClickUnlockTokenImpossible();
    }

    interface ManageSecurityTokenMvpView {
        void statusLineAdd(StatusLine statusLine);
        void statusLineOk();
        void statusLineError();
        void resetStatusLines();

        void showActionImport();
        void showActionViewKey();
        void showActionRetryOrFromFile();
        void showActionLocked(int unlockAttempts);
        void showActionEmptyToken();
        void hideAction();

        void operationImportKey(byte[] importKeyData);
        void operationPromote(long masterKeyId, byte[] cardAid, List<byte[]> fingerprints);
        void operationResetSecurityToken();
        void operationChangePinSecurityToken(String adminPin, String newPin);

        void finishAndShowKey(long masterKeyId);

        void showFileSelectDialog();
        void showConfirmResetDialog();
        void showAdminPinDialog();
        void startCreateKeyForToken(SecurityTokenInfo tokenInfo);

        void showDisplayLogActivity(OperationResult result);

        void requestStoragePermission();

        void showErrorCannotUnlock();
    }
}

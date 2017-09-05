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


import android.net.Uri;

import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.ui.token.ManageSecurityTokenFragment.StatusLine;


class ManageSecurityTokenContract {
    interface ManageSecurityTokenMvpPresenter {
        void setView(ManageSecurityTokenMvpView createSecurityTokenImportFragment);
        void onActivityCreated();

        void onClickRetry();
        void onClickViewKey();
        void onClickViewLog();

        void onClickImport();
        void onImportSuccess(OperationResult result);
        void onImportError(OperationResult result);

        void onPromoteSuccess(OperationResult result);
        void onPromoteError(OperationResult result);


        void onClickLoadFile();
        void onFileSelected(Uri fileUri);
        void onStoragePermissionGranted();
        void onStoragePermissionDenied();

        void onClickResetToken();
        void onClickConfirmReset();
        void onSecurityTokenResetSuccess();
    }

    interface ManageSecurityTokenMvpView {
        void statusLineAdd(StatusLine statusLine);
        void statusLineOk();
        void statusLineError();
        void resetStatusLines();

        void showActionImport();
        void showActionViewKey();
        void showActionRetryOrFromFile();
        void hideAction();

        void operationImportKey(byte[] importKeyData);
        void operationPromote(long masterKeyId, byte[] cardAid);
        void operationResetSecurityToken();

        void finishAndShowKey(long masterKeyId);

        void showFileSelectDialog();
        void showConfirmResetDialog();

        void showDisplayLogActivity(OperationResult result);

        void requestStoragePermission();
    }
}

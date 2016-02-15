/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.remote;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.remote.ui.RemoteCreateAccountActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteErrorActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteImportKeysActivity;
import org.sufficientlysecure.keychain.remote.ui.RemotePassphraseDialogActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteRegisterActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteSecurityTokenOperationActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteSelectPubKeyActivity;
import org.sufficientlysecure.keychain.remote.ui.SelectAllowedKeysActivity;
import org.sufficientlysecure.keychain.remote.ui.SelectSignKeyIdActivity;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.ViewKeyActivity;

import java.util.ArrayList;

public class ApiPendingIntentFactory {

    Context mContext;
    private Intent mPendingIntentData;

    public ApiPendingIntentFactory(Context context, Intent data) {
        mContext = context;
        mPendingIntentData = data;
    }

    PendingIntent requiredInputPi(RequiredInputParcel requiredInput,
                                  CryptoInputParcel cryptoInput) {

        switch (requiredInput.mType) {
            case NFC_MOVE_KEY_TO_CARD:
            case NFC_DECRYPT:
            case NFC_SIGN: {
                return nfc(requiredInput, cryptoInput);
            }

            case PASSPHRASE: {
                return passphrase(requiredInput, cryptoInput);
            }

            default:
                throw new AssertionError("Unhandled required input type!");
        }
    }

    private PendingIntent nfc(RequiredInputParcel requiredInput, CryptoInputParcel cryptoInput) {
        Intent intent = new Intent(mContext, RemoteSecurityTokenOperationActivity.class);
        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(RemoteSecurityTokenOperationActivity.EXTRA_REQUIRED_INPUT, requiredInput);
        intent.putExtra(RemoteSecurityTokenOperationActivity.EXTRA_CRYPTO_INPUT, cryptoInput);

        return build(intent);
    }

    private PendingIntent passphrase(RequiredInputParcel requiredInput, CryptoInputParcel cryptoInput) {
        Intent intent = new Intent(mContext, RemotePassphraseDialogActivity.class);
        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(RemotePassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
        intent.putExtra(RemotePassphraseDialogActivity.EXTRA_CRYPTO_INPUT, cryptoInput);

        return build(intent);
    }

    PendingIntent selectPublicKey(long[] keyIdsArray, ArrayList<String> missingEmails,
                                  ArrayList<String> duplicateEmails, boolean noUserIdsCheck) {
        Intent intent = new Intent(mContext, RemoteSelectPubKeyActivity.class);
        intent.putExtra(RemoteSelectPubKeyActivity.EXTRA_SELECTED_MASTER_KEY_IDS, keyIdsArray);
        intent.putExtra(RemoteSelectPubKeyActivity.EXTRA_NO_USER_IDS_CHECK, noUserIdsCheck);
        intent.putExtra(RemoteSelectPubKeyActivity.EXTRA_MISSING_EMAILS, missingEmails);
        intent.putExtra(RemoteSelectPubKeyActivity.EXTRA_DUPLICATE_EMAILS, duplicateEmails);

        return build(intent);
    }

    PendingIntent importFromKeyserver(long masterKeyId) {
        Intent intent = new Intent(mContext, RemoteImportKeysActivity.class);
        intent.setAction(RemoteImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT);
        intent.putExtra(RemoteImportKeysActivity.EXTRA_KEY_ID, masterKeyId);

        return build(intent);
    }

    PendingIntent selectAllowedKeys(String packageName) {
        Intent intent = new Intent(mContext, SelectAllowedKeysActivity.class);
        intent.setData(KeychainContract.ApiApps.buildByPackageNameUri(packageName));

        return build(intent);
    }

    PendingIntent showKey(long masterKeyId) {
        Intent intent = new Intent(mContext, ViewKeyActivity.class);
        intent.setData(KeychainContract.KeyRings.buildGenericKeyRingUri(masterKeyId));

        return build(intent);
    }

    PendingIntent selectSignKeyId(String packageName, String preferredUserId) {
        Intent intent = new Intent(mContext, SelectSignKeyIdActivity.class);
        intent.setData(KeychainContract.ApiApps.buildByPackageNameUri(packageName));
        intent.putExtra(SelectSignKeyIdActivity.EXTRA_USER_ID, preferredUserId);

        return build(intent);
    }

    /**
     * @deprecated
     */
    PendingIntent createAccount(String packageName, String accountName) {
        Intent intent = new Intent(mContext, RemoteCreateAccountActivity.class);
        intent.putExtra(RemoteCreateAccountActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(RemoteCreateAccountActivity.EXTRA_ACC_NAME, accountName);

        return build(intent);
    }

    PendingIntent error(String errorMessage) {
        Intent intent = new Intent(mContext, RemoteErrorActivity.class);
        intent.putExtra(RemoteErrorActivity.EXTRA_ERROR_MESSAGE, errorMessage);

        return build(intent);
    }

    private PendingIntent build(Intent intent) {
        // re-attach "data" for pass through. It will be used later to repeat pgp operation
        intent.putExtra(RemoteSecurityTokenOperationActivity.EXTRA_DATA, mPendingIntentData);

        return PendingIntent.getActivity(mContext, 0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    PendingIntent register(String packageName, byte[] packageCertificate) {
        Intent intent = new Intent(mContext, RemoteRegisterActivity.class);
        intent.putExtra(RemoteRegisterActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(RemoteRegisterActivity.EXTRA_PACKAGE_SIGNATURE, packageCertificate);
        intent.putExtra(RemoteRegisterActivity.EXTRA_DATA, mPendingIntentData);

        return PendingIntent.getActivity(mContext, 0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
    }

}

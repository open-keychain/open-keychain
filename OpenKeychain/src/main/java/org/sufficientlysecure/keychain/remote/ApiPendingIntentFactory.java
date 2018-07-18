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

package org.sufficientlysecure.keychain.remote;


import java.util.ArrayList;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import org.sufficientlysecure.keychain.pgp.DecryptVerifySecurityProblem;
import org.sufficientlysecure.keychain.remote.ui.RemoteBackupActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteDisplayTransferCodeActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteErrorActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteImportKeysActivity;
import org.sufficientlysecure.keychain.remote.ui.RemotePassphraseDialogActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteRegisterActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteSecurityProblemDialogActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteSecurityTokenOperationActivity;
import org.sufficientlysecure.keychain.remote.ui.RemoteSelectPubKeyActivity;
import org.sufficientlysecure.keychain.remote.ui.RequestKeyPermissionActivity;
import org.sufficientlysecure.keychain.remote.ui.SelectSignKeyIdActivity;
import org.sufficientlysecure.keychain.remote.ui.dialog.RemoteDeduplicateActivity;
import org.sufficientlysecure.keychain.remote.ui.dialog.RemoteSelectAuthenticationKeyActivity;
import org.sufficientlysecure.keychain.remote.ui.dialog.RemoteSelectIdKeyActivity;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel;
import org.sufficientlysecure.keychain.ui.keyview.ViewKeyActivity;
import org.sufficientlysecure.keychain.util.Passphrase;

public class ApiPendingIntentFactory {

    Context mContext;

    public ApiPendingIntentFactory(Context context) {
        mContext = context;
    }

    PendingIntent requiredInputPi(Intent data, RequiredInputParcel requiredInput,
                                  CryptoInputParcel cryptoInput) {

        switch (requiredInput.mType) {
            case SECURITY_TOKEN_MOVE_KEY_TO_CARD:
            case SECURITY_TOKEN_DECRYPT:
            case SECURITY_TOKEN_AUTH:
            case SECURITY_TOKEN_SIGN: {
                return createSecurityTokenOperationPendingIntent(data, requiredInput, cryptoInput);
            }

            case PASSPHRASE_AUTH:
            case PASSPHRASE: {
                return createPassphrasePendingIntent(data, requiredInput, cryptoInput);
            }

            default:
                throw new AssertionError("Unhandled required input type!");
        }
    }

    private PendingIntent createSecurityTokenOperationPendingIntent(Intent data, RequiredInputParcel requiredInput, CryptoInputParcel cryptoInput) {
        Intent intent = new Intent(mContext, RemoteSecurityTokenOperationActivity.class);
        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(RemoteSecurityTokenOperationActivity.EXTRA_REQUIRED_INPUT, requiredInput);
        intent.putExtra(RemoteSecurityTokenOperationActivity.EXTRA_CRYPTO_INPUT, cryptoInput);

        return createInternal(data, intent);
    }

    private PendingIntent createPassphrasePendingIntent(Intent data, RequiredInputParcel requiredInput, CryptoInputParcel cryptoInput) {
        Intent intent = new Intent(mContext, RemotePassphraseDialogActivity.class);
        // pass params through to activity that it can be returned again later to repeat pgp operation
        intent.putExtra(RemotePassphraseDialogActivity.EXTRA_REQUIRED_INPUT, requiredInput);
        intent.putExtra(RemotePassphraseDialogActivity.EXTRA_CRYPTO_INPUT, cryptoInput);

        return createInternal(data, intent);
    }

    public PendingIntent createSelectPublicKeyPendingIntent(Intent data, long[] keyIdsArray,
            ArrayList<String> missingEmails,
            ArrayList<String> duplicateEmails, boolean noUserIdsCheck) {
        Intent intent = new Intent(mContext, RemoteSelectPubKeyActivity.class);
        intent.putExtra(RemoteSelectPubKeyActivity.EXTRA_SELECTED_MASTER_KEY_IDS, keyIdsArray);
        intent.putExtra(RemoteSelectPubKeyActivity.EXTRA_NO_USER_IDS_CHECK, noUserIdsCheck);
        intent.putExtra(RemoteSelectPubKeyActivity.EXTRA_MISSING_EMAILS, missingEmails);
        intent.putExtra(RemoteSelectPubKeyActivity.EXTRA_DUPLICATE_EMAILS, duplicateEmails);

        return createInternal(data, intent);
    }

    public PendingIntent createDeduplicatePendingIntent(String packageName, Intent data, ArrayList<String> duplicateEmails) {
        Intent intent = new Intent(mContext, RemoteDeduplicateActivity.class);

        intent.putExtra(RemoteDeduplicateActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(RemoteDeduplicateActivity.EXTRA_DUPLICATE_EMAILS, duplicateEmails);

        return createInternal(data, intent);
    }


    PendingIntent createImportFromKeyserverPendingIntent(Intent data, long masterKeyId) {
        Intent intent = new Intent(mContext, RemoteImportKeysActivity.class);
        intent.setAction(RemoteImportKeysActivity.ACTION_IMPORT_KEY_FROM_KEYSERVER_AND_RETURN_RESULT);
        intent.putExtra(RemoteImportKeysActivity.EXTRA_KEY_ID, masterKeyId);

        return createInternal(data, intent);
    }

    public PendingIntent createRequestKeyPermissionPendingIntent(Intent data, String packageName, long... masterKeyIds) {
        Intent intent = new Intent(mContext, RequestKeyPermissionActivity.class);
        intent.putExtra(RequestKeyPermissionActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(RequestKeyPermissionActivity.EXTRA_REQUESTED_KEY_IDS, masterKeyIds);

        return createInternal(data, intent);
    }

    PendingIntent createShowKeyPendingIntent(Intent data, long masterKeyId) {
        Intent intent = ViewKeyActivity.getViewKeyActivityIntent(mContext, masterKeyId);

        return createInternal(data, intent);
    }

    public PendingIntent createSelectSignKeyIdLegacyPendingIntent(Intent data, String packageName,
            byte[] packageSignature, String preferredUserId) {
        Intent intent = new Intent(mContext, SelectSignKeyIdActivity.class);
        intent.putExtra(SelectSignKeyIdActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(SelectSignKeyIdActivity.EXTRA_PACKAGE_SIGNATURE, packageSignature);
        intent.putExtra(SelectSignKeyIdActivity.EXTRA_USER_ID, preferredUserId);

        return createInternal(data, intent);
    }

    public PendingIntent createSelectSignKeyIdPendingIntent(Intent data, String packageName,
            byte[] packageSignature, String preferredUserId, boolean showAutocryptHint) {
        Intent intent = new Intent(mContext, RemoteSelectIdKeyActivity.class);
        intent.putExtra(RemoteSelectIdKeyActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(RemoteSelectIdKeyActivity.EXTRA_PACKAGE_SIGNATURE, packageSignature);
        intent.putExtra(RemoteSelectIdKeyActivity.EXTRA_USER_ID, preferredUserId);
        intent.putExtra(RemoteSelectIdKeyActivity.EXTRA_SHOW_AUTOCRYPT_HINT, showAutocryptHint);

        return createInternal(data, intent);
    }

    public PendingIntent createSelectAuthenticationKeyIdPendingIntent(Intent data, String packageName) {
        Intent intent = new Intent(mContext, RemoteSelectAuthenticationKeyActivity.class);
        intent.putExtra(RemoteSelectAuthenticationKeyActivity.EXTRA_PACKAGE_NAME, packageName);

        return createInternal(data, intent);
    }

    PendingIntent createBackupPendingIntent(Intent data, long[] masterKeyIds, boolean backupSecret) {
        Intent intent = new Intent(mContext, RemoteBackupActivity.class);
        intent.putExtra(RemoteBackupActivity.EXTRA_MASTER_KEY_IDS, masterKeyIds);
        intent.putExtra(RemoteBackupActivity.EXTRA_SECRET, backupSecret);

        return createInternal(data, intent);
    }

    PendingIntent createErrorPendingIntent(Intent data, String errorMessage) {
        Intent intent = new Intent(mContext, RemoteErrorActivity.class);
        intent.putExtra(RemoteErrorActivity.EXTRA_ERROR_MESSAGE, errorMessage);

        return createInternal(data, intent);
    }

    PendingIntent createSecurityProblemIntent(String packageName, DecryptVerifySecurityProblem securityProblem,
            boolean supportOverride) {
        Intent intent = new Intent(mContext, RemoteSecurityProblemDialogActivity.class);
        intent.putExtra(RemoteSecurityProblemDialogActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(RemoteSecurityProblemDialogActivity.EXTRA_SECURITY_PROBLEM, securityProblem);
        intent.putExtra(RemoteSecurityProblemDialogActivity.EXTRA_SUPPORT_OVERRIDE, supportOverride);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //noinspection ResourceType, looks like lint is missing FLAG_IMMUTABLE
            return PendingIntent.getActivity(mContext, 0, intent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getActivity(mContext, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    public PendingIntent createDisplayTransferCodePendingIntent(Passphrase autocryptTransferCode) {
        Intent intent = new Intent(mContext, RemoteDisplayTransferCodeActivity.class);
        intent.putExtra(RemoteDisplayTransferCodeActivity.EXTRA_TRANSFER_CODE, autocryptTransferCode);

        return createInternal(null, intent);
    }

    private PendingIntent createInternal(Intent data, Intent intent) {
        // re-attach "data" for pass through. It will be used later to repeat pgp operation
        if (data != null) {
            intent.putExtra(RemoteSecurityTokenOperationActivity.EXTRA_DATA, data);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //noinspection ResourceType, looks like lint is missing FLAG_IMMUTABLE
            return PendingIntent.getActivity(mContext, 0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getActivity(mContext, 0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);
        }
    }

    public PendingIntent createRegisterPendingIntent(Intent data, String packageName, byte[] packageCertificate) {
        Intent intent = new Intent(mContext, RemoteRegisterActivity.class);
        intent.putExtra(RemoteRegisterActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(RemoteRegisterActivity.EXTRA_PACKAGE_SIGNATURE, packageCertificate);
        intent.putExtra(RemoteRegisterActivity.EXTRA_DATA, data);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //noinspection ResourceType, looks like lint is missing FLAG_IMMUTABLE
            return PendingIntent.getActivity(mContext, 0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT
                            | PendingIntent.FLAG_IMMUTABLE);
        } else {
            return PendingIntent.getActivity(mContext, 0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);
        }
    }
}

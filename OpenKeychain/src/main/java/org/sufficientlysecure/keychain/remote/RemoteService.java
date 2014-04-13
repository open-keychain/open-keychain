/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Binder;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.KeychainContract;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.remote.ui.RemoteServiceActivity;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Abstract service class for remote APIs that handle app registration and user input.
 */
public abstract class RemoteService extends Service {

    public static class WrongPackageSignatureException extends Exception {
        private static final long serialVersionUID = -8294642703122196028L;

        public WrongPackageSignatureException(String message) {
            super(message);
        }
    }

    Context mContext;
    ProviderHelper mProviderHelper;

    public Context getContext() {
        return mContext;
    }

    /**
     * Checks if caller is allowed to access the API
     *
     * @param data
     * @return null if caller is allowed, or a Bundle with a PendingIntent
     */
    protected Intent isAllowed(Intent data) {
        try {
            if (isCallerAllowed(false)) {
                return null;
            } else {
                String packageName = getCurrentCallingPackage();
                Log.d(Constants.TAG, "isAllowed packageName: " + packageName);

                byte[] packageSignature;
                try {
                    packageSignature = getPackageSignature(packageName);
                } catch (NameNotFoundException e) {
                    Log.e(Constants.TAG, "Should not happen, returning!", e);
                    // return error
                    Intent result = new Intent();
                    result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
                    result.putExtra(OpenPgpApi.RESULT_ERROR,
                            new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
                    return result;
                }
                Log.e(Constants.TAG, "Not allowed to use service! return PendingIntent for registration!");

                Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
                intent.setAction(RemoteServiceActivity.ACTION_REGISTER);
                intent.putExtra(RemoteServiceActivity.EXTRA_PACKAGE_NAME, packageName);
                intent.putExtra(RemoteServiceActivity.EXTRA_PACKAGE_SIGNATURE, packageSignature);
                intent.putExtra(RemoteServiceActivity.EXTRA_DATA, data);

                PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0,
                        intent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_ONE_SHOT);

                // return PendingIntent to be executed by client
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                result.putExtra(OpenPgpApi.RESULT_INTENT, pi);

                return result;
            }
        } catch (WrongPackageSignatureException e) {
            Log.e(Constants.TAG, "wrong signature!", e);

            Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
            intent.setAction(RemoteServiceActivity.ACTION_ERROR_MESSAGE);
            intent.putExtra(RemoteServiceActivity.EXTRA_ERROR_MESSAGE,
                    getString(R.string.api_error_wrong_signature));
            intent.putExtra(RemoteServiceActivity.EXTRA_DATA, data);

            PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0,
                    intent,
                    PendingIntent.FLAG_CANCEL_CURRENT);

            // return PendingIntent to be executed by client
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
            result.putExtra(OpenPgpApi.RESULT_INTENT, pi);

            return result;
        }
    }

    private byte[] getPackageSignature(String packageName) throws NameNotFoundException {
        PackageInfo pkgInfo = getPackageManager().getPackageInfo(packageName,
                PackageManager.GET_SIGNATURES);
        Signature[] signatures = pkgInfo.signatures;
        // TODO: Only first signature?!
        byte[] packageSignature = signatures[0].toByteArray();

        return packageSignature;
    }

    /**
     * Returns package name associated with the UID, which is assigned to the process that sent you the
     * current transaction that is being processed :)
     *
     * @return package name
     */
    protected String getCurrentCallingPackage() {
        // TODO:
        // callingPackages contains more than one entry when sharedUserId has been used...
        String[] callingPackages = getPackageManager().getPackagesForUid(Binder.getCallingUid());
        String currentPkg = callingPackages[0];
        Log.d(Constants.TAG, "currentPkg: " + currentPkg);

        return currentPkg;
    }

    /**
     * Retrieves AccountSettings from database for the application calling this remote service
     *
     * @return
     */
    protected AccountSettings getAccSettings(String accountName) {
        String currentPkg = getCurrentCallingPackage();
        Log.d(Constants.TAG, "accountName: " + accountName);

        Uri uri = KeychainContract.ApiAccounts.buildByPackageAndAccountUri(currentPkg, accountName);

        AccountSettings settings = mProviderHelper.getApiAccountSettings(uri);

        return settings; // can be null!
    }

    protected Intent getCreateAccountIntent(Intent data, String accountName) {
        String packageName = getCurrentCallingPackage();
        Log.d(Constants.TAG, "accountName: " + accountName);

        Intent intent = new Intent(getBaseContext(), RemoteServiceActivity.class);
        intent.setAction(RemoteServiceActivity.ACTION_CREATE_ACCOUNT);
        intent.putExtra(RemoteServiceActivity.EXTRA_PACKAGE_NAME, packageName);
        intent.putExtra(RemoteServiceActivity.EXTRA_ACC_NAME, accountName);
        intent.putExtra(RemoteServiceActivity.EXTRA_DATA, data);

        PendingIntent pi = PendingIntent.getActivity(getBaseContext(), 0,
                intent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        // return PendingIntent to be executed by client
        Intent result = new Intent();
        result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
        result.putExtra(OpenPgpApi.RESULT_INTENT, pi);

        return result;
    }

    /**
     * Checks if process that binds to this service (i.e. the package name corresponding to the
     * process) is in the list of allowed package names.
     *
     * @param allowOnlySelf allow only Keychain app itself
     * @return true if process is allowed to use this service
     * @throws WrongPackageSignatureException
     */
    private boolean isCallerAllowed(boolean allowOnlySelf) throws WrongPackageSignatureException {
        return isUidAllowed(Binder.getCallingUid(), allowOnlySelf);
    }

    private boolean isUidAllowed(int uid, boolean allowOnlySelf)
            throws WrongPackageSignatureException {
        if (android.os.Process.myUid() == uid) {
            return true;
        }
        if (allowOnlySelf) { // barrier
            return false;
        }

        String[] callingPackages = getPackageManager().getPackagesForUid(uid);

        // is calling package allowed to use this service?
        for (int i = 0; i < callingPackages.length; i++) {
            String currentPkg = callingPackages[i];

            if (isPackageAllowed(currentPkg)) {
                return true;
            }
        }

        Log.d(Constants.TAG, "Uid is NOT allowed!");
        return false;
    }

    /**
     * Checks if packageName is a registered app for the API. Does not return true for own package!
     *
     * @param packageName
     * @return
     * @throws WrongPackageSignatureException
     */
    private boolean isPackageAllowed(String packageName) throws WrongPackageSignatureException {
        Log.d(Constants.TAG, "isPackageAllowed packageName: " + packageName);

        ArrayList<String> allowedPkgs = mProviderHelper.getRegisteredApiApps();
        Log.d(Constants.TAG, "allowed: " + allowedPkgs);

        // check if package is allowed to use our service
        if (allowedPkgs.contains(packageName)) {
            Log.d(Constants.TAG, "Package is allowed! packageName: " + packageName);

            // check package signature
            byte[] currentSig;
            try {
                currentSig = getPackageSignature(packageName);
            } catch (NameNotFoundException e) {
                throw new WrongPackageSignatureException(e.getMessage());
            }

            byte[] storedSig = mProviderHelper.getApiAppSignature(packageName);
            if (Arrays.equals(currentSig, storedSig)) {
                Log.d(Constants.TAG,
                        "Package signature is correct! (equals signature from database)");
                return true;
            } else {
                throw new WrongPackageSignatureException(
                        "PACKAGE NOT ALLOWED! Signature wrong! (Signature not " +
                                "equals signature from database)");
            }
        }

        Log.d(Constants.TAG, "Package is NOT allowed! packageName: " + packageName);
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        mProviderHelper = new ProviderHelper(this);
    }

}

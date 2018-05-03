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


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Binder;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import timber.log.Timber;


/**
 * Abstract service class for remote APIs that handle app registration and user input.
 */
public class ApiPermissionHelper {

    private final Context mContext;
    private final ApiDataAccessObject mApiDao;
    private PackageManager mPackageManager;

    public ApiPermissionHelper(Context context, ApiDataAccessObject apiDao) {
        mContext = context;
        mPackageManager = context.getPackageManager();
        mApiDao = apiDao;
    }

    public static class WrongPackageCertificateException extends Exception {
        private static final long serialVersionUID = -8294642703122196028L;

        public WrongPackageCertificateException(String message) {
            super(message);
        }
    }

    /** Returns true iff the caller is allowed, or false on any type of problem.
     * This method should only be used in cases where error handling is dealt with separately.
     */
    public boolean isAllowedIgnoreErrors() {
        try {
            return isCallerAllowed();
        } catch (WrongPackageCertificateException e) {
            return false;
        }
    }

    /**
     * Checks if caller is allowed to access the API
     *
     * @return null if caller is allowed, or a Bundle with a PendingIntent
     */
    protected Intent isAllowedOrReturnIntent(Intent data) {
        ApiPendingIntentFactory piFactory = new ApiPendingIntentFactory(mContext);
        try {
            if (isCallerAllowed()) {
                return null;
            } else {
                String packageName = getCurrentCallingPackage();
                Timber.d("isAllowed packageName: " + packageName);

                byte[] packageCertificate;
                try {
                    packageCertificate = getPackageCertificate(packageName);
                } catch (NameNotFoundException e) {
                    Timber.e(e, "Should not happen, returning!");
                    // return error
                    Intent result = new Intent();
                    result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_ERROR);
                    result.putExtra(OpenPgpApi.RESULT_ERROR,
                            new OpenPgpError(OpenPgpError.GENERIC_ERROR, e.getMessage()));
                    return result;
                }
                Timber.e("Not allowed to use service! return PendingIntent for registration!");

                PendingIntent pi = piFactory.createRegisterPendingIntent(data, packageName, packageCertificate);

                // return PendingIntent to be executed by client
                Intent result = new Intent();
                result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
                result.putExtra(OpenPgpApi.RESULT_INTENT, pi);

                return result;
            }
        } catch (WrongPackageCertificateException e) {
            Timber.e(e, "wrong signature!");

            PendingIntent pi = piFactory.createErrorPendingIntent(data, mContext.getString(R.string.api_error_wrong_signature));

            // return PendingIntent to be executed by client
            Intent result = new Intent();
            result.putExtra(OpenPgpApi.RESULT_CODE, OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED);
            result.putExtra(OpenPgpApi.RESULT_INTENT, pi);

            return result;
        }
    }

    byte[] getPackageCertificateOrError(String packageName) {
        try {
            return getPackageCertificate(packageName);
        } catch (NameNotFoundException e) {
            throw new AssertionError("Package signature must be retrievable");
        }
    }

    private byte[] getPackageCertificate(String packageName) throws NameNotFoundException {
        @SuppressLint("PackageManagerGetSignatures") // we do check the byte array of *all* signatures
        PackageInfo pkgInfo = mContext.getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        // NOTE: Silly Android API naming: Signatures are actually certificates
        Signature[] certificates = pkgInfo.signatures;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Signature cert : certificates) {
            try {
                outputStream.write(cert.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Should not happen! Writing ByteArrayOutputStream to concat certificates failed");
            }
        }

        // Even if an apk has several certificates, these certificates should never change
        // Google Play does not allow the introduction of new certificates into an existing apk
        // Also see this attack: http://stackoverflow.com/a/10567852
        return outputStream.toByteArray();
    }

    /**
     * Returns package name associated with the UID, which is assigned to the process that sent you the
     * current transaction that is being processed :)
     *
     * @return package name
     */
    protected String getCurrentCallingPackage() {
        String[] callingPackages = mPackageManager.getPackagesForUid(Binder.getCallingUid());

        // NOTE: No support for sharedUserIds
        // callingPackages contains more than one entry when sharedUserId has been used
        // No plans to support sharedUserIds due to many bugs connected to them:
        // http://java-hamster.blogspot.de/2010/05/androids-shareduserid.html
        String currentPkg = callingPackages[0];
        Timber.d("currentPkg: " + currentPkg);

        return currentPkg;
    }

    /**
     * Checks if process that binds to this service (i.e. the package name corresponding to the
     * process) is in the list of allowed package names.
     *
     * @return true if process is allowed to use this service
     * @throws WrongPackageCertificateException
     */
    private boolean isCallerAllowed() throws WrongPackageCertificateException {
        return isUidAllowed(Binder.getCallingUid());
    }

    private boolean isUidAllowed(int uid)
            throws WrongPackageCertificateException {

        String[] callingPackages = mPackageManager.getPackagesForUid(uid);

        // is calling package allowed to use this service?
        for (String currentPkg : callingPackages) {
            if (isPackageAllowed(currentPkg)) {
                return true;
            }
        }

        Timber.e("Uid is NOT allowed!");
        return false;
    }

    /**
     * Checks if packageName is a registered app for the API. Does not return true for own package!
     *
     * @throws WrongPackageCertificateException
     */
    public boolean isPackageAllowed(String packageName) throws WrongPackageCertificateException {
        Timber.d("isPackageAllowed packageName: " + packageName);

        byte[] storedPackageCert = mApiDao.getApiAppCertificate(packageName);

        boolean isKnownPackage = storedPackageCert != null;
        if (!isKnownPackage) {
            Timber.d("Package is NOT allowed! packageName: " + packageName);
            return false;
        }
        Timber.d("Package is allowed! packageName: " + packageName);

        byte[] currentPackageCert;
        try {
            currentPackageCert = getPackageCertificate(packageName);
        } catch (NameNotFoundException e) {
            throw new WrongPackageCertificateException(e.getMessage());
        }

        boolean packageCertMatchesStored = Arrays.equals(currentPackageCert, storedPackageCert);
        if (packageCertMatchesStored) {
            Timber.d("Package certificate matches expected.");
            return true;
        }

        throw new WrongPackageCertificateException("PACKAGE NOT ALLOWED DUE TO CERTIFICATE MISMATCH!");
    }

}

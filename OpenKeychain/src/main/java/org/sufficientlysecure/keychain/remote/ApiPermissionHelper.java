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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.os.Binder;

import timber.log.Timber;


/**
 * Abstract service class for remote APIs that handle app registration and user input.
 */
public class ApiPermissionHelper {

    private final Context mContext;
    private PackageManager mPackageManager;

    public ApiPermissionHelper(Context context) {
        mContext = context;
        mPackageManager = context.getPackageManager();
    }

    public static class WrongPackageCertificateException extends Exception {
        private static final long serialVersionUID = -8294642703122196028L;

        public WrongPackageCertificateException(String message) {
            super(message);
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

}

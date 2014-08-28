/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.pgp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.Preferences;
import org.sufficientlysecure.keychain.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.regex.Pattern;

public class PgpHelper {

    public static final Pattern PGP_MESSAGE = Pattern.compile(
            ".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*", Pattern.DOTALL);

    public static final Pattern PGP_CLEARTEXT_SIGNATURE = Pattern
            .compile(".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----" +
                    "BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                    Pattern.DOTALL);

    public static final Pattern PGP_PUBLIC_KEY = Pattern.compile(
            ".*?(-----BEGIN PGP PUBLIC KEY BLOCK-----.*?-----END PGP PUBLIC KEY BLOCK-----).*",
            Pattern.DOTALL);

    public static String getVersion(Context context) {
        String version;
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(Constants.PACKAGE_NAME, 0);
            version = pi.versionName;
            return version;
        } catch (NameNotFoundException e) {
            Log.e(Constants.TAG, "Version could not be retrieved!", e);
            return "0.0";
        }
    }

    public static String getVersionForHeader(Context context) {
        if(Preferences.getPreferences(context).getWriteVersionHeader()){
            return "OpenKeychain v" + getVersion(context);
        } else {
            return null;
        }
    }

    /**
     * Deletes file securely by overwriting it with random data before deleting it.
     * <p/>
     * TODO: Does this really help on flash storage?
     *
     * @param context
     * @param progressable
     * @param file
     * @throws IOException
     */
    public static void deleteFileSecurely(Context context, Progressable progressable, File file)
            throws IOException {
        long length = file.length();
        SecureRandom random = new SecureRandom();
        RandomAccessFile raf = new RandomAccessFile(file, "rws");
        raf.seek(0);
        raf.getFilePointer();
        byte[] data = new byte[1 << 16];
        int pos = 0;
        String msg = context.getString(R.string.progress_deleting_securely, file.getName());
        while (pos < length) {
            if (progressable != null) {
                progressable.setProgress(msg, (int) (100 * pos / length), 100);
            }
            random.nextBytes(data);
            raf.write(data);
            pos += data.length;
        }
        raf.close();
        file.delete();
    }
}

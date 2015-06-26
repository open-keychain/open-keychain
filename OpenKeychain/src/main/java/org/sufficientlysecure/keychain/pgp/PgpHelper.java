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
import android.text.TextUtils;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Preferences;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.regex.Matcher;
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

    /**
     * Deletes file securely by overwriting it with random data before deleting it.
     * <p/>
     * TODO: Does this really help on flash storage?
     *
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

    /**
     * Fixing broken PGP MESSAGE Strings coming from GMail/AOSP Mail
     */
    public static String fixPgpMessage(String message) {
        // windows newline -> unix newline
        message = message.replaceAll("\r\n", "\n");
        // Mac OS before X newline -> unix newline
        message = message.replaceAll("\r", "\n");

        // remove whitespaces before newline
        message = message.replaceAll(" +\n", "\n");
        // only two consecutive newlines are allowed
        message = message.replaceAll("\n\n+", "\n\n");

        // replace non breakable spaces
        message = message.replaceAll("\\xa0", " ");

        return message;
    }

    /**
     * Fixing broken PGP SIGNED MESSAGE Strings coming from GMail/AOSP Mail
     */
    public static String fixPgpCleartextSignature(CharSequence input) {
        if (!TextUtils.isEmpty(input)) {
            String text = input.toString();

            // windows newline -> unix newline
            text = text.replaceAll("\r\n", "\n");
            // Mac OS before X newline -> unix newline
            text = text.replaceAll("\r", "\n");

            return text;
        } else {
            return null;
        }
    }

    public static String getPgpContent(CharSequence input) {
        // only decrypt if clipboard content is available and a pgp message or cleartext signature
        if (!TextUtils.isEmpty(input)) {
            Log.dEscaped(Constants.TAG, "input: " + input);

            Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(input);
            if (matcher.matches()) {
                String text = matcher.group(1);
                text = fixPgpMessage(text);

                Log.dEscaped(Constants.TAG, "input fixed: " + text);
                return text;
            } else {
                matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(input);
                if (matcher.matches()) {
                    String text = matcher.group(1);
                    text = fixPgpCleartextSignature(text);

                    Log.dEscaped(Constants.TAG, "input fixed: " + text);
                    return text;
                } else {
                    return null;
                }
            }
        } else {
            return null;
        }
    }

}

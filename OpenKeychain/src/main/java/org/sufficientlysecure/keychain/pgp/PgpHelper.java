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


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.annotation.NonNull;
import android.text.TextUtils;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

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

    public static String getPgpMessageContent(@NonNull CharSequence input) {
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
    }

    public static String getPgpKeyContent(@NonNull CharSequence input) {
        Log.dEscaped(Constants.TAG, "input: " + input);

        Matcher matcher = PgpHelper.PGP_PUBLIC_KEY.matcher(input);
        if (matcher.matches()) {
            String text = matcher.group(1);
            text = fixPgpMessage(text);

            Log.dEscaped(Constants.TAG, "input fixed: " + text);
            return text;
        }
        return null;
    }

}

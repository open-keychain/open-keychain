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

package org.sufficientlysecure.keychain.pgp;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import timber.log.Timber;


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
    private static final Pattern KEYDATA_START_PATTERN = Pattern.compile("\\s(m[A-Q])");

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
        Timber.d("input: %s");

        Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(input);
        if (matcher.matches()) {
            String text = matcher.group(1);
            text = fixPgpMessage(text);

            Timber.d("input fixed: %s", text);
            return text;
        } else {
            matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(input);
            if (matcher.matches()) {
                String text = matcher.group(1);
                text = fixPgpCleartextSignature(text);

                Timber.d("input fixed: %s", text);
                return text;
            } else {
                return null;
            }
        }
    }

    public static String getPgpPublicKeyContent(@NonNull CharSequence input) {
        Timber.d("input: %s", input);

        Matcher matcher = PgpHelper.PGP_PUBLIC_KEY.matcher(input);
        if (!matcher.matches()) {
            return null;
        }

        String text = matcher.group(1);
        text = fixPgpMessage(text);
        text = reformatPgpPublicKeyBlock(text);

        // Log.dEscaped(Constants.TAG, "input fixed: " + text);
        return text;
    }

    @Nullable
    @CheckResult
    @VisibleForTesting
    /* Reformats a public key block with messed up whitespace. This will strip headers in the process. */
    static String reformatPgpPublicKeyBlock(@NonNull String text) {
        StringBuilder reformattedKeyBlocks = new StringBuilder();

        /*
            This method assumes that the base64 encoded public key data always starts with "m[A-Q]".
            This holds based on a few assumptions based on the following observations:

            mA encodes 12 bits: 1001 1000 0000
            ...
            mP encodes 12 bits: 1001 1000 1111
            mQ encodes 12 bits: 1001 1001 0000
                                1234 5678

            The first bit is a constant 1, the second is 0 for old packet format. Bits 3
            through 6 encode the packet tag (constant 6 = b0110). Bits 7 and 8 encode the
            length type of the packet, with a value of b00 or b01 referring to a 2- or
            3-octet header, respectively. The following four bits are part of the length
            header.

            Thus we make the following assumptions:
            - The packet uses the old packet format. Since the public key packet tag is available in the old format,
              there is no reason to use the new one - implementations *could* do that, however.
            - The first packet is a public key.
            - The length is encoded as one or two bytes.
            - If the length is encoded as one byte, the second character may be A through P (four length bits).
            - If the length is encoded as two bytes, the second character is Q. This fixes the first four bits of
              the length field to zero, limiting the length to 4096.
         */

        while (!text.isEmpty()) {
            int indexOfBlock = text.indexOf("-----BEGIN PGP PUBLIC KEY BLOCK-----");
            int indexOfBlockEnd = text.indexOf("-----END PGP PUBLIC KEY BLOCK-----");
            if (indexOfBlock < 0 || indexOfBlockEnd < 0) {
                break;
            }

            Matcher matcher = KEYDATA_START_PATTERN.matcher(text);
            if (!matcher.find()) {
                Timber.e("Could not find start of key data!");
                break;
            }
            int indexOfPubkeyMaterial = matcher.start(1);

            String keyMaterial = text.substring(indexOfPubkeyMaterial, indexOfBlockEnd);
            keyMaterial = keyMaterial.replaceAll("\\s+", "\n");

            reformattedKeyBlocks.append("-----BEGIN PGP PUBLIC KEY BLOCK-----\n");
            reformattedKeyBlocks.append('\n');
            reformattedKeyBlocks.append(keyMaterial);
            reformattedKeyBlocks.append("-----END PGP PUBLIC KEY BLOCK-----\n");

            text = text.substring(indexOfBlockEnd +34).trim();
        }

        if (reformattedKeyBlocks.length() == 0) {
            return null;
        }

        return reformattedKeyBlocks.toString();
    }

}

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


import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

public class PgpAsciiArmorReformatter {

    public static final Pattern PGP_MESSAGE = Pattern.compile(
            ".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*", Pattern.DOTALL);

    public static final Pattern PGP_CLEARTEXT_SIGNATURE = Pattern
            .compile(".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----" +
                    "BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                    Pattern.DOTALL);

    public static final Pattern PGP_PUBLIC_KEY = Pattern.compile(
            ".*?(-----BEGIN PGP PUBLIC KEY BLOCK-----.*?-----END PGP PUBLIC KEY BLOCK-----).*",
            Pattern.DOTALL);
    private static final Pattern ASCII_ARMOR_LINE = Pattern.compile("\\s[a-zA-Z0-9=/+]{30,}\\s");
    private static final Pattern HEADER_VALUE = Pattern.compile("\\s[a-zA-Z0-9]+: ");
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

    public static String getPgpMessageContent(@NonNull String input) {
        Log.dEscaped(Constants.TAG, "input: " + input);

        int indexOfMsg = input.indexOf("-----BEGIN PGP MESSAGE-----");
        if (indexOfMsg < 0) {
            return null;
        }
        input = input.substring(indexOfMsg);

        int indexOfEnd = input.indexOf("-----END PGP MESSAGE-----");
        if (indexOfEnd < 0) {
            return null;
        }

        // TODO deal with cleartext signatures
        String text = fixPgpMessage(input);
        text = reformatPgpEncryptedMessageBlock(text);

        return text;
    }

    public static String getPgpPublicKeyContent(@NonNull CharSequence input) {
        Log.dEscaped(Constants.TAG, "input: " + input);

        Matcher matcher = PgpAsciiArmorReformatter.PGP_PUBLIC_KEY.matcher(input);
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
    static String reformatPgpEncryptedMessageBlock(@NonNull String text) {
        text = text.substring("-----BEGIN PGP MESSAGE-----".length()).trim();
        text = text.substring(0, text.indexOf("-----END PGP MESSAGE-----"));

        // deal with headers
        ArrayList<String> headerLines = new ArrayList<>();
        int headerIndex;
        while ((headerIndex = text.indexOf(": ")) >= 0) {
            String headerName = text.substring(0, headerIndex).trim();
            text = text.substring(headerIndex + 2);

            int headerContentEnd;

            Matcher matcher = HEADER_VALUE.matcher(text);
            if (matcher.find()) {
                headerContentEnd = matcher.start();
            } else {
                Matcher matcher2 = ASCII_ARMOR_LINE.matcher(text);
                if (!matcher2.find()) {
                    return null;
                }
                headerContentEnd = matcher2.start();
            }

            String headerContent = text.substring(0, headerContentEnd).trim();
            text = text.substring(headerContentEnd);

            headerLines.add(headerName + ": " + headerContent);
        }

        // change every consecutive amount of whitespace into newlines
        String data = text.trim().replaceAll("\\s+", "\n");

        StringBuilder reformattedKeyBlocks = new StringBuilder();

        reformattedKeyBlocks.append("-----BEGIN PGP MESSAGE-----\n");
        for (String headerLine : headerLines) {
            reformattedKeyBlocks.append(headerLine);
            reformattedKeyBlocks.append('\n');
        }
        reformattedKeyBlocks.append('\n');
        reformattedKeyBlocks.append(data);
        reformattedKeyBlocks.append('\n');
        reformattedKeyBlocks.append("-----END PGP MESSAGE-----\n");

        return reformattedKeyBlocks.toString();
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
                Log.e(Constants.TAG, "Could not find start of key data!");
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

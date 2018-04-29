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

package org.sufficientlysecure.keychain.util;


import java.nio.CharBuffer;
import java.security.SecureRandom;
import java.util.Random;
import java.util.regex.Pattern;

import android.support.annotation.VisibleForTesting;

// see https://autocrypt.org/level1.html#setup-code
public class Numeric9x4PassphraseUtil {
    public static Passphrase generateNumeric9x4Passphrase() {
        return generateNumeric9x4Passphrase(new SecureRandom());
    }

    @VisibleForTesting
    static Passphrase generateNumeric9x4Passphrase(Random r) {
        StringBuilder code = new StringBuilder(36);
        for (int i = 0; i < 36; i++) {
            boolean isBeginningOfBlock = i > 0 && (i % 4) == 0;
            if (isBeginningOfBlock) {
                code.append('-');
            }

            String digit = Integer.toString(r.nextInt(10));
            code.append(digit);
        }

        return new Passphrase(code.toString());
    }

    private static final Pattern AUTOCRYPT_TRANSFER_CODE = Pattern.compile("(\\d{4}-){8}\\d{4}");

    public static boolean isNumeric9x4Passphrase(Passphrase transferCodeChars) {
        return isNumeric9x4Passphrase(CharBuffer.wrap(transferCodeChars.getCharArray()));
    }

    public static boolean isNumeric9x4Passphrase(CharSequence code) {
        return AUTOCRYPT_TRANSFER_CODE.matcher(code).matches();
    }
}

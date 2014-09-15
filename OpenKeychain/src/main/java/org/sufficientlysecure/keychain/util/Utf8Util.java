/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.sufficientlysecure.keychain.Constants;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;

public class Utf8Util {

    public static boolean isValidUTF8(byte[] input) {
        CharsetDecoder cs = Charset.forName("UTF-8").newDecoder();

        try {
            cs.decode(ByteBuffer.wrap(input));
            return true;
        } catch (CharacterCodingException e) {
            return false;
        }
    }

    public static String fromUTF8ByteArrayReplaceBadEncoding(byte[] input) {
        final CharsetDecoder charsetDecoder = Charset.forName("UTF-8").newDecoder();
        charsetDecoder.onMalformedInput(CodingErrorAction.REPLACE);
        charsetDecoder.onUnmappableCharacter(CodingErrorAction.REPLACE);

        try {
            return charsetDecoder.decode(ByteBuffer.wrap(input)).toString();
        } catch (CharacterCodingException e) {
            Log.e(Constants.TAG, "Decoding failed!", e);
            return charsetDecoder.replacement();
        }
    }
}

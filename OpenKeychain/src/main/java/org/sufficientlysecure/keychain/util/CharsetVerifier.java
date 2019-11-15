/*
 * Copyright (C) 2017 Dominik Schürmann <dominik@dominikschuermann.de>
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


import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import android.content.ClipDescription;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/** This class can be used to guess whether a stream of data is encoded in a given
 * charset or not.
 *
 * An object of this class must be initialized with a byte[] buffer, which should
 * be filled with data, then processed with {@link #readBytesFromBuffer}. This can
 * be done any number of times. Once all data has been read, a final status can be
 * read using the getter methods.
 */
public class CharsetVerifier {

    private final ByteBuffer bufWrap;
    private final CharBuffer dummyOutput;

    private final CharsetDecoder charsetDecoder;

    private boolean isFinished;
    private boolean isFaulty;
    private boolean isGuessed;
    private boolean isPossibleTextMimeType;
    private boolean isTextMimeType;
    private String charset;
    private String mimeType;

    public CharsetVerifier(@NonNull  byte[] buf, @NonNull String mimeType, @Nullable String charset) {

        this.mimeType = mimeType;
        isTextMimeType = ClipDescription.compareMimeTypes(mimeType, "text/*");
        isPossibleTextMimeType = isTextMimeType
                || ClipDescription.compareMimeTypes(mimeType, "application/octet-stream")
                || ClipDescription.compareMimeTypes(mimeType, "application/x-download");
        if (!isPossibleTextMimeType) {
            charsetDecoder = null;
            bufWrap = null;
            dummyOutput = null;
            return;
        }

        bufWrap = ByteBuffer.wrap(buf);
        dummyOutput = CharBuffer.allocate(buf.length);

        // the charset defaults to us-ascii, but we want to default to utf-8
        if (charset == null || "us-ascii".equals(charset)) {
            charset = "utf-8";
            isGuessed = true;
        } else {
            isGuessed = false;
        }
        this.charset = charset;

        charsetDecoder = Charset.forName(charset).newDecoder();
        charsetDecoder.onMalformedInput(CodingErrorAction.REPORT);
        charsetDecoder.onUnmappableCharacter(CodingErrorAction.REPORT);
        charsetDecoder.reset();
    }

    public void readBytesFromBuffer(int pos, int len) {
        if (isFinished) {
            throw new IllegalStateException("cannot write again after reading charset status!");
        }
        if (isFaulty || bufWrap == null) {
            return;
        }
        bufWrap.rewind();
        bufWrap.position(pos);
        bufWrap.limit(len);
        dummyOutput.rewind();
        CoderResult result = charsetDecoder.decode(bufWrap, dummyOutput, false);
        if (result.isError()) {
            isFaulty = true;
        }
    }

    private void finishIfNecessary() {
        if (isFinished || isFaulty || bufWrap == null) {
            return;
        }
        isFinished = true;
        bufWrap.rewind();
        bufWrap.limit(0);
        dummyOutput.rewind();
        CoderResult result = charsetDecoder.decode(bufWrap, dummyOutput, true);
        if (result.isError()) {
            isFaulty = true;
        }
    }

    public String getGuessedMimeType() {
        if (isTextMimeType) {
            return mimeType;
        }
        if (isProbablyText()) {
            return "text/plain";
        }
        return mimeType;
    }

    public boolean isCharsetFaulty() {
        finishIfNecessary();
        return isFaulty;
    }

    public boolean isCharsetGuessed() {
        finishIfNecessary();
        return isGuessed;
    }

    public String getCharset() {
        finishIfNecessary();
        if (!isPossibleTextMimeType || (isGuessed && isFaulty)) {
            return null;
        }
        return charset;
    }

    public String getMaybeFaultyCharset() {
        return charset;
    }

    /** Returns true if the data which was read is definitely binary.
     *
     * This can happen when either the supplied mimeType indicated a non-ambiguous
     * binary data type, or if we guessed a charset but got errors while decoding.
     */
    public boolean isDefinitelyBinary() {
        finishIfNecessary();
        return !isTextMimeType && (!isPossibleTextMimeType || (isGuessed && isFaulty));
    }

    /** Returns true iff the data which was read is probably (or
     * definitely) text.
     *
     * The corner case where isDefinitelyBinary returns false but isProbablyText
     * returns true is where the charset was provided by the data (so is not
     * guessed) but is still faulty.
     */
    public boolean isProbablyText() {
        finishIfNecessary();
        return isTextMimeType || isPossibleTextMimeType && (!isGuessed || !isFaulty);
    }
}

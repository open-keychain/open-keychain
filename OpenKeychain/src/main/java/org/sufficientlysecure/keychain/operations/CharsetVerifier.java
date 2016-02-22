package org.sufficientlysecure.keychain.operations;


import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

import android.content.ClipDescription;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;


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

    public CharsetVerifier(@NonNull  byte[] buf, String mimeType, @Nullable String charset) {

        isPossibleTextMimeType = ClipDescription.compareMimeTypes(mimeType, "application/octet-stream")
                || ClipDescription.compareMimeTypes(mimeType, "application/x-download")
                || ClipDescription.compareMimeTypes(mimeType, "text/*");
        if (!isPossibleTextMimeType) {
            charsetDecoder = null;
            bufWrap = null;
            dummyOutput = null;
            return;
        }
        isTextMimeType = ClipDescription.compareMimeTypes(mimeType, "text/*");

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

    public void write(int pos, int len) {
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

    public boolean isDefinitelyBinary() {
        finishIfNecessary();
        return !isTextMimeType && (!isPossibleTextMimeType || (isGuessed && isFaulty));
    }

    public boolean isProbablyText() {
        return isTextMimeType || isPossibleTextMimeType && (!isGuessed || !isFaulty);
    }
}

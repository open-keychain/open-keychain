package org.sufficientlysecure.keychain.util;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.sufficientlysecure.keychain.KeychainTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(KeychainTestRunner.class)
public class CharsetVerifierTest {

    @Test
    public void testTypeImagePngAlwaysBinary() throws Exception {
        byte[] bytes = "bla bluh ☭".getBytes("utf-8");

        CharsetVerifier charsetVerifier = new CharsetVerifier(bytes, "image/png", null);
        charsetVerifier.readBytesFromBuffer(0, bytes.length);

        assertTrue("image/png should be marked as definitely binary", charsetVerifier.isDefinitelyBinary());
        assertFalse("image/png should never be marked as, even if it is", charsetVerifier.isProbablyText());
        assertNull("charset should be null", charsetVerifier.getCharset());
    }

    @Test
    public void testUtf8SpecifiedButFaulty() throws Exception {
        byte[] bytes = "bla bluh ☭".getBytes("utf-8");
        bytes[4] = (byte) 0xc3;
        bytes[5] = (byte) 0x28;

        CharsetVerifier charsetVerifier = new CharsetVerifier(bytes, "text/something", "utf-8");
        charsetVerifier.readBytesFromBuffer(0, bytes.length);

        assertFalse("text/plain should not be marked as binary, even if it is", charsetVerifier.isDefinitelyBinary());
        assertTrue("text/plain should be marked as text, even if it isn't valid", charsetVerifier.isProbablyText());
        assertTrue("encoding contained illegal chars, so it should be marked as faulty", charsetVerifier.isCharsetFaulty());
        assertFalse("charset was specified and should not be marked as guessed", charsetVerifier.isCharsetGuessed());
        assertEquals("mimetype should be preserved", "text/something", charsetVerifier.getGuessedMimeType());
        assertEquals("charset should be utf-8 since it was given explicitly", "utf-8", charsetVerifier.getCharset());
        assertEquals("charset should be utf-8 since it was given explicitly", "utf-8", charsetVerifier.getMaybeFaultyCharset());
    }

    @Test
    public void testUtf8GuessedAndFaulty() throws Exception {
        byte[] bytes = "bla bluh ☭".getBytes("utf-8");
        bytes[4] = (byte) 0xc3;
        bytes[5] = (byte) 0x28;

        CharsetVerifier charsetVerifier = new CharsetVerifier(bytes, "text/plain", null);
        charsetVerifier.readBytesFromBuffer(0, bytes.length);

        assertFalse("text/plain should not be marked as binary, even if it is", charsetVerifier.isDefinitelyBinary());
        assertTrue("text/plain should be marked as text, even if it isn't valid", charsetVerifier.isProbablyText());
        assertTrue("encoding contained illegal chars, so it should be marked as faulty", charsetVerifier.isCharsetFaulty());
        assertTrue("charset was guessed and should be marked as such", charsetVerifier.isCharsetGuessed());
        assertNull("charset should be null since the guess was faulty", charsetVerifier.getCharset());
        assertEquals("mimetype should be set to text", "text/plain", charsetVerifier.getGuessedMimeType());
        assertEquals("maybe-faulty charset should be utf-8", "utf-8", charsetVerifier.getMaybeFaultyCharset());
    }

    @Test
    public void testGuessedEncoding() throws Exception {
        byte[] bytes = "bla bluh ☭".getBytes("utf-8");

        CharsetVerifier charsetVerifier = new CharsetVerifier(bytes, "application/octet-stream", null);
        charsetVerifier.readBytesFromBuffer(0, bytes.length);

        assertFalse("application/octet-stream with text content is not definitely binary", charsetVerifier.isDefinitelyBinary());
        assertTrue("application/octet-stream with text content should be probably text", charsetVerifier.isProbablyText());
        assertFalse("detected charset should not be faulty", charsetVerifier.isCharsetFaulty());
        assertTrue("charset was guessed and should be marked as such", charsetVerifier.isCharsetGuessed());
        assertEquals("mimetype should be set to text", "text/plain", charsetVerifier.getGuessedMimeType());
        assertEquals("guessed charset is utf-8", "utf-8", charsetVerifier.getCharset());
    }

    @Test
    public void testWindows1252Faulty() throws Exception {
        byte[] bytes = "bla bluh  ☭".getBytes("windows-1252");
        bytes[2] = (byte) 0x9d;

        CharsetVerifier charsetVerifier = new CharsetVerifier(bytes, "text/plain", "windows-1252");
        charsetVerifier.readBytesFromBuffer(0, bytes.length);

        assertFalse("text/plain is never definitely binary", charsetVerifier.isDefinitelyBinary());
        assertTrue("text/plain is always probably text", charsetVerifier.isProbablyText());
        assertTrue("charset contained faulty characters", charsetVerifier.isCharsetFaulty());
        assertFalse("charset was not guessed", charsetVerifier.isCharsetGuessed());
        assertEquals("charset is returned correctly", "windows-1252", charsetVerifier.getCharset());
    }

    @Test
    public void testWindows1252Good() throws Exception {
        byte[] bytes = "bla bluh ☭".getBytes("windows-1252");
        // this is ‡ in windows-1252
        bytes[2] = (byte) 0x87;

        CharsetVerifier charsetVerifier = new CharsetVerifier(bytes, "text/plain", "windows-1252");
        charsetVerifier.readBytesFromBuffer(0, bytes.length);

        assertFalse("text/plain is never definitely binary", charsetVerifier.isDefinitelyBinary());
        assertTrue("text/plain is always probably text", charsetVerifier.isProbablyText());
        assertFalse("charset contained no faulty characters", charsetVerifier.isCharsetFaulty());
        assertFalse("charset was not guessed", charsetVerifier.isCharsetGuessed());
        assertEquals("charset is returned correctly", "windows-1252", charsetVerifier.getCharset());
    }

    @Test(expected = IllegalStateException.class)
    public void testReadAfterGetterShouldCrash() throws Exception {
        byte[] bytes = "bla bluh ☭".getBytes("utf-8");

        CharsetVerifier charsetVerifier = new CharsetVerifier(bytes, "text/plain", null);
        charsetVerifier.readBytesFromBuffer(0, bytes.length);
        charsetVerifier.isCharsetFaulty();

        charsetVerifier.readBytesFromBuffer(0, bytes.length);
    }


    @Test
    public void testStaggeredInput() throws Exception {
        byte[] bytes = "bla bluh ☭".getBytes("utf-8");
        bytes[4] = (byte) 0xc3;
        bytes[5] = (byte) 0x28;

        CharsetVerifier charsetVerifier = new CharsetVerifier(bytes, "text/plain", null);
        for (int i = 0; i < bytes.length; i++) {
            charsetVerifier.readBytesFromBuffer(i, i+1);
        }

        assertFalse("text/plain should not be marked as binary, even if it is", charsetVerifier.isDefinitelyBinary());
        assertTrue("text/plain should be marked as text, even if it isn't valid", charsetVerifier.isProbablyText());
        assertTrue("encoding contained illegal chars, so it should be marked as faulty", charsetVerifier.isCharsetFaulty());
        assertTrue("charset was guessed and should be marked as such", charsetVerifier.isCharsetGuessed());
        assertNull("charset should be null since the guess was faulty", charsetVerifier.getCharset());
        assertEquals("maybe-faulty charset should be utf-8", "utf-8", charsetVerifier.getMaybeFaultyCharset());
    }

}

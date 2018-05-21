package org.sufficientlysecure.keychain.util;

/**
 * Utilities for handling ZBase-32 encoding.
 *
 * @see <a href="https://tools.ietf.org/html/rfc6189#section-5.1.6">Z-Base32 encoding as used in RFC 6189</a>
 */
public final class ZBase32 {

    private static final char[] ALPHABET = "ybndrfg8ejkmcpqxot1uwisza345h769".toCharArray();
    private static final int SHIFT = Integer.numberOfTrailingZeros(ALPHABET.length);
    private static final int MASK = ALPHABET.length - 1;

    public static String encode(byte[] data) {
        if (data.length == 0) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        int buffer = data[0];
        int index = 1;
        int bitsLeft = 8;
        while (bitsLeft > 0 || index < data.length) {
            if (bitsLeft < SHIFT) {
                if (index < data.length) {
                    buffer <<= 8;
                    buffer |= (data[index++] & 0xff);
                    bitsLeft += 8;
                } else {
                    int pad = SHIFT - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }
            bitsLeft -= SHIFT;
            result.append(ALPHABET[MASK & (buffer >> bitsLeft)]);
        }
        return result.toString();
    }

    private ZBase32() {
    }

}

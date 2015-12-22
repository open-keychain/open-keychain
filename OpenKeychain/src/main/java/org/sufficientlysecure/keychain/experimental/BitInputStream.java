/*
 * Copyright (C) Andreas Jakl
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

package org.sufficientlysecure.keychain.experimental;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

/**
 * The BitInputStream allows reading individual bits from a
 * general Java InputStream.
 * Like the various Stream-classes from Java, the BitInputStream
 * has to be created based on another Input stream. It provides
 * a function to read the next bit from the stream, as well as to read multiple
 * bits at once and write the resulting data into an integer value.
 * <p/>
 * source: http://developer.nokia.com/Community/Wiki/Bit_Input/Output_Stream_utility_classes_for_efficient_data_transfer
 */
public class BitInputStream {
    /**
     * The Java InputStream this class is working on.
     */
    private InputStream iIs;

    /**
     * The buffer containing the currently processed
     * byte of the input stream.
     */
    private int iBuffer;

    /**
     * Next bit of the current byte value that the user will
     * get. If it's 8, the next bit will be read from the
     * next byte of the InputStream.
     */
    private int iNextBit = 8;

    /**
     * Create a new bit input stream based on an existing Java InputStream.
     *
     * @param aIs the input stream this class should read the bits from.
     */
    public BitInputStream(InputStream aIs) {
        iIs = aIs;
    }

    /**
     * Read a specified number of bits and return them combined as
     * an integer value. The bits are written to the integer
     * starting at the highest bit ( << aNumberOfBits ), going down
     * to the lowest bit ( << 0 )
     *
     * @param aNumberOfBits defines how many bits to read from the stream.
     * @return integer value containing the bits read from the stream.
     * @throws IOException
     */
    synchronized public int readBits(final int aNumberOfBits, boolean stuffIfEnd)
            throws IOException {
        int value = 0;
        for (int i = aNumberOfBits - 1; i >= 0; i--) {
            value |= (readBit(stuffIfEnd) << i);
        }
        return value;
    }

    synchronized public int available() {
        try {
            return (8 - iNextBit) + iIs.available() * 8; // bytestream to bitstream available
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Read the next bit from the stream.
     *
     * @return 0 if the bit is 0, 1 if the bit is 1.
     * @throws IOException
     */
    synchronized public int readBit(boolean stuffIfEnd) throws IOException {
        if (iIs == null)
            throw new IOException("Already closed");

        if (iNextBit == 8) {
            iBuffer = iIs.read();

            if (iBuffer == -1) {
                if (stuffIfEnd) {
                    return 1;
                } else {
                    throw new EOFException();
                }
            }

            iNextBit = 0;
        }

        int bit = iBuffer & (1 << iNextBit);
        iNextBit++;

        bit = (bit == 0) ? 0 : 1;

        return bit;
    }

    /**
     * Close the underlying input stream.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        iIs.close();
        iIs = null;
    }
}
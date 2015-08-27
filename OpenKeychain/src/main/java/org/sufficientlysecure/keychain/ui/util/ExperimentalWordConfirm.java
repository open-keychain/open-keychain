/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.util;

import android.content.Context;

import org.spongycastle.util.Arrays;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.BitSet;

public class ExperimentalWordConfirm {

    public static String getWords(Context context, byte[] fingerprintBlob) {
        ArrayList<String> words = new ArrayList<>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    context.getAssets().open("word_confirm_list.txt"),
                    "UTF-8"
            ));

            String line = reader.readLine();
            while (line != null) {
                words.add(line);

                line = reader.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException("IOException", e);
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }

        String fingerprint = "";

        // NOTE: 160 bit SHA-1 truncated to 156 bit
        byte[] fingerprintBlobTruncated = Arrays.copyOfRange(fingerprintBlob, 0, 156 / 8);

        // TODO: implement key stretching to minimize fp length?

        // BitSet bits = BitSet.valueOf(fingerprintBlob);  // min API 19 and little endian!
        BitSet bits = bitSetToByteArray(fingerprintBlobTruncated);
        Log.d(Constants.TAG, "bits: " + bits.toString());

        final int CHUNK_SIZE = 13;
        final int LAST_CHUNK_INDEX = fingerprintBlobTruncated.length * 8 / CHUNK_SIZE; // 12
        Log.d(Constants.TAG, "LAST_CHUNK_INDEX: " + LAST_CHUNK_INDEX);

        int from = 0;
        int to = CHUNK_SIZE;
        for (int i = 0; i < (LAST_CHUNK_INDEX + 1); i++) {
            Log.d(Constants.TAG, "from: " + from + " to: " + to);

            BitSet setIndex = bits.get(from, to);
            int wordIndex = (int) bitSetToLong(setIndex);
            // int wordIndex = (int) setIndex.toLongArray()[0]; // min API 19

            fingerprint += words.get(wordIndex);

            if (i != LAST_CHUNK_INDEX) {
                // line break every 3 words
                if (to % (CHUNK_SIZE * 3) == 0) {
                    fingerprint += "\n";
                } else {
                    fingerprint += " ";
                }
            }

            from = to;
            to += CHUNK_SIZE;
        }

        return fingerprint;
    }

    /**
     * Returns a BitSet containing the values in bytes.
     * BIG ENDIAN!
     */
    private static BitSet bitSetToByteArray(byte[] bytes) {
        int arrayLength = bytes.length * 8;
        BitSet bits = new BitSet();

        for (int i = 0; i < arrayLength; i++) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }

    private static long bitSetToLong(BitSet bits) {
        long value = 0L;
        for (int i = 0; i < bits.length(); ++i) {
            value += bits.get(i) ? (1L << i) : 0L;
        }
        return value;
    }
}

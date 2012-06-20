/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.helper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.Constants;

import android.util.Log;

public class PGPConversionHelper {
    /**
     * Converts Vector<PGPSecretKey> to a byte[] array to send it by intent to service
     * 
     * @param keys
     * @return
     */
    public static byte[] PGPSecretKeyListToBytes(Vector<PGPSecretKey> keys) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (PGPSecretKey key : keys) {
            try {
                key.encode(os);
            } catch (IOException e) {
                Log.e(Constants.TAG,
                        "Error while converting PGPSecretKey to byte[]: " + e.getMessage());
                e.printStackTrace();
            }
        }

        byte[] keysBytes = os.toByteArray();

        return keysBytes;
    }

    /**
     * Convert from byte[] to ArrayList<PGPSecretKey>
     * 
     * @param keysBytes
     * @return
     */
    public static PGPSecretKeyRing BytesToPGPSecretKeyRing(byte[] keysBytes) {
        PGPObjectFactory factory = new PGPObjectFactory(keysBytes);
        PGPSecretKeyRing keyRing = null;
        try {
            if ((keyRing = (PGPSecretKeyRing) factory.nextObject()) == null) {
                Log.e(Constants.TAG, "No keys given!");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return keyRing;
    }

    public static ArrayList<PGPSecretKey> BytesToPGPSecretKeyList(byte[] keysBytes) {
        PGPSecretKeyRing keyRing = BytesToPGPSecretKeyRing(keysBytes);
        ArrayList<PGPSecretKey> keys = new ArrayList<PGPSecretKey>();

        Iterator<PGPSecretKey> itr = keyRing.getSecretKeys();
        while (itr.hasNext()) {
            keys.add(itr.next());
        }

        return keys;
    }

    public static PGPSecretKey BytesToPGPSecretKey(byte[] keyBytes) {
        PGPSecretKey key = BytesToPGPSecretKeyList(keyBytes).get(0);

        return key;
    }

    public static byte[] PGPSecretKeyToBytes(PGPSecretKey key) {
        try {
            return key.getEncoded();
        } catch (IOException e) {
            Log.e(Constants.TAG, "Encoding failed: ", e);

            return null;
        }
    }

    public static byte[] PGPSecretKeyRingToBytes(PGPSecretKeyRing keyRing) {
        try {
            return keyRing.getEncoded();
        } catch (IOException e) {
            Log.e(Constants.TAG, "Encoding failed: ", e);

            return null;
        }
    }
}

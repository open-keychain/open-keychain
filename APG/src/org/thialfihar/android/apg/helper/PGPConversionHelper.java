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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPUtil;
import org.thialfihar.android.apg.Constants;

import org.thialfihar.android.apg.provider.ProviderHelper;
import org.thialfihar.android.apg.util.Log;

import android.content.Context;

public class PGPConversionHelper {

    /**
     * Convert from byte[] to PGPKeyRing
     * 
     * @param keysBytes
     * @return
     */
    public static PGPKeyRing BytesToPGPKeyRing(byte[] keysBytes) {
        PGPObjectFactory factory = new PGPObjectFactory(keysBytes);
        PGPKeyRing keyRing = null;
        try {
            if ((keyRing = (PGPKeyRing) factory.nextObject()) == null) {
                Log.e(Constants.TAG, "No keys given!");
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error while converting to PGPKeyRing!", e);
        }

        return keyRing;
    }

    /**
     * Convert from byte[] to ArrayList<PGPSecretKey>
     * 
     * @param keysBytes
     * @return
     */
    public static ArrayList<PGPSecretKey> BytesToPGPSecretKeyList(byte[] keysBytes) {
        PGPSecretKeyRing keyRing = (PGPSecretKeyRing) BytesToPGPKeyRing(keysBytes);
        ArrayList<PGPSecretKey> keys = new ArrayList<PGPSecretKey>();

        @SuppressWarnings("unchecked")
        Iterator<PGPSecretKey> itr = keyRing.getSecretKeys();
        while (itr.hasNext()) {
            keys.add(itr.next());
        }

        return keys;
    }

    /**
     * Convert from byte[] to PGPSecretKey
     * 
     * Singles keys are encoded as keyRings with one single key in it by Bouncy Castle
     * 
     * @param keysBytes
     * @return
     */
    public static PGPSecretKey BytesToPGPSecretKey(byte[] keyBytes) {
        PGPSecretKey key = BytesToPGPSecretKeyList(keyBytes).get(0);

        return key;
    }

    /**
     * Convert from ArrayList<PGPSecretKey> to byte[]
     * 
     * @param keys
     * @return
     */
    public static byte[] PGPSecretKeyArrayListToBytes(ArrayList<PGPSecretKey> keys) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        for (PGPSecretKey key : keys) {
            try {
                key.encode(os);
            } catch (IOException e) {
                Log.e(Constants.TAG, "Error while converting ArrayList<PGPSecretKey> to byte[]!", e);
            }
        }

        return os.toByteArray();
    }

    /**
     * Convert from PGPSecretKey to byte[]
     * 
     * @param keysBytes
     * @return
     */
    public static byte[] PGPSecretKeyToBytes(PGPSecretKey key) {
        try {
            return key.getEncoded();
        } catch (IOException e) {
            Log.e(Constants.TAG, "Encoding failed", e);

            return null;
        }
    }

    /**
     * Convert from PGPSecretKeyRing to byte[]
     * 
     * @param keysBytes
     * @return
     */
    public static byte[] PGPSecretKeyRingToBytes(PGPSecretKeyRing keyRing) {
        try {
            return keyRing.getEncoded();
        } catch (IOException e) {
            Log.e(Constants.TAG, "Encoding failed", e);

            return null;
        }
    }

    public static PGPKeyRing decodeKeyRing(InputStream is) throws IOException {
        InputStream in = PGPUtil.getDecoderStream(is);
        PGPObjectFactory objectFactory = new PGPObjectFactory(in);
        Object obj = objectFactory.nextObject();

        if (obj instanceof PGPKeyRing) {
            return (PGPKeyRing) obj;
        }

        return null;
    }

}

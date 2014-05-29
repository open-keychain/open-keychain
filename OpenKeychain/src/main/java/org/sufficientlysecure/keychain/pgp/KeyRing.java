/*
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.pgp;

import android.content.Context;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;

import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class KeyRing {
    private PGPSecretKeyRing mSecretKeyRing;
    private PGPPublicKeyRing mPublicKeyRing;

    public static KeyRing decode(byte[] data) {
        PGPObjectFactory factory = new PGPObjectFactory(data);
        KeyRing keyRing = null;
        try {
            Object obj = factory.nextObject();
            if (obj == null) {
                return null;
            } else if (obj instanceof PGPPublicKeyRing) {
                keyRing = new PublicKeyRing((PGPPublicKeyRing) obj);
            } else if (obj instanceof PGPSecretKeyRing) {
                keyRing = new SecretKeyRing((PGPSecretKeyRing) obj);
            }
        } catch (IOException e) {
            return null;
        }

        return keyRing;
    }

    public KeyRing(PGPKeyRing keyRing) {
        if (keyRing instanceof PGPPublicKeyRing) {
            mPublicKeyRing = (PGPPublicKeyRing) keyRing;
        } else {
            mSecretKeyRing = (PGPSecretKeyRing) keyRing;
        }
    }

    public KeyRing(PGPPublicKeyRing publicKeyRing) {
        mPublicKeyRing = publicKeyRing;
    }

    public KeyRing(PGPSecretKeyRing secretKeyRing) {
        mSecretKeyRing = secretKeyRing;
    }

    public boolean isPublic() {
        if (mPublicKeyRing != null) {
            return true;
        }
        return false;
    }

    public PGPPublicKeyRing getPublicKeyRing() {
        return mPublicKeyRing;
    }

    public PGPSecretKeyRing getSecretKeyRing() {
        return mSecretKeyRing;
    }

    public Key getSecretKey(long keyId) {
        if (isPublic()) {
            return null;
        }
        return new Key(mSecretKeyRing.getSecretKey(keyId));
    }

    public Key getPublicKey(long keyId) {
        return new Key(mPublicKeyRing.getPublicKey(keyId));
    }

    public byte[] getEncoded() throws IOException {
        if (isPublic()) {
            return mPublicKeyRing.getEncoded();
        }
        return mSecretKeyRing.getEncoded();
    }

    public String getArmoredEncoded(Context context) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ArmoredOutputStream aos = new ArmoredOutputStream(bos);
        aos.setHeader("Version", PgpHelper.getFullVersion(context));
        aos.write(getEncoded());
        aos.close();

        return bos.toString("UTF-8");
    }

    public ArrayList<Key> getPublicKeys() {
        ArrayList<Key> keys = new ArrayList<Key>();
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(mPublicKeyRing.getPublicKeys())) {
            keys.add(new Key(key));
        }
        return keys;
    }

    public ArrayList<Key> getSecretKeys() {
        ArrayList<Key> keys = new ArrayList<Key>();
        if (isPublic()) {
            return keys;
        }
        for (PGPSecretKey key : new IterableIterator<PGPSecretKey>(mSecretKeyRing.getSecretKeys())) {
            keys.add(new Key(key));
        }
        return keys;
    }

    public Key getMasterKey() {
        if (isPublic()) {
            for (Key key : getPublicKeys()) {
                if (key.isMasterKey()) {
                    return key;
                }
            }

            return null;
        } else {
            for (Key key : getSecretKeys()) {
                if (key.isMasterKey()) {
                    return key;
                }
            }

            return null;
        }
    }

    public ArrayList<Key> getEncryptKeys() {
        ArrayList<Key> encryptKeys = new ArrayList<Key>();
        for (Key key : getPublicKeys()) {
            if (key.isEncryptionKey()) {
                encryptKeys.add(key);
            }
        }

        return encryptKeys;
    }

    public ArrayList<Key> getSigningKeys() {
        ArrayList<Key> signingKeys = new ArrayList<Key>();
        for (Key key : getSecretKeys()) {
            if (key.isSigningKey()) {
                signingKeys.add(key);
            }
        }

        return signingKeys;
    }

    public ArrayList<Key> getUsableEncryptKeys() {
        ArrayList<Key> usableKeys = new ArrayList<Key>();
        ArrayList<Key> encryptKeys = getEncryptKeys();
        Key masterKey = null;
        for (int i = 0; i < encryptKeys.size(); ++i) {
            Key key = encryptKeys.get(i);
            if (!key.isExpired()) {
                if (key.isMasterKey()) {
                    masterKey = key;
                } else {
                    usableKeys.add(key);
                }
            }
        }
        if (masterKey != null) {
            usableKeys.add(masterKey);
        }
        return usableKeys;
    }

    public ArrayList<Key> getUsableSigningKeys() {
        ArrayList<Key> usableKeys = new ArrayList<Key>();
        ArrayList<Key> signingKeys = getSigningKeys();
        Key masterKey = null;
        for (int i = 0; i < signingKeys.size(); ++i) {
            Key key = signingKeys.get(i);
            if (key.isMasterKey()) {
                masterKey = key;
            } else {
                usableKeys.add(key);
            }
        }
        if (masterKey != null) {
            usableKeys.add(masterKey);
        }
        return usableKeys;
    }


    public Key getSigningKey() {
        for (Key key : getUsableSigningKeys()) {
            return key;
        }
        return null;
    }

    public Key getEncryptKey() {
        for (Key key : getUsableEncryptKeys()) {
            return key;
        }
        return null;
    }
}

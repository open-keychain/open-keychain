/*
 * Copyright (C) 2017 Jakob Bode
 * Copyright (C) 2017 Matthias Sekul
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

//Have a look at https://github.com/MrJabo/SecureDataSocket

package org.sufficientlysecure.keychain.network.secureDataSocket;

import java.lang.IllegalStateException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.InvalidAlgorithmParameterException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.AEADBadTagException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.GCMParameterSpec;


class CryptoObject {
    private SecretKeySpec mSharedSecret = null;
    private Cipher mEnc = null;
    private Cipher mDec = null;
    private boolean mHasSymmetricKey = false;
    private SecureRandom mRandom = null;
    private int mIvSize = 16;
    private int mTagSize = 32;

    /**
     * Constructor.
     * Create a new CryptoObject with encryption asymmetric elliptic curve25519 encryption keypair
     * Short authentication byte length is 3 byte.
     */
    CryptoObject() throws CryptoSocketException {
        this(3, 16, 16);
    }

    /**
     * Constructor.
     * Create a new CryptoObject with encryption asymmetric elliptic curve (curve25519, ECDH) encryption keypair
     * and digital sign asymmetric elliptic curve keypair.
     * shortAuthenticationStringSize must be a positive number, that represents the short
     * authentication byte length.
     * ivSize must be positiv, byte size of iv for encryption scheme
     * tagSize must be positiv, byte size of tag for encryption scheme
     */
    private CryptoObject(int shortAuthenticationStringSize, int ivSize, int tagSize)
            throws CryptoSocketException {
        if (0 >= shortAuthenticationStringSize || 0 >= ivSize || 0 >= tagSize) {
            throw new CryptoSocketException("shortAuthenticationStringSize,ivSize and tagSize " +
                    "must be a positive number!");
        }

        this.mIvSize = ivSize;
        this.mTagSize = tagSize;
    }


    /**
     * Get encryption offset (ivSize + tagSize)
     */
    public int getOffset() {
        return this.mIvSize + this.mTagSize;
    }


    /**
     * set a already known sharedsecret, instead of using commitment to create one
     */
    void setSharedSecret(byte[] sharedSecret) throws CryptoSocketException {
        if (sharedSecret.length != 32) {
            throw new CryptoSocketException("invalid sharedSecret-size; has to have a length of " +
                    "32 bytes");
        }
        try {
            byte[] byteSharedSecret = new byte[sharedSecret.length / 2];
            System.arraycopy(sharedSecret, 0, byteSharedSecret, 0, byteSharedSecret.length);
            this.mSharedSecret = new SecretKeySpec(byteSharedSecret, "AES");
            this.mHasSymmetricKey = true;
            this.mEnc = Cipher.getInstance("AES/GCM/NoPadding");
            this.mDec = Cipher.getInstance("AES/GCM/NoPadding");
        } catch (IllegalStateException is) {
            throw new CryptoSocketException("unable to create shared encryption key, wrong state!");
        } catch (NoSuchAlgorithmException nsa) {
            throw new CryptoSocketException("Encryption algorithm not found!");
        } catch (NoSuchPaddingException nsp) {
            throw new CryptoSocketException("Invalid padding algorithm!");
        }

    }


    /**
     * Encrypt data with AES-GCM mode.
     * Encrypt data array and returns ciphertext.
     */

    public byte[] encrypt(byte[] data) throws IllegalStateException, CryptoSocketException,
            IllegalBlockSizeException {
        if (!this.mHasSymmetricKey) {
            throw new IllegalStateException("Have no symmetric key, you need to create a shared " +
                    "secret first!");
        }

        if (data == null) {
            throw new CryptoSocketException("No data found for encryption");
        }

        byte[] iv = new byte[this.mIvSize];
        byte[] output;
        byte[] encryptData;
        this.mRandom.nextBytes(iv);
        try {
            this.mEnc.init(Cipher.ENCRYPT_MODE, this.mSharedSecret,
                    new GCMParameterSpec(this.mTagSize * 8, iv));
            encryptData = this.mEnc.doFinal(data);
            output = new byte[this.mIvSize + encryptData.length];
            System.arraycopy(iv, 0, output, 0, this.mIvSize);
            System.arraycopy(encryptData, 0, output, this.mIvSize, encryptData.length);
        } catch (AEADBadTagException abt) {
            throw new CryptoSocketException("Decryption exception? Impossible!");
        } catch (BadPaddingException bp) {
            throw new CryptoSocketException("Padding exception? Impossible!");
        } catch (IllegalStateException ise) {
            throw new IllegalStateException("Wrong AES cipher use!");
        } catch (InvalidKeyException ik) {
            ik.printStackTrace();
            throw new CryptoSocketException("Invalid key for encryption!");
        } catch (InvalidAlgorithmParameterException iap) {
            iap.printStackTrace();
            throw new CryptoSocketException("Encryption parameters are wrong!");
        }

        return output;
    }

    /**
     * Decrypt data with AES-GCM mode.
     * Decrypt data array and return message, if tag was valid, otherwise null.
     */

    public byte[] decrypt(byte[] data) throws IllegalStateException, CryptoSocketException,
            IllegalBlockSizeException {
        if (!this.mHasSymmetricKey) {
            throw new IllegalStateException("Have no symmetric key, you need to create a shared " +
                    "secret first!");
        }

        //data can be null. So the check is necessary!
        if (data.length <= this.mIvSize + this.mTagSize || data == null) {
            throw new CryptoSocketException("The data are too small for a ciphertext!");
        }

        byte[] iv = new byte[this.mIvSize];
        byte[] ciphertext = new byte[data.length - this.mIvSize];
        System.arraycopy(data, 0, iv, 0, this.mIvSize);
        System.arraycopy(data, this.mIvSize, ciphertext, 0, ciphertext.length);
        byte[] decryptData;
        try {
            this.mDec.init(Cipher.DECRYPT_MODE, this.mSharedSecret,
                    new GCMParameterSpec(this.mTagSize * 8, iv));
            decryptData = this.mDec.doFinal(ciphertext);
        } catch (AEADBadTagException abt) {
            return null;
        } catch (BadPaddingException bp) {
            throw new CryptoSocketException("Padding exception? Impossible!");
        } catch (IllegalStateException ibs) {
            throw new IllegalStateException("Wrong AES cipher use!");
        } catch (InvalidKeyException ik) {
            throw new CryptoSocketException("Invalid key for decryption!");
        } catch (InvalidAlgorithmParameterException iap) {
            throw new CryptoSocketException("Decryption parameters are wrong!");
        }

        return decryptData;
    }
}

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

import org.spongycastle.bcpg.BCPGInputStream;
import org.spongycastle.bcpg.PacketTags;
import org.spongycastle.bcpg.PublicKeyPacket;
import org.spongycastle.bcpg.SecretKeyPacket;
import org.spongycastle.bcpg.SecretSubkeyPacket;
import org.spongycastle.bcpg.TrustPacket;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPrivateKey;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.spongycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.spongycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Vector;

public class Key implements Serializable {
    public static final int ELGAMAL_ENCRYPT = PGPPublicKey.ELGAMAL_ENCRYPT;
    public static final int DSA = PGPPublicKey.DSA;

    private transient PGPSecretKey mSecretKey;
    private transient PGPPublicKey mPublicKey;

    public static Key decode(byte[] data) {
        PGPObjectFactory factory = new PGPObjectFactory(data);
        Object obj = null;

        try {
            obj = factory.nextObject();
        } catch (IOException e) {
            Log.e(Constants.TAG, "error while decoding Key/KeyRing", e);
            return null;
        }

        Log.v(Constants.TAG, "class: " + obj.getClass().getName());

        if (obj instanceof PGPSecretKey) {
            return new Key((PGPSecretKey) obj);
        } else if (obj instanceof PGPPublicKey) {
            return new Key((PGPPublicKey) obj);
        } else if (obj instanceof PGPSecretKeyRing) {
            KeyRing keyRing = new KeyRing((PGPSecretKeyRing) obj);
            return keyRing.getMasterKey();
        } else if (obj instanceof PGPPublicKeyRing) {
            KeyRing keyRing = new KeyRing((PGPPublicKeyRing) obj);
            return keyRing.getMasterKey();
        }

        return null;
    }

    public Key(PGPPublicKey publicKey) {
        mPublicKey = publicKey;
    }

    public Key(PGPSecretKey secretKey) {
        mSecretKey = secretKey;
        mPublicKey = mSecretKey.getPublicKey();
    }

    public PGPPublicKey getPublicKey() {
        return mPublicKey;
    }

    public PGPSecretKey getSecretKey() {
        return mSecretKey;
    }

    public boolean isPublic() {
        if (mSecretKey == null) {
            return true;
        }
        return false;
    }

    public boolean isMasterKey() {
        if (mSecretKey != null) {
            return mSecretKey.isMasterKey();
        }
        return mPublicKey.isMasterKey();
    }

    public long getKeyId() {
       return mPublicKey.getKeyID();
    }

    public Date getCreationDate() {
        return mPublicKey.getCreationTime();
    }

    public Date getExpiryDate() {
        Date creationDate = getCreationDate();
        if (mPublicKey.getValidDays() == 0) {
            // no expiry
            return null;
        }
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.DATE, mPublicKey.getValidDays());
        Date expiryDate = calendar.getTime();

        return expiryDate;
    }

    public boolean isExpired() {
        Date creationDate = getCreationDate();
        Date expiryDate = getExpiryDate();
        Date now = new Date();
        if (now.compareTo(creationDate) >= 0 &&
            (expiryDate == null || now.compareTo(expiryDate) <= 0)) {
            return false;
        }
        return true;
    }

    public boolean isRevoked() {
        return mPublicKey.isRevoked();
    }

    public Vector<PGPSignature> getSignatures() {
        Vector<PGPSignature> signatures = new Vector<PGPSignature>();
        for (PGPSignature signature : new IterableIterator<PGPSignature>(mPublicKey.getSignatures())) {
            signatures.add(signature);
        }
        return signatures;
    }

    public IterableIterator<String> getUserIds() {
        return new IterableIterator<String>(mPublicKey.getUserIDs());
    }

    public String getMainUserId() {
        for (String userId : getUserIds()) {
            return userId;
        }
        return null;
    }

    public boolean isEncryptionKey() {
        if (!mPublicKey.isEncryptionKey()) {
            return false;
        }

        if (mPublicKey.getVersion() <= 3) {
            return true;
        }

        // special cases
        if (mPublicKey.getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT) {
            return true;
        }

        if (mPublicKey.getAlgorithm() == PGPPublicKey.RSA_ENCRYPT) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignatures())) {
            if (mPublicKey.isMasterKey() && sig.getKeyID() != mPublicKey.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null && (hashed.getKeyFlags() &
                                   (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() &
                                     (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                return true;
            }
        }
        return false;
    }

    public boolean isSigningKey() {
        if (isPublic()) {
            return false;
        }

        if (mSecretKey.isPrivateKeyEmpty()) {
            return false;
        }

        if (mPublicKey.getVersion() <= 3) {
            return true;
        }

        // special case
        if (mPublicKey.getAlgorithm() == PGPPublicKey.RSA_SIGN) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignatures())) {
            if (mPublicKey.isMasterKey() && sig.getKeyID() != mPublicKey.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null && (hashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() & KeyFlags.SIGN_DATA) != 0) {
                return true;
            }
        }

        return false;
    }

    public boolean isCertificationKey() {
        if (mSecretKey.isPrivateKeyEmpty()) {
            return false;
        }

        if (mPublicKey.getVersion() <= 3) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignatures())) {
            if (mPublicKey.isMasterKey() && sig.getKeyID() != mPublicKey.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null && (hashed.getKeyFlags() & KeyFlags.CERTIFY_OTHER) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() & KeyFlags.CERTIFY_OTHER) != 0) {
                return true;
            }
        }

        return false;
    }

    public boolean isAuthenticationKey() {
        if (mPublicKey.getVersion() <= 3) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignatures())) {
            if (mPublicKey.isMasterKey() && sig.getKeyID() != mPublicKey.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null && (hashed.getKeyFlags() & KeyFlags.AUTHENTICATION) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null && (unhashed.getKeyFlags() & KeyFlags.AUTHENTICATION) != 0) {
                return true;
            }
        }

        return false;
    }

    public int getKeyUsage() {
        if (mPublicKey.getVersion() <= 3) {
            return 0;
        }

        int usage = 0;
        for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignatures())) {
            if (mPublicKey.isMasterKey() && sig.getKeyID() != mPublicKey.getKeyID()) {
                continue;
            }

            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();
            if (hashed != null) {
                usage |= hashed.getKeyFlags();
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();
            if (unhashed != null) {
                usage |= unhashed.getKeyFlags();
            }
        }
        return usage;
    }

    public int getAlgorithm() {
        return mPublicKey.getAlgorithm();
    }

    public int getBitStrength() {
        return mPublicKey.getBitStrength();
    }

    public String getAlgorithmInfo() {
        int algorithm = getAlgorithm();
        int keySize = getBitStrength();
        return Key.getAlgorithmInfo(algorithm, keySize);
    }

    public static String getAlgorithmInfo(int algorithm, int keySize) {
        String algorithmStr = null;

        switch (algorithm) {
            case PGPPublicKey.RSA_ENCRYPT:
            case PGPPublicKey.RSA_GENERAL:
            case PGPPublicKey.RSA_SIGN: {
                algorithmStr = "RSA";
                break;
            }

            case PGPPublicKey.DSA: {
                algorithmStr = "DSA";
                break;
            }

            case PGPPublicKey.ELGAMAL_ENCRYPT:
            case PGPPublicKey.ELGAMAL_GENERAL: {
                algorithmStr = "ElGamal";
                break;
            }

            default: {
                algorithmStr = "???";
                break;
            }
        }
        return algorithmStr + ", " + keySize + "bit";
    }

    public byte[] getFingerprint() {
        return mPublicKey.getFingerprint();
    }

    /*public String getFingerprint() {
        String fingerprint = "";
        byte fp[] = mPublicKey.getFingerprint();
        for (int i = 0; i < fp.length; ++i) {
            if (i != 0 && i % 10 == 0) {
                fingerprint += "  ";
            } else if (i != 0 && i % 2 == 0) {
                fingerprint += " ";
            }
            String chunk = Integer.toHexString((fp[i] + 256) % 256).toUpperCase();
            while (chunk.length() < 2) {
                chunk = "0" + chunk;
            }
            fingerprint += chunk;
        }

        return fingerprint;
    }*/

    public byte[] getEncoded() throws IOException {
        if (isPublic()) {
            return mPublicKey.getEncoded();
        } else {
            return mSecretKey.getEncoded();
        }
    }

    public PGPPrivateKey extractPrivateKey(String passphrase) throws PGPException {
        if (isPublic()) {
            return null;
        }
        PBESecretKeyDecryptor keyDecryptor =
            new JcePBESecretKeyDecryptorBuilder().setProvider(
                BouncyCastleProvider.PROVIDER_NAME).build(passphrase.toCharArray());
        return extractPrivateKey(keyDecryptor);
    }

    public PGPPrivateKey extractPrivateKey(PBESecretKeyDecryptor keyDecryptor) throws PGPException {
        if (isPublic()) {
            return null;
        }
        return mSecretKey.extractPrivateKey(keyDecryptor);
    }

    private void writeObject(ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(getEncoded());
    }

    // this method requires some changes in spongycastle, as it uses methods and constructors
    // that aren't public
    /*private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        byte[] data = (byte[]) in.readObject();
        mPublicKey = null;
        mSecretKey = null;
        BCPGInputStream pIn = new BCPGInputStream(new ByteArrayInputStream(data));

        int initialTag = pIn.nextPacketTag();
        if (initialTag != PacketTags.PUBLIC_KEY &&
            initialTag != PacketTags.PUBLIC_SUBKEY &&
            initialTag != PacketTags.SECRET_KEY &&
            initialTag != PacketTags.SECRET_SUBKEY) {
            throw new IOException("could not decode Key: tag " + initialTag);
        }

        BcKeyFingerprintCalculator fingerPrintCalculator = new BcKeyFingerprintCalculator();

        switch (initialTag) {
        case PacketTags.PUBLIC_KEY: {
            PublicKeyPacket pubPk = (PublicKeyPacket) pIn.readPacket();
            TrustPacket trustPk = PGPKeyRing.readOptionalTrustPacket(pIn);

            // direct signatures and revocations
            List keySigs = PGPKeyRing.readSignaturesAndTrust(pIn);

            List ids = new ArrayList();
            List idTrusts = new ArrayList();
            List idSigs = new ArrayList();
            PGPKeyRing.readUserIDs(pIn, ids, idTrusts, idSigs);

            try {
                mPublicKey = new PGPPublicKey(pubPk, trustPk, keySigs, ids, idTrusts, idSigs,
                                              fingerPrintCalculator);
            } catch (PGPException e) {
                throw new IOException("processing exception: " + e.toString());
            }
            break;
        }

        case PacketTags.PUBLIC_SUBKEY: {
            PublicKeyPacket pk = (PublicKeyPacket) pIn.readPacket();
            TrustPacket kTrust = PGPKeyRing.readOptionalTrustPacket(pIn);

            // PGP 8 actually leaves out the signature.
            List sigList = PGPKeyRing.readSignaturesAndTrust(pIn);

            try {
                mPublicKey = new PGPPublicKey(pk, kTrust, sigList, fingerPrintCalculator);
            } catch (PGPException e) {
                throw new IOException("processing exception: " + e.toString());
            }
            break;
        }

        case PacketTags.SECRET_KEY: {
            SecretKeyPacket secret = (SecretKeyPacket) pIn.readPacket();

            // ignore GPG comment packets if found.
            while (pIn.nextPacketTag() == PacketTags.EXPERIMENTAL_2) {
                pIn.readPacket();
            }

            TrustPacket trust = PGPKeyRing.readOptionalTrustPacket(pIn);

            // revocation and direct signatures
            List keySigs = PGPKeyRing.readSignaturesAndTrust(pIn);

            List ids = new ArrayList();
            List idTrusts = new ArrayList();
            List idSigs = new ArrayList();
            PGPKeyRing.readUserIDs(pIn, ids, idTrusts, idSigs);

            try {
                mSecretKey = new PGPSecretKey(secret, new PGPPublicKey(secret.getPublicKeyPacket(),
                                trust, keySigs, ids, idTrusts, idSigs, fingerPrintCalculator));
            } catch (PGPException e) {
                throw new IOException("processing exception: " + e.toString());
            }
            break;
        }

        case PacketTags.SECRET_SUBKEY: {
            SecretSubkeyPacket sub = (SecretSubkeyPacket) pIn.readPacket();

            // ignore GPG comment packets if found.
            while (pIn.nextPacketTag() == PacketTags.EXPERIMENTAL_2) {
                pIn.readPacket();
            }

            TrustPacket subTrust = PGPKeyRing.readOptionalTrustPacket(pIn);
            List sigList = PGPKeyRing.readSignaturesAndTrust(pIn);

            try {
                mSecretKey = new PGPSecretKey(sub, new PGPPublicKey(sub.getPublicKeyPacket(), subTrust,
                                                sigList, fingerPrintCalculator));
            } catch (PGPException e) {
                throw new IOException("processing exception: " + e.toString());
            }
            break;
        }
        }

        if (mSecretKey != null) {
            mPublicKey = mSecretKey.getPublicKey();
        }
    }*/
}

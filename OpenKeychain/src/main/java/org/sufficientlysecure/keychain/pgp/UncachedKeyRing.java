/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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


import android.text.TextUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeSet;

import android.support.annotation.VisibleForTesting;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SignatureSubpacketTags;
import org.bouncycastle.bcpg.UserAttributeSubpacketTags;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.openpgp.PGPKeyRing;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureList;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUserAttributeSubpacketVector;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.Utf8Util;

/** Wrapper around PGPKeyRing class, to be constructed from bytes.
 *
 * This class and its relatives UncachedPublicKey and UncachedSecretKey are
 * used to move around pgp key rings in non crypto related (UI, mostly) code.
 * It should be used for simple inspection only until it saved in the database,
 * all actual crypto operations should work with CanonicalizedKeyRings
 * exclusively.
 *
 * This class is also special in that it can hold either the PGPPublicKeyRing
 * or PGPSecretKeyRing derivate of the PGPKeyRing class, since these are
 * treated equally for most purposes in UI code. It is up to the programmer to
 * take care of the differences.
 *
 * @see CanonicalizedKeyRing
 * @see org.sufficientlysecure.keychain.pgp.UncachedPublicKey
 * @see org.sufficientlysecure.keychain.pgp.UncachedSecretKey
 *
 */
public class UncachedKeyRing {

    final PGPKeyRing mRing;
    final boolean mIsSecret;

    private static final int CANONICALIZE_MAX_USER_IDS = 100;

    UncachedKeyRing(PGPKeyRing ring) {
        mRing = ring;
        mIsSecret = ring instanceof PGPSecretKeyRing;
    }

    public long getMasterKeyId() {
        return mRing.getPublicKey().getKeyID();
    }

    public UncachedPublicKey getPublicKey() {
        return new UncachedPublicKey(mRing.getPublicKey());
    }

    public UncachedPublicKey getPublicKey(long keyId) {
        return new UncachedPublicKey(mRing.getPublicKey(keyId));
    }

    public Iterator<UncachedPublicKey> getPublicKeys() {
        final Iterator<PGPPublicKey> it = mRing.getPublicKeys();
        return new Iterator<UncachedPublicKey>() {
            public void remove() {
                throw new UnsupportedOperationException();
            }
            public UncachedPublicKey next() {
                return new UncachedPublicKey(it.next());
            }
            public boolean hasNext() {
                return it.hasNext();
            }
        };
    }

    /** Returns the dynamic (though final) property if this is a secret keyring or not. */
    public boolean isSecret() {
        return mIsSecret;
    }

    public byte[] getEncoded() throws IOException {
        return mRing.getEncoded();
    }

    public void encode(OutputStream out) throws IOException {
        mRing.encode(out);
    }

    public byte[] getFingerprint() {
        return mRing.getPublicKey().getFingerprint();
    }

    public int getVersion() {
        return mRing.getPublicKey().getVersion();
    }

    public static UncachedKeyRing decodeFromData(byte[] data)
            throws PgpGeneralException, IOException {

        IteratorWithIOThrow<UncachedKeyRing> parsed = fromStream(new ByteArrayInputStream(data));

        if ( ! parsed.hasNext()) {
            throw new PgpGeneralException("Object not recognized as PGPKeyRing!");
        }

        UncachedKeyRing ring = parsed.next();

        if (parsed.hasNext()) {
            throw new PgpGeneralException("Expected single keyring in stream, found at least two");
        }

        return ring;

    }

    public static UncachedKeyRing decodeFromData(
            byte[] data, String expectedFingerprint, String expectedKeyId)
            throws PgpGeneralException, IOException {

        IteratorWithIOThrow<UncachedKeyRing> parsed = fromStream(new ByteArrayInputStream(data));

        if ( ! parsed.hasNext()) {
            throw new PgpGeneralException("Object not recognized as PGPKeyRing!");
        }

        UncachedKeyRing ringToAccept = null;

        while (parsed.hasNext()) {
            UncachedKeyRing ring = parsed.next();
            Iterator<UncachedPublicKey> publicKeys = ring.getPublicKeys();

            while (publicKeys.hasNext()) {
                UncachedPublicKey publicKey = publicKeys.next();

                boolean publicKeyMatches = (
                        KeyFormattingUtils.convertFingerprintToHex(publicKey.getFingerprint())
                                .equalsIgnoreCase(expectedFingerprint)
                        || TextUtils.equals(Long.toString(publicKey.getKeyId()), expectedKeyId));
                if (publicKeyMatches) {
                    if (publicKey.isMasterKey()) {
                        //master key matches expectedFingerprint, return this ring immediately
                        return ring;
                    } else if (ringToAccept == null) {
                        //one of the subkeys matches expectedFingerprint
                        //this ring is potentially the desired ring
                        ringToAccept = ring;
                    }
                }
            }
        }

        if (ringToAccept == null) {
            throw new PgpGeneralException("A key with the expected fingerprint/key ID was not found!");
        } else {
            return ringToAccept;
        }

    }

    public static IteratorWithIOThrow<UncachedKeyRing> fromStream(final InputStream stream) {

        return new IteratorWithIOThrow<UncachedKeyRing>() {

            UncachedKeyRing mNext = null;
            PGPObjectFactory mObjectFactory = null;

            private void cacheNext() throws IOException {
                if (mNext != null) {
                    return;
                }

                try {
                    while (stream.available() > 0) {
                        // if there are no objects left from the last factory, create a new one
                        if (mObjectFactory == null) {
                            InputStream in = PGPUtil.getDecoderStream(stream);
                            mObjectFactory = new PGPObjectFactory(in, new JcaKeyFingerprintCalculator());
                        }

                        // go through all objects in this block
                        Object obj;
                        while ((obj = mObjectFactory.nextObject()) != null) {
                            Log.d(Constants.TAG, "Found class: " + obj.getClass());
                            if (!(obj instanceof PGPKeyRing)) {
                                Log.i(Constants.TAG,
                                        "Skipping object of bad type " + obj.getClass().getName() + " in stream");
                                // skip object
                                continue;
                            }
                            mNext = new UncachedKeyRing((PGPKeyRing) obj);
                            return;
                        }
                        // if we are past the while loop, that means the objectFactory had no next
                        mObjectFactory = null;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new IOException(e);
                }
            }

            @Override
            public boolean hasNext() throws IOException {
                cacheNext();
                return mNext != null;
            }

            @Override
            public UncachedKeyRing next() throws IOException {
                try {
                    cacheNext();
                    return mNext;
                } finally {
                    mNext = null;
                }
            }
        };

    }

    public interface IteratorWithIOThrow<E> {
        public boolean hasNext() throws IOException;
        public E next() throws IOException;
    }
    public void encodeArmored(OutputStream out, String version) throws IOException {
        ArmoredOutputStream aos = new ArmoredOutputStream(out);
        if (version != null) {
            aos.setHeader("Version", version);
        }
        aos.write(mRing.getEncoded());
        aos.close();
    }

    // An array of known algorithms. Note this must be numerically sorted for binarySearch() to work!
    static final int[] KNOWN_ALGORITHMS = new int[] {
        PublicKeyAlgorithmTags.RSA_GENERAL, // 1
        PublicKeyAlgorithmTags.RSA_ENCRYPT, // 2
        PublicKeyAlgorithmTags.RSA_SIGN, // 3
        PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT, // 16
        PublicKeyAlgorithmTags.DSA, // 17
        PublicKeyAlgorithmTags.ECDH, // 18
        PublicKeyAlgorithmTags.ECDSA, // 19
        PublicKeyAlgorithmTags.ELGAMAL_GENERAL, // 20
        // PublicKeyAlgorithmTags.DIFFIE_HELLMAN, // 21
    };

    /** "Canonicalizes" a public key, removing inconsistencies in the process.
     *
     * More specifically:
     *  - Remove all non-verifying self-certificates
     *  - Remove all "future" self-certificates
     *  - Remove all certificates flagged as "local"
     *  - For UID certificates, remove all certificates which are
     *      superseded by a newer one on the same target, including
     *      revocations with later re-certifications.
     *  - For subkey certifications, remove all certificates which
     *      are superseded by a newer one on the same target, unless
     *      it encounters a revocation certificate. The revocation
     *      certificate is considered to permanently revoke the key,
     *      even if contains later re-certifications.
     *  This is the "behavior in practice" used by (e.g.) GnuPG, and
     *  the rationale for both can be found as comments in the GnuPG
     *  source.
     *  UID signatures:
     *  https://github.com/mtigas/gnupg/blob/50c98c7ed6b542857ee2f902eca36cda37407737/g10/getkey.c#L1668-L1674
     *  Subkey signatures:
     *  https://github.com/mtigas/gnupg/blob/50c98c7ed6b542857ee2f902eca36cda37407737/g10/getkey.c#L1990-L1997
     *  - Remove all certificates in other positions if not of known type:
     *   - key revocation signatures on the master key
     *   - subkey binding signatures for subkeys
     *   - certifications and certification revocations for user ids
     *  - If a subkey retains no valid subkey binding certificate, remove it
     *  - If a user id retains no valid self certificate, remove it
     *  - If the key is a secret key, remove all certificates by foreign keys
     *  - If no valid user id remains, log an error and return null
     *
     * This operation writes an OperationLog which can be used as part of an OperationResultParcel.
     *
     * @return A canonicalized key, or null on fatal error (log will include a message in this case)
     *
     */
    @SuppressWarnings("ConstantConditions")
    public CanonicalizedKeyRing canonicalize(OperationLog log, int indent) {
        return canonicalize(log, indent, false);
    }


    /** "Canonicalizes" a public key, removing inconsistencies in the process.
     *
     * More specifically:
     *  - Remove all non-verifying self-certificates
     *  - Remove all "future" self-certificates
     *  - Remove all certificates flagged as "local"
     *  - For UID certificates, remove all certificates which are
     *      superseded by a newer one on the same target, including
     *      revocations with later re-certifications.
     *  - For subkey certifications, remove all certificates which
     *      are superseded by a newer one on the same target, unless
     *      it encounters a revocation certificate. The revocation
     *      certificate is considered to permanently revoke the key,
     *      even if contains later re-certifications.
     *  This is the "behavior in practice" used by (e.g.) GnuPG, and
     *  the rationale for both can be found as comments in the GnuPG
     *  source.
     *  UID signatures:
     *  https://github.com/mtigas/gnupg/blob/50c98c7ed6b542857ee2f902eca36cda37407737/g10/getkey.c#L1668-L1674
     *  Subkey signatures:
     *  https://github.com/mtigas/gnupg/blob/50c98c7ed6b542857ee2f902eca36cda37407737/g10/getkey.c#L1990-L1997
     *  - Remove all certificates in other positions if not of known type:
     *   - key revocation signatures on the master key
     *   - subkey binding signatures for subkeys
     *   - certifications and certification revocations for user ids
     *  - If a subkey retains no valid subkey binding certificate, remove it
     *  - If a user id retains no valid self certificate, remove it
     *  - If the key is a secret key, remove all certificates by foreign keys
     *  - If no valid user id remains, log an error and return null
     *
     * This operation writes an OperationLog which can be used as part of an OperationResultParcel.
     *
     * @param forExport if this is true, non-exportable signatures will be removed
     * @return A canonicalized key, or null on fatal error (log will include a message in this case)
     *
     */
    @SuppressWarnings("ConstantConditions")
    public CanonicalizedKeyRing canonicalize(OperationLog log, int indent, boolean forExport) {

        log.add(isSecret() ? LogType.MSG_KC_SECRET : LogType.MSG_KC_PUBLIC,
                indent, KeyFormattingUtils.convertKeyIdToHex(getMasterKeyId()));
        indent += 1;

        // do not accept v3 keys
        if (getVersion() <= 3) {
            log.add(LogType.MSG_KC_ERROR_V3, indent);
            return null;
        }

        Calendar nowCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        // allow for diverging clocks up to one day when checking creation time
        nowCal.add(Calendar.DAY_OF_YEAR, 1);
        final Date nowPlusOneDay = nowCal.getTime();

        int redundantCerts = 0, badCerts = 0;

        PGPKeyRing ring = mRing;
        PGPPublicKey masterKey = mRing.getPublicKey();
        final long masterKeyId = masterKey.getKeyID();

        if (Arrays.binarySearch(KNOWN_ALGORITHMS, masterKey.getAlgorithm()) < 0) {
            log.add(LogType.MSG_KC_ERROR_MASTER_ALGO, indent,
                    Integer.toString(masterKey.getAlgorithm()));
            return null;
        }

        {
            log.add(LogType.MSG_KC_MASTER,
                    indent, KeyFormattingUtils.convertKeyIdToHex(masterKey.getKeyID()));
            indent += 1;

            PGPPublicKey modified = masterKey;
            PGPSignature revocation = null;
            PGPSignature notation = null;
            for (PGPSignature zert : new IterableIterator<PGPSignature>(masterKey.getKeySignatures())) {
                int type = zert.getSignatureType();

                // These should most definitely not be here...
                if (type == PGPSignature.NO_CERTIFICATION
                        || type == PGPSignature.DEFAULT_CERTIFICATION
                        || type == PGPSignature.CASUAL_CERTIFICATION
                        || type == PGPSignature.POSITIVE_CERTIFICATION
                        || type == PGPSignature.CERTIFICATION_REVOCATION) {
                    log.add(LogType.MSG_KC_MASTER_BAD_TYPE_UID, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }
                WrappedSignature cert = new WrappedSignature(zert);

                if (type != PGPSignature.KEY_REVOCATION && type != PGPSignature.DIRECT_KEY) {
                    // Unknown type, just remove
                    log.add(LogType.MSG_KC_MASTER_BAD_TYPE, indent, "0x" + Integer.toString(type, 16));
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                if (cert.getCreationTime().after(nowPlusOneDay)) {
                    // Creation date in the future? No way!
                    log.add(LogType.MSG_KC_MASTER_BAD_TIME, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                try {
                    cert.init(masterKey);
                    if (!cert.verifySignature(masterKey)) {
                        log.add(LogType.MSG_KC_MASTER_BAD, indent);
                        modified = PGPPublicKey.removeCertification(modified, zert);
                        badCerts += 1;
                        continue;
                    }
                } catch (PgpGeneralException e) {
                    log.add(LogType.MSG_KC_MASTER_BAD_ERR, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                // if this is for export, we always remove any non-exportable certs
                if (forExport && cert.isLocal()) {
                    // Remove revocation certs with "local" flag
                    log.add(LogType.MSG_KC_MASTER_LOCAL, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    continue;
                }

                // special case: non-exportable, direct key signatures for notations!
                if (cert.getSignatureType() == PGPSignature.DIRECT_KEY) {
                    // must be local, otherwise strip!
                    if (!cert.isLocal()) {
                        log.add(LogType.MSG_KC_MASTER_BAD_TYPE, indent);
                        modified = PGPPublicKey.removeCertification(modified, zert);
                        badCerts += 1;
                        continue;
                    }

                    // first notation? fine then.
                    if (notation == null) {
                        notation = zert;
                        // more notations? at least one is superfluous, then.
                    } else if (notation.getCreationTime().before(zert.getCreationTime())) {
                        log.add(LogType.MSG_KC_NOTATION_DUP, indent);
                        modified = PGPPublicKey.removeCertification(modified, notation);
                        redundantCerts += 1;
                        notation = zert;
                    } else {
                        log.add(LogType.MSG_KC_NOTATION_DUP, indent);
                        modified = PGPPublicKey.removeCertification(modified, zert);
                        redundantCerts += 1;
                    }
                    continue;
                } else if (cert.isLocal()) {
                    // Remove revocation certs with "local" flag
                    log.add(LogType.MSG_KC_MASTER_BAD_LOCAL, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                // first revocation? fine then.
                if (revocation == null) {
                    revocation = zert;
                    // more revocations? at least one is superfluous, then.
                } else if (revocation.getCreationTime().before(zert.getCreationTime())) {
                    log.add(LogType.MSG_KC_REVOKE_DUP, indent);
                    modified = PGPPublicKey.removeCertification(modified, revocation);
                    redundantCerts += 1;
                    revocation = zert;
                } else {
                    log.add(LogType.MSG_KC_REVOKE_DUP, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    redundantCerts += 1;
                }
            }

            // If we have a notation packet, check if there is even any data in it?
            if (notation != null) {
                // If there isn't, might as well strip it
                if (new WrappedSignature(notation).getNotation().isEmpty()) {
                    log.add(LogType.MSG_KC_NOTATION_EMPTY, indent);
                    modified = PGPPublicKey.removeCertification(modified, notation);
                    redundantCerts += 1;
                }
            }

            ArrayList<String> processedUserIds = new ArrayList<>();
            for (byte[] rawUserId : new IterableIterator<byte[]>(masterKey.getRawUserIDs())) {
                String userId = Utf8Util.fromUTF8ByteArrayReplaceBadEncoding(rawUserId);

                // warn if user id was made with bad encoding
                if (!Utf8Util.isValidUTF8(rawUserId)) {
                    log.add(LogType.MSG_KC_UID_WARN_ENCODING, indent);
                }

                // check for duplicate user ids
                if (processedUserIds.contains(userId)) {
                    log.add(LogType.MSG_KC_UID_DUP, indent, userId);
                    // strip out the first found user id with this name
                    modified = PGPPublicKey.removeCertification(modified, rawUserId);
                }
                if (processedUserIds.size() > CANONICALIZE_MAX_USER_IDS) {
                    log.add(LogType.MSG_KC_UID_TOO_MANY, indent, userId);
                    // strip out the user id
                    modified = PGPPublicKey.removeCertification(modified, rawUserId);
                }
                processedUserIds.add(userId);

                PGPSignature selfCert = null;
                revocation = null;

                // look through signatures for this specific user id
                @SuppressWarnings("unchecked")
                Iterator<PGPSignature> signaturesIt = masterKey.getSignaturesForID(rawUserId);
                if (signaturesIt != null) {
                    for (PGPSignature zert : new IterableIterator<>(signaturesIt)) {
                        WrappedSignature cert = new WrappedSignature(zert);
                        long certId = cert.getKeyId();

                        int type = zert.getSignatureType();
                        if (type != PGPSignature.DEFAULT_CERTIFICATION
                                && type != PGPSignature.NO_CERTIFICATION
                                && type != PGPSignature.CASUAL_CERTIFICATION
                                && type != PGPSignature.POSITIVE_CERTIFICATION
                                && type != PGPSignature.CERTIFICATION_REVOCATION) {
                            log.add(LogType.MSG_KC_UID_BAD_TYPE,
                                    indent, "0x" + Integer.toString(zert.getSignatureType(), 16));
                            modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                            badCerts += 1;
                            continue;
                        }

                        if (cert.getCreationTime().after(nowPlusOneDay)) {
                            // Creation date in the future? No way!
                            log.add(LogType.MSG_KC_UID_BAD_TIME, indent);
                            modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                            badCerts += 1;
                            continue;
                        }

                        if (cert.isLocal()) {
                            // Creation date in the future? No way!
                            log.add(LogType.MSG_KC_UID_BAD_LOCAL, indent);
                            modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                            badCerts += 1;
                            continue;
                        }

                        // If this is a foreign signature, ...
                        if (certId != masterKeyId) {
                            // never mind any further for public keys, but remove them from secret ones
                            if (isSecret()) {
                                log.add(LogType.MSG_KC_UID_FOREIGN,
                                        indent, KeyFormattingUtils.convertKeyIdToHex(certId));
                                modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                                badCerts += 1;
                            }
                            continue;
                        }

                        // Otherwise, first make sure it checks out
                        try {
                            cert.init(masterKey);
                            if (!cert.verifySignature(masterKey, rawUserId)) {
                                log.add(LogType.MSG_KC_UID_BAD,
                                        indent, userId);
                                modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                                badCerts += 1;
                                continue;
                            }
                        } catch (PgpGeneralException e) {
                            log.add(LogType.MSG_KC_UID_BAD_ERR,
                                    indent, userId);
                            modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                            badCerts += 1;
                            continue;
                        }

                        switch (type) {
                            case PGPSignature.DEFAULT_CERTIFICATION:
                            case PGPSignature.NO_CERTIFICATION:
                            case PGPSignature.CASUAL_CERTIFICATION:
                            case PGPSignature.POSITIVE_CERTIFICATION:
                                if (selfCert == null) {
                                    selfCert = zert;
                                } else if (selfCert.getCreationTime().before(cert.getCreationTime())) {
                                    log.add(LogType.MSG_KC_UID_CERT_DUP,
                                            indent, userId);
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, selfCert);
                                    redundantCerts += 1;
                                    selfCert = zert;
                                } else {
                                    log.add(LogType.MSG_KC_UID_CERT_DUP,
                                            indent, userId);
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                                    redundantCerts += 1;
                                }
                                // If there is a revocation certificate, and it's older than this, drop it
                                if (revocation != null
                                        && revocation.getCreationTime().before(selfCert.getCreationTime())) {
                                    log.add(LogType.MSG_KC_UID_REVOKE_OLD,
                                            indent, userId);
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, revocation);
                                    revocation = null;
                                    redundantCerts += 1;
                                }
                                break;

                            case PGPSignature.CERTIFICATION_REVOCATION:
                                // If this is older than the (latest) self cert, drop it
                                if (selfCert != null && selfCert.getCreationTime().after(zert.getCreationTime())) {
                                    log.add(LogType.MSG_KC_UID_REVOKE_OLD,
                                            indent, userId);
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                                    redundantCerts += 1;
                                    continue;
                                }
                                // first revocation? remember it.
                                if (revocation == null) {
                                    revocation = zert;
                                    // more revocations? at least one is superfluous, then.
                                } else if (revocation.getCreationTime().before(cert.getCreationTime())) {
                                    log.add(LogType.MSG_KC_UID_REVOKE_DUP,
                                            indent, userId);
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, revocation);
                                    redundantCerts += 1;
                                    revocation = zert;
                                } else {
                                    log.add(LogType.MSG_KC_UID_REVOKE_DUP,
                                            indent, userId);
                                    modified = PGPPublicKey.removeCertification(modified, rawUserId, zert);
                                    redundantCerts += 1;
                                }
                                break;
                        }
                    }
                }

                // If no valid certificate (if only a revocation) remains, drop it
                if (selfCert == null && revocation == null) {
                    log.add(LogType.MSG_KC_UID_REMOVE,
                            indent, userId);
                    modified = PGPPublicKey.removeCertification(modified, rawUserId);
                }
            }

            // If NO user ids remain, error out!
            if (modified == null || !modified.getUserIDs().hasNext()) {
                log.add(LogType.MSG_KC_ERROR_NO_UID, indent);
                return null;
            }

            ArrayList<PGPUserAttributeSubpacketVector> processedUserAttributes = new ArrayList<>();
            for (PGPUserAttributeSubpacketVector userAttribute :
                    new IterableIterator<PGPUserAttributeSubpacketVector>(masterKey.getUserAttributes())) {

                if (userAttribute.getSubpacket(UserAttributeSubpacketTags.IMAGE_ATTRIBUTE) != null) {
                    log.add(LogType.MSG_KC_UAT_JPEG, indent);
                } else {
                    log.add(LogType.MSG_KC_UAT_UNKNOWN, indent);
                }

                try {
                    indent += 1;

                    // check for duplicate user attributes
                    if (processedUserAttributes.contains(userAttribute)) {
                        log.add(LogType.MSG_KC_UAT_DUP, indent);
                        // strip out the first found user id with this name
                        modified = PGPPublicKey.removeCertification(modified, userAttribute);
                    }
                    processedUserAttributes.add(userAttribute);

                    PGPSignature selfCert = null;
                    revocation = null;

                    // look through signatures for this specific user id
                    @SuppressWarnings("unchecked")
                    Iterator<PGPSignature> signaturesIt = masterKey.getSignaturesForUserAttribute(userAttribute);
                    if (signaturesIt != null) {
                        for (PGPSignature zert : new IterableIterator<>(signaturesIt)) {
                            WrappedSignature cert = new WrappedSignature(zert);
                            long certId = cert.getKeyId();

                            int type = zert.getSignatureType();
                            if (type != PGPSignature.DEFAULT_CERTIFICATION
                                    && type != PGPSignature.NO_CERTIFICATION
                                    && type != PGPSignature.CASUAL_CERTIFICATION
                                    && type != PGPSignature.POSITIVE_CERTIFICATION
                                    && type != PGPSignature.CERTIFICATION_REVOCATION) {
                                log.add(LogType.MSG_KC_UAT_BAD_TYPE,
                                        indent, "0x" + Integer.toString(zert.getSignatureType(), 16));
                                modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                badCerts += 1;
                                continue;
                            }

                            if (cert.getCreationTime().after(nowPlusOneDay)) {
                                // Creation date in the future? No way!
                                log.add(LogType.MSG_KC_UAT_BAD_TIME, indent);
                                modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                badCerts += 1;
                                continue;
                            }

                            if (cert.isLocal()) {
                                // Creation date in the future? No way!
                                log.add(LogType.MSG_KC_UAT_BAD_LOCAL, indent);
                                modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                badCerts += 1;
                                continue;
                            }

                            // If this is a foreign signature, ...
                            if (certId != masterKeyId) {
                                // never mind any further for public keys, but remove them from secret ones
                                if (isSecret()) {
                                    log.add(LogType.MSG_KC_UAT_FOREIGN,
                                            indent, KeyFormattingUtils.convertKeyIdToHex(certId));
                                    modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                    badCerts += 1;
                                }
                                continue;
                            }

                            // Otherwise, first make sure it checks out
                            try {
                                cert.init(masterKey);
                                if (!cert.verifySignature(masterKey, userAttribute)) {
                                    log.add(LogType.MSG_KC_UAT_BAD,
                                            indent);
                                    modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                    badCerts += 1;
                                    continue;
                                }
                            } catch (PgpGeneralException e) {
                                log.add(LogType.MSG_KC_UAT_BAD_ERR,
                                        indent);
                                modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                badCerts += 1;
                                continue;
                            }

                            switch (type) {
                                case PGPSignature.DEFAULT_CERTIFICATION:
                                case PGPSignature.NO_CERTIFICATION:
                                case PGPSignature.CASUAL_CERTIFICATION:
                                case PGPSignature.POSITIVE_CERTIFICATION:
                                    if (selfCert == null) {
                                        selfCert = zert;
                                    } else if (selfCert.getCreationTime().before(cert.getCreationTime())) {
                                        log.add(LogType.MSG_KC_UAT_CERT_DUP,
                                                indent);
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, selfCert);
                                        redundantCerts += 1;
                                        selfCert = zert;
                                    } else {
                                        log.add(LogType.MSG_KC_UAT_CERT_DUP,
                                                indent);
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                        redundantCerts += 1;
                                    }
                                    // If there is a revocation certificate, and it's older than this, drop it
                                    if (revocation != null
                                            && revocation.getCreationTime().before(selfCert.getCreationTime())) {
                                        log.add(LogType.MSG_KC_UAT_REVOKE_OLD,
                                                indent);
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, revocation);
                                        revocation = null;
                                        redundantCerts += 1;
                                    }
                                    break;

                                case PGPSignature.CERTIFICATION_REVOCATION:
                                    // If this is older than the (latest) self cert, drop it
                                    if (selfCert != null && selfCert.getCreationTime().after(zert.getCreationTime())) {
                                        log.add(LogType.MSG_KC_UAT_REVOKE_OLD,
                                                indent);
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                        redundantCerts += 1;
                                        continue;
                                    }
                                    // first revocation? remember it.
                                    if (revocation == null) {
                                        revocation = zert;
                                        // more revocations? at least one is superfluous, then.
                                    } else if (revocation.getCreationTime().before(cert.getCreationTime())) {
                                        log.add(LogType.MSG_KC_UAT_REVOKE_DUP,
                                                indent);
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, revocation);
                                        redundantCerts += 1;
                                        revocation = zert;
                                    } else {
                                        log.add(LogType.MSG_KC_UAT_REVOKE_DUP,
                                                indent);
                                        modified = PGPPublicKey.removeCertification(modified, userAttribute, zert);
                                        redundantCerts += 1;
                                    }
                                    break;
                            }
                        }
                    }

                    // If no valid certificate (if only a revocation) remains, drop it
                    if (selfCert == null && revocation == null) {
                        log.add(LogType.MSG_KC_UAT_REMOVE,
                                indent);
                        modified = PGPPublicKey.removeCertification(modified, userAttribute);
                    }

                } finally {
                    indent -= 1;
                }
            }


            // Replace modified key in the keyring
            ring = replacePublicKey(ring, modified);
            indent -= 1;

        }

        // Keep track of ids we encountered so far
        Set<Long> knownIds = new HashSet<>();

        // Process all keys
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(ring.getPublicKeys())) {
            // Make sure this is not a duplicate, avoid undefined behavior!
            if (knownIds.contains(key.getKeyID())) {
                log.add(LogType.MSG_KC_ERROR_DUP_KEY, indent,
                        KeyFormattingUtils.convertKeyIdToHex(key.getKeyID()));
                return null;
            }
            // Add the key id to known
            knownIds.add(key.getKeyID());

            // Don't care about the master key any further, that one gets special treatment above
            if (key.isMasterKey()) {
                continue;
            }

            log.add(LogType.MSG_KC_SUB,
                    indent, KeyFormattingUtils.convertKeyIdToHex(key.getKeyID()));
            indent += 1;

            if (Arrays.binarySearch(KNOWN_ALGORITHMS, key.getAlgorithm()) < 0) {
                ring = removeSubKey(ring, key);

                log.add(LogType.MSG_KC_SUB_UNKNOWN_ALGO, indent,
                        Integer.toString(key.getAlgorithm()));
                indent -= 1;
                continue;
            }

            Date keyCreationTime = key.getCreationTime(), keyCreationTimeLenient;
            {
                Calendar keyCreationCal = Calendar.getInstance();
                keyCreationCal.setTime(keyCreationTime);
                // allow for diverging clocks up to one day when checking creation time
                keyCreationCal.add(Calendar.MINUTE, -5);
                keyCreationTimeLenient = keyCreationCal.getTime();
            }

            // A subkey needs exactly one subkey binding certificate, and optionally one revocation
            // certificate.
            PGPPublicKey modified = key;
            PGPSignature selfCert = null, revocation = null;
            uids: for (PGPSignature zert : new IterableIterator<PGPSignature>(key.getSignatures())) {
                // remove from keyring (for now)
                modified = PGPPublicKey.removeCertification(modified, zert);

                WrappedSignature cert = new WrappedSignature(zert);
                int type = cert.getSignatureType();

                // filter out bad key types...
                if (cert.getKeyId() != masterKey.getKeyID()) {
                    log.add(LogType.MSG_KC_SUB_BAD_KEYID, indent);
                    badCerts += 1;
                    continue;
                }

                if (type != PGPSignature.SUBKEY_BINDING && type != PGPSignature.SUBKEY_REVOCATION) {
                    log.add(LogType.MSG_KC_SUB_BAD_TYPE, indent, "0x" + Integer.toString(type, 16));
                    badCerts += 1;
                    continue;
                }

                if (cert.getCreationTime().after(nowPlusOneDay)) {
                    // Creation date in the future? No way!
                    log.add(LogType.MSG_KC_SUB_BAD_TIME, indent);
                    badCerts += 1;
                    continue;
                }

                if (cert.getCreationTime().before(keyCreationTime)) {
                    // Signature is earlier than key creation time
                    log.add(LogType.MSG_KC_SUB_BAD_TIME_EARLY, indent);
                    // due to an earlier accident, we generated keys which had creation timestamps
                    // a few seconds after their signature timestamp. for compatibility, we only
                    // error out with some margin of error
                    if (cert.getCreationTime().before(keyCreationTimeLenient)) {
                        badCerts += 1;
                        continue;
                    }
                }

                if (cert.isLocal()) {
                    // Creation date in the future? No way!
                    log.add(LogType.MSG_KC_SUB_BAD_LOCAL, indent);
                    badCerts += 1;
                    continue;
                }

                if (type == PGPSignature.SUBKEY_BINDING) {

                    // make sure the certificate checks out
                    try {
                        cert.init(masterKey);
                        if (!cert.verifySignature(masterKey, key)) {
                            log.add(LogType.MSG_KC_SUB_BAD, indent);
                            badCerts += 1;
                            continue;
                        }
                    } catch (PgpGeneralException e) {
                        log.add(LogType.MSG_KC_SUB_BAD_ERR, indent);
                        badCerts += 1;
                        continue;
                    }

                    boolean needsPrimaryBinding = false;

                    // If the algorithm is even suitable for signing
                    if (isSigningAlgo(key.getAlgorithm())) {

                        // If this certificate says it allows signing for the key
                        if (zert.getHashedSubPackets() != null &&
                                zert.getHashedSubPackets().hasSubpacket(SignatureSubpacketTags.KEY_FLAGS)) {
                            int flags = ((KeyFlags) zert.getHashedSubPackets()
                                    .getSubpacket(SignatureSubpacketTags.KEY_FLAGS)).getFlags();
                            if ((flags & KeyFlags.SIGN_DATA) == KeyFlags.SIGN_DATA) {
                                needsPrimaryBinding = true;
                            }
                        } else {
                            // If there are no key flags, we STILL require this because the key can sign!
                            needsPrimaryBinding = true;
                        }

                    }

                    // If this key can sign, it MUST have a primary key binding certificate
                    if (needsPrimaryBinding) {
                        boolean ok = false;
                        if (zert.getUnhashedSubPackets() != null) try {
                            // Check all embedded signatures, if any of them fits
                            PGPSignatureList list = zert.getUnhashedSubPackets().getEmbeddedSignatures();
                            for (int i = 0; i < list.size(); i++) {
                                WrappedSignature subsig = new WrappedSignature(list.get(i));
                                if (subsig.getSignatureType() == PGPSignature.PRIMARYKEY_BINDING) {
                                    subsig.init(key);
                                    if (subsig.verifySignature(masterKey, key)) {
                                        ok = true;
                                    } else {
                                        log.add(LogType.MSG_KC_SUB_PRIMARY_BAD, indent);
                                        badCerts += 1;
                                        continue uids;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.add(LogType.MSG_KC_SUB_PRIMARY_BAD_ERR, indent);
                            badCerts += 1;
                            continue;
                        }
                        // if it doesn't, get rid of this!
                        if (!ok) {
                            log.add(LogType.MSG_KC_SUB_PRIMARY_NONE, indent);
                            badCerts += 1;
                            continue;
                        }
                    }

                    // if we already have a cert, and this one is older: skip it
                    if (selfCert != null && cert.getCreationTime().before(selfCert.getCreationTime())) {
                        log.add(LogType.MSG_KC_SUB_DUP, indent);
                        redundantCerts += 1;
                        continue;
                    }

                    selfCert = zert;

                // it must be a revocation, then (we made sure above)
                } else {

                    // make sure the certificate checks out
                    try {
                        cert.init(masterKey);
                        if (!cert.verifySignature(masterKey, key)) {
                            log.add(LogType.MSG_KC_SUB_REVOKE_BAD, indent);
                            badCerts += 1;
                            continue;
                        }
                    } catch (PgpGeneralException e) {
                        log.add(LogType.MSG_KC_SUB_REVOKE_BAD_ERR, indent);
                        badCerts += 1;
                        continue;
                    }

                    // If we already have a newer revocation cert, skip this one.
                    if (revocation != null &&
                        revocation.getCreationTime().after(cert.getCreationTime())) {
                        log.add(LogType.MSG_KC_SUB_REVOKE_DUP, indent);
                        redundantCerts += 1;
                        continue;
                    }

                    revocation = zert;
                }
            }

            // it is not properly bound? error!
            if (selfCert == null) {
                ring = removeSubKey(ring, key);

                log.add(LogType.MSG_KC_SUB_NO_CERT,
                        indent, KeyFormattingUtils.convertKeyIdToHex(key.getKeyID()));
                indent -= 1;
                continue;
            }

            // If we have flags, check if the algorithm supports all of them
            if (selfCert.getHashedSubPackets() != null
                    && selfCert.getHashedSubPackets().hasSubpacket(SignatureSubpacketTags.KEY_FLAGS)) {
                int flags = ((KeyFlags) selfCert.getHashedSubPackets().getSubpacket(SignatureSubpacketTags.KEY_FLAGS)).getFlags();
                int algo = key.getAlgorithm();
                // If this is a signing key, but not a signing algorithm, warn the user
                if (!isSigningAlgo(algo) && (flags & KeyFlags.SIGN_DATA) == KeyFlags.SIGN_DATA) {
                    log.add(LogType.MSG_KC_SUB_ALGO_BAD_SIGN, indent);
                }
                // If this is an encryption key, but not an encryption algorithm, warn the user
                if (!isEncryptionAlgo(algo) && (
                           (flags & KeyFlags.ENCRYPT_STORAGE) == KeyFlags.ENCRYPT_STORAGE
                        || (flags & KeyFlags.ENCRYPT_COMMS) == KeyFlags.ENCRYPT_COMMS
                    )) {
                    log.add(LogType.MSG_KC_SUB_ALGO_BAD_ENCRYPT, indent);
                }
            }

            // re-add certification
            modified = PGPPublicKey.addCertification(modified, selfCert);
            // add revocation, if any
            if (revocation != null) {
                modified = PGPPublicKey.addCertification(modified, revocation);
            }
            // replace pubkey in keyring
            ring = replacePublicKey(ring, modified);
            indent -= 1;
        }

        if (badCerts > 0 && redundantCerts > 0) {
            // multi plural would make this complex, just leaving this as is...
            log.add(LogType.MSG_KC_SUCCESS_BAD_AND_RED,
                    indent, Integer.toString(badCerts), Integer.toString(redundantCerts));
        } else if (badCerts > 0) {
            log.add(LogType.MSG_KC_SUCCESS_BAD,
                    indent, badCerts);
        } else if (redundantCerts > 0) {
            log.add(LogType.MSG_KC_SUCCESS_REDUNDANT,
                    indent, redundantCerts);
        } else {
            log.add(LogType.MSG_KC_SUCCESS, indent);
        }

        return isSecret() ? new CanonicalizedSecretKeyRing((PGPSecretKeyRing) ring, 1)
                          : new CanonicalizedPublicKeyRing((PGPPublicKeyRing) ring, 0);
    }

    /** This operation merges information from a different keyring, returning a combined
     * UncachedKeyRing.
     *
     * The combined keyring contains the subkeys, user ids and user attributes of both input
     * keyrings, but it does not necessarily have the canonicalized property.
     *
     * @param other The UncachedKeyRing to merge. Must not be empty, and of the same masterKeyId
     * @return A consolidated UncachedKeyRing with the data of both input keyrings. Same type as
     * this object, or null on error.
     *
     */
    public UncachedKeyRing merge(UncachedKeyRing other, OperationLog log, int indent) {

        // This is logged in the calling method to provide more meta info
        // log.add(isSecret() ? LogType.MSG_MG_SECRET : LogType.MSG_MG_PUBLIC,
                // indent, KeyFormattingUtils.convertKeyIdToHex(getMasterKeyId()));
        indent += 1;

        long masterKeyId = other.getMasterKeyId();

        if (getMasterKeyId() != masterKeyId
                || !Arrays.equals(getFingerprint(), other.getFingerprint())) {
            log.add(LogType.MSG_MG_ERROR_HETEROGENEOUS, indent);
            return null;
        }

        // remember which certs we already added. this is cheaper than semantic deduplication
        Set<byte[]> certs = new TreeSet<>(new Comparator<byte[]>() {
            public int compare(byte[] left, byte[] right) {
                // check for length equality
                if (left.length != right.length) {
                    return left.length - right.length;
                }
                // compare byte-by-byte
                for (int i = 0; i < left.length; i++) {
                    if (left[i] != right[i]) {
                        return (left[i] & 0xff) - (right[i] & 0xff);
                    }
                }
                // ok they're the same
                return 0;
        }});

        try {
            PGPKeyRing result = mRing;
            PGPKeyRing candidate = other.mRing;

            // Pre-load all existing certificates
            for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(result.getPublicKeys())) {
                for (PGPSignature cert : new IterableIterator<PGPSignature>(key.getSignatures())) {
                    certs.add(cert.getEncoded());
                }
            }

            // keep track of the number of new certs we add
            int newCerts = 0;

            for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(candidate.getPublicKeys())) {

                final PGPPublicKey resultKey = result.getPublicKey(key.getKeyID());
                if (resultKey == null) {
                    log.add(LogType.MSG_MG_NEW_SUBKEY, indent);
                    // special case: if both rings are secret, copy over the secret key
                    if (isSecret() && other.isSecret()) {
                        PGPSecretKey sKey = ((PGPSecretKeyRing) candidate).getSecretKey(key.getKeyID());
                        result = PGPSecretKeyRing.insertSecretKey((PGPSecretKeyRing) result, sKey);
                    } else {
                        // otherwise, just insert the public key
                        result = replacePublicKey(result, key);
                    }
                    continue;
                }

                // Modifiable version of the old key, which we merge stuff into (keep old for comparison)
                PGPPublicKey modified = resultKey;

                // Iterate certifications
                for (PGPSignature cert : new IterableIterator<PGPSignature>(key.getKeySignatures())) {
                    // Don't merge foreign stuff into secret keys
                    if (cert.getKeyID() != masterKeyId && isSecret()) {
                        continue;
                    }

                    byte[] encoded = cert.getEncoded();
                    // Known cert, skip it
                    if (certs.contains(encoded)) {
                        continue;
                    }
                    certs.add(encoded);
                    modified = PGPPublicKey.addCertification(modified, cert);
                    newCerts += 1;
                }

                // If this is a subkey, merge it in and stop here
                if (!key.isMasterKey()) {
                    if (modified != resultKey) {
                        result = replacePublicKey(result, modified);
                    }
                    continue;
                }

                // Copy over all user id certificates
                for (byte[] rawUserId : new IterableIterator<byte[]>(key.getRawUserIDs())) {
                    @SuppressWarnings("unchecked")
                    Iterator<PGPSignature> signaturesIt = key.getSignaturesForID(rawUserId);
                    // no signatures for this User ID, skip it
                    if (signaturesIt == null) {
                        continue;
                    }
                    for (PGPSignature cert : new IterableIterator<>(signaturesIt)) {
                        // Don't merge foreign stuff into secret keys
                        if (cert.getKeyID() != masterKeyId && isSecret()) {
                            continue;
                        }
                        byte[] encoded = cert.getEncoded();
                        // Known cert, skip it
                        if (certs.contains(encoded)) {
                            continue;
                        }
                        newCerts += 1;
                        certs.add(encoded);
                        modified = PGPPublicKey.addCertification(modified, rawUserId, cert);
                    }
                }

                // Copy over all user attribute certificates
                for (PGPUserAttributeSubpacketVector vector :
                        new IterableIterator<PGPUserAttributeSubpacketVector>(key.getUserAttributes())) {
                    @SuppressWarnings("unchecked")
                    Iterator<PGPSignature> signaturesIt = key.getSignaturesForUserAttribute(vector);
                    // no signatures for this user attribute attribute, skip it
                    if (signaturesIt == null) {
                        continue;
                    }
                    for (PGPSignature cert : new IterableIterator<>(signaturesIt)) {
                        // Don't merge foreign stuff into secret keys
                        if (cert.getKeyID() != masterKeyId && isSecret()) {
                            continue;
                        }
                        byte[] encoded = cert.getEncoded();
                        // Known cert, skip it
                        if (certs.contains(encoded)) {
                            continue;
                        }
                        newCerts += 1;
                        certs.add(encoded);
                        modified = PGPPublicKey.addCertification(modified, vector, cert);
                    }
                }

                // If anything change, save the updated (sub)key
                if (modified != resultKey) {
                    result = replacePublicKey(result, modified);
                }

            }

            if (newCerts > 0) {
                log.add(LogType.MSG_MG_FOUND_NEW, indent,
                        Integer.toString(newCerts));
            } else {
                log.add(LogType.MSG_MG_UNCHANGED, indent);
            }

            return new UncachedKeyRing(result);

        } catch (IOException e) {
            log.add(LogType.MSG_MG_ERROR_ENCODE, indent);
            return null;
        }

    }

    public UncachedKeyRing extractPublicKeyRing() throws IOException {
        if(!isSecret()) {
            throw new RuntimeException("Tried to extract public keyring from non-secret keyring. " +
                    "This is a programming error and should never happen!");
        }

        Iterator<PGPPublicKey> it = mRing.getPublicKeys();
        ByteArrayOutputStream stream = new ByteArrayOutputStream(2048);
        while (it.hasNext()) {
            stream.write(it.next().getEncoded());
        }

        return new UncachedKeyRing(
                new PGPPublicKeyRing(stream.toByteArray(), new JcaKeyFingerprintCalculator()));
    }

    /** This method replaces a public key in a keyring.
     *
     * This method essentially wraps PGP*KeyRing.insertPublicKey, where the keyring may be of either
     * the secret or public subclass.
     *
     * @return the resulting PGPKeyRing of the same type as the input
     */
    private static PGPKeyRing replacePublicKey(PGPKeyRing ring, PGPPublicKey key) {
        if (ring instanceof PGPPublicKeyRing) {
            PGPPublicKeyRing pubRing = (PGPPublicKeyRing) ring;
            return PGPPublicKeyRing.insertPublicKey(pubRing, key);
        } else {
            PGPSecretKeyRing secRing = (PGPSecretKeyRing) ring;
            PGPSecretKey sKey = secRing.getSecretKey(key.getKeyID());
            // if this is a secret key which does not yet occur in the secret ring
            if (sKey == null) {
                // generate a stripped secret (sub)key
                sKey = PGPSecretKey.constructGnuDummyKey(key);
            }
            sKey = PGPSecretKey.replacePublicKey(sKey, key);
            return PGPSecretKeyRing.insertSecretKey(secRing, sKey);
        }
    }

    /** This method removes a subkey in a keyring.
     *
     * This method essentially wraps PGP*KeyRing.remove*Key, where the keyring may be of either
     * the secret or public subclass.
     *
     * @return the resulting PGPKeyRing of the same type as the input
     */
    private static PGPKeyRing removeSubKey(PGPKeyRing ring, PGPPublicKey key) {
        if (ring instanceof PGPPublicKeyRing) {
            return PGPPublicKeyRing.removePublicKey((PGPPublicKeyRing) ring, key);
        } else {
            PGPSecretKey sKey = ((PGPSecretKeyRing) ring).getSecretKey(key.getKeyID());
            return PGPSecretKeyRing.removeSecretKey((PGPSecretKeyRing) ring, sKey);
        }
    }


    /** Returns true if the algorithm is of a type which is suitable for signing. */
    static boolean isSigningAlgo(int algorithm) {
        return algorithm == PGPPublicKey.RSA_GENERAL
                || algorithm == PGPPublicKey.RSA_SIGN
                || algorithm == PGPPublicKey.DSA
                || algorithm == PGPPublicKey.ELGAMAL_GENERAL
                || algorithm == PGPPublicKey.ECDSA;
    }

    /** Returns true if the algorithm is of a type which is suitable for encryption. */
    static boolean isEncryptionAlgo(int algorithm) {
        return algorithm == PGPPublicKey.RSA_GENERAL
                || algorithm == PGPPublicKey.RSA_ENCRYPT
                || algorithm == PGPPublicKey.ELGAMAL_ENCRYPT
                || algorithm == PGPPublicKey.ELGAMAL_GENERAL
                || algorithm == PGPPublicKey.ECDH;
    }

    // ONLY TO BE USED FOR TESTING!!
    @VisibleForTesting
    public static UncachedKeyRing forTestingOnlyAddDummyLocalSignature(
            UncachedKeyRing uncachedKeyRing, String passphrase) throws Exception {
        PGPSecretKeyRing sKR = (PGPSecretKeyRing) uncachedKeyRing.mRing;

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                Constants.BOUNCY_CASTLE_PROVIDER_NAME).build(passphrase.toCharArray());
        PGPPrivateKey masterPrivateKey = sKR.getSecretKey().extractPrivateKey(keyDecryptor);
        PGPPublicKey masterPublicKey = uncachedKeyRing.mRing.getPublicKey();

        // add packet with "pin" notation data
        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                masterPrivateKey.getPublicKeyPacket().getAlgorithm(),
                PgpSecurityConstants.SECRET_KEY_BINDING_SIGNATURE_HASH_ALGO)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        { // set subpackets
            PGPSignatureSubpacketGenerator hashedPacketsGen = new PGPSignatureSubpacketGenerator();
            hashedPacketsGen.setExportable(false, false);
            hashedPacketsGen.setNotationData(false, true, "dummynotationdata", "some data");
            sGen.setHashedSubpackets(hashedPacketsGen.generate());
        }
        sGen.init(PGPSignature.DIRECT_KEY, masterPrivateKey);
        PGPSignature emptySig = sGen.generateCertification(masterPublicKey);

        masterPublicKey = PGPPublicKey.addCertification(masterPublicKey, emptySig);
        sKR = PGPSecretKeyRing.insertSecretKey(sKR,
                PGPSecretKey.replacePublicKey(sKR.getSecretKey(), masterPublicKey));

        return new UncachedKeyRing(sKR);
    }

}

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

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPKeyFlags;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPUtil;
import org.spongycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogLevel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogType;
import org.sufficientlysecure.keychain.service.OperationResultParcel.OperationLog;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

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
@SuppressWarnings("unchecked")
public class UncachedKeyRing {

    final PGPKeyRing mRing;
    final boolean mIsSecret;

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

    public byte[] getFingerprint() {
        return mRing.getPublicKey().getFingerprint();
    }

    public int getVersion() {
        return mRing.getPublicKey().getVersion();
    }

    public static UncachedKeyRing decodeFromData(byte[] data)
            throws PgpGeneralException, IOException {

        Iterator<UncachedKeyRing> parsed = fromStream(new ByteArrayInputStream(data));

        if ( ! parsed.hasNext()) {
            throw new PgpGeneralException("Object not recognized as PGPKeyRing!");
        }

        UncachedKeyRing ring = parsed.next();

        if (parsed.hasNext()) {
            throw new PgpGeneralException("Expected single keyring in stream, found at least two");
        }

        return ring;

    }

    public static Iterator<UncachedKeyRing> fromStream(final InputStream stream) throws IOException {

        return new Iterator<UncachedKeyRing>() {

            UncachedKeyRing mNext = null;
            PGPObjectFactory mObjectFactory = null;

            private void cacheNext() {
                if (mNext != null) {
                    return;
                }

                try {
                    while(stream.available() > 0) {
                        // if there are no objects left from the last factory, create a new one
                        if (mObjectFactory == null) {
                            mObjectFactory = new PGPObjectFactory(PGPUtil.getDecoderStream(stream),
                                    new JcaKeyFingerprintCalculator());
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
                } catch (IOException e) {
                    Log.e(Constants.TAG, "IOException while processing stream", e);
                }

            }

            @Override
            public boolean hasNext() {
                cacheNext();
                return mNext != null;
            }

            @Override
            public UncachedKeyRing next() {
                try {
                    cacheNext();
                    return mNext;
                } finally {
                    mNext = null;
                }
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };

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
     *  - Remove all certificates which are superseded by a newer one on the same target,
     *      including revocations with later re-certifications.
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

        log.add(LogLevel.START, isSecret() ? LogType.MSG_KC_SECRET : LogType.MSG_KC_PUBLIC,
                indent, PgpKeyHelper.convertKeyIdToHex(getMasterKeyId()));
        indent += 1;

        // do not accept v3 keys
        if (getVersion() <= 3) {
            log.add(LogLevel.ERROR, LogType.MSG_KC_ERROR_V3, indent);
            return null;
        }

        final Date now = new Date();

        int redundantCerts = 0, badCerts = 0;

        PGPKeyRing ring = mRing;
        PGPPublicKey masterKey = mRing.getPublicKey();
        final long masterKeyId = masterKey.getKeyID();

        if (Arrays.binarySearch(KNOWN_ALGORITHMS, masterKey.getAlgorithm()) < 0) {
            log.add(LogLevel.ERROR, LogType.MSG_KC_ERROR_MASTER_ALGO, indent,
                    Integer.toString(masterKey.getAlgorithm()));
            return null;
        }

        {
            log.add(LogLevel.DEBUG, LogType.MSG_KC_MASTER,
                    indent, PgpKeyHelper.convertKeyIdToHex(masterKey.getKeyID()));
            indent += 1;

            PGPPublicKey modified = masterKey;
            PGPSignature revocation = null;
            for (PGPSignature zert : new IterableIterator<PGPSignature>(masterKey.getKeySignatures())) {
                int type = zert.getSignatureType();

                // These should most definitely not be here...
                if (type == PGPSignature.NO_CERTIFICATION
                        || type == PGPSignature.DEFAULT_CERTIFICATION
                        || type == PGPSignature.CASUAL_CERTIFICATION
                        || type == PGPSignature.POSITIVE_CERTIFICATION
                        || type == PGPSignature.CERTIFICATION_REVOCATION) {
                    log.add(LogLevel.WARN, LogType.MSG_KC_REVOKE_BAD_TYPE_UID, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }
                WrappedSignature cert = new WrappedSignature(zert);

                if (type != PGPSignature.KEY_REVOCATION) {
                    // Unknown type, just remove
                    log.add(LogLevel.WARN, LogType.MSG_KC_REVOKE_BAD_TYPE, indent, "0x" + Integer.toString(type, 16));
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                if (cert.getCreationTime().after(now)) {
                    // Creation date in the future? No way!
                    log.add(LogLevel.WARN, LogType.MSG_KC_REVOKE_BAD_TIME, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                if (cert.isLocal()) {
                    // Creation date in the future? No way!
                    log.add(LogLevel.WARN, LogType.MSG_KC_REVOKE_BAD_LOCAL, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                try {
                    cert.init(masterKey);
                    if (!cert.verifySignature(masterKey)) {
                        log.add(LogLevel.WARN, LogType.MSG_KC_REVOKE_BAD, indent);
                        modified = PGPPublicKey.removeCertification(modified, zert);
                        badCerts += 1;
                        continue;
                    }
                } catch (PgpGeneralException e) {
                    log.add(LogLevel.WARN, LogType.MSG_KC_REVOKE_BAD_ERR, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    badCerts += 1;
                    continue;
                }

                // first revocation? fine then.
                if (revocation == null) {
                    revocation = zert;
                    // more revocations? at least one is superfluous, then.
                } else if (revocation.getCreationTime().before(zert.getCreationTime())) {
                    log.add(LogLevel.INFO, LogType.MSG_KC_REVOKE_DUP, indent);
                    modified = PGPPublicKey.removeCertification(modified, revocation);
                    redundantCerts += 1;
                    revocation = zert;
                } else {
                    log.add(LogLevel.INFO, LogType.MSG_KC_REVOKE_DUP, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    redundantCerts += 1;
                }
            }

            ArrayList<String> processedUserIds = new ArrayList<String>();
            for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
                // check for duplicate user ids
                if (processedUserIds.contains(userId)) {
                    log.add(LogLevel.WARN, LogType.MSG_KC_UID_DUP,
                            indent, userId);
                    // strip out the first found user id with this name
                    modified = PGPPublicKey.removeCertification(modified, userId);
                }
                processedUserIds.add(userId);

                PGPSignature selfCert = null;
                revocation = null;

                // look through signatures for this specific key
                for (PGPSignature zert : new IterableIterator<PGPSignature>(
                        masterKey.getSignaturesForID(userId))) {
                    WrappedSignature cert = new WrappedSignature(zert);
                    long certId = cert.getKeyId();

                    int type = zert.getSignatureType();
                    if (type != PGPSignature.DEFAULT_CERTIFICATION
                            && type != PGPSignature.NO_CERTIFICATION
                            && type != PGPSignature.CASUAL_CERTIFICATION
                            && type != PGPSignature.POSITIVE_CERTIFICATION
                            && type != PGPSignature.CERTIFICATION_REVOCATION) {
                        log.add(LogLevel.WARN, LogType.MSG_KC_UID_BAD_TYPE,
                                indent, "0x" + Integer.toString(zert.getSignatureType(), 16));
                        modified = PGPPublicKey.removeCertification(modified, userId, zert);
                        badCerts += 1;
                        continue;
                    }

                    if (cert.getCreationTime().after(now)) {
                        // Creation date in the future? No way!
                        log.add(LogLevel.WARN, LogType.MSG_KC_UID_BAD_TIME, indent);
                        modified = PGPPublicKey.removeCertification(modified, userId, zert);
                        badCerts += 1;
                        continue;
                    }

                    if (cert.isLocal()) {
                        // Creation date in the future? No way!
                        log.add(LogLevel.WARN, LogType.MSG_KC_UID_BAD_LOCAL, indent);
                        modified = PGPPublicKey.removeCertification(modified, userId, zert);
                        badCerts += 1;
                        continue;
                    }

                    // If this is a foreign signature, ...
                    if (certId != masterKeyId) {
                        // never mind any further for public keys, but remove them from secret ones
                        if (isSecret()) {
                            log.add(LogLevel.WARN, LogType.MSG_KC_UID_FOREIGN,
                                    indent, PgpKeyHelper.convertKeyIdToHex(certId));
                            modified = PGPPublicKey.removeCertification(modified, userId, zert);
                            badCerts += 1;
                        }
                        continue;
                    }

                    // Otherwise, first make sure it checks out
                    try {
                        cert.init(masterKey);
                        if (!cert.verifySignature(masterKey, userId)) {
                            log.add(LogLevel.WARN, LogType.MSG_KC_UID_BAD,
                                    indent, userId);
                            modified = PGPPublicKey.removeCertification(modified, userId, zert);
                            badCerts += 1;
                            continue;
                        }
                    } catch (PgpGeneralException e) {
                        log.add(LogLevel.WARN, LogType.MSG_KC_UID_BAD_ERR,
                                indent, userId);
                        modified = PGPPublicKey.removeCertification(modified, userId, zert);
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
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_CERT_DUP,
                                        indent, userId);
                                modified = PGPPublicKey.removeCertification(modified, userId, selfCert);
                                redundantCerts += 1;
                                selfCert = zert;
                            } else {
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_CERT_DUP,
                                        indent, userId);
                                modified = PGPPublicKey.removeCertification(modified, userId, zert);
                                redundantCerts += 1;
                            }
                            // If there is a revocation certificate, and it's older than this, drop it
                            if (revocation != null
                                    && revocation.getCreationTime().before(selfCert.getCreationTime())) {
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_REVOKE_OLD,
                                        indent, userId);
                                modified = PGPPublicKey.removeCertification(modified, userId, revocation);
                                revocation = null;
                                redundantCerts += 1;
                            }
                            break;

                        case PGPSignature.CERTIFICATION_REVOCATION:
                            // If this is older than the (latest) self cert, drop it
                            if (selfCert != null && selfCert.getCreationTime().after(zert.getCreationTime())) {
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_REVOKE_OLD,
                                        indent, userId);
                                modified = PGPPublicKey.removeCertification(modified, userId, zert);
                                redundantCerts += 1;
                                continue;
                            }
                            // first revocation? remember it.
                            if (revocation == null) {
                                revocation = zert;
                                // more revocations? at least one is superfluous, then.
                            } else if (revocation.getCreationTime().before(cert.getCreationTime())) {
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_REVOKE_DUP,
                                        indent, userId);
                                modified = PGPPublicKey.removeCertification(modified, userId, revocation);
                                redundantCerts += 1;
                                revocation = zert;
                            } else {
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_REVOKE_DUP,
                                        indent, userId);
                                modified = PGPPublicKey.removeCertification(modified, userId, zert);
                                redundantCerts += 1;
                            }
                            break;

                    }

                }

                // If no valid certificate (if only a revocation) remains, drop it
                if (selfCert == null && revocation == null) {
                    log.add(LogLevel.ERROR, LogType.MSG_KC_UID_REMOVE,
                            indent, userId);
                    modified = PGPPublicKey.removeCertification(modified, userId);
                }
            }

            // If NO user ids remain, error out!
            if (!modified.getUserIDs().hasNext()) {
                log.add(LogLevel.ERROR, LogType.MSG_KC_ERROR_NO_UID, indent);
                return null;
            }

            // Replace modified key in the keyring
            ring = replacePublicKey(ring, modified);
            if (ring == null) {
                log.add(LogLevel.ERROR, LogType.MSG_MG_ERROR_SECRET_DUMMY, indent);
                return null;
            }
            indent -= 1;

        }

        // Process all keys
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(ring.getPublicKeys())) {
            // Don't care about the master key here, that one gets special treatment above
            if (key.isMasterKey()) {
                continue;
            }
            log.add(LogLevel.DEBUG, LogType.MSG_KC_SUB,
                    indent, PgpKeyHelper.convertKeyIdToHex(key.getKeyID()));
            indent += 1;

            if (Arrays.binarySearch(KNOWN_ALGORITHMS, key.getAlgorithm()) < 0) {
                ring = removeSubKey(ring, key);

                log.add(LogLevel.ERROR, LogType.MSG_KC_SUB_UNKNOWN_ALGO, indent,
                        Integer.toString(key.getAlgorithm()));
                indent -= 1;
                continue;
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
                    log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD_KEYID, indent);
                    badCerts += 1;
                    continue;
                }

                if (type != PGPSignature.SUBKEY_BINDING && type != PGPSignature.SUBKEY_REVOCATION) {
                    log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD_TYPE, indent, "0x" + Integer.toString(type, 16));
                    badCerts += 1;
                    continue;
                }

                if (cert.getCreationTime().after(now)) {
                    // Creation date in the future? No way!
                    log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD_TIME, indent);
                    badCerts += 1;
                    continue;
                }

                if (cert.isLocal()) {
                    // Creation date in the future? No way!
                    log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD_LOCAL, indent);
                    badCerts += 1;
                    continue;
                }

                if (type == PGPSignature.SUBKEY_BINDING) {

                    // make sure the certificate checks out
                    try {
                        cert.init(masterKey);
                        if (!cert.verifySignature(masterKey, key)) {
                            log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD, indent);
                            badCerts += 1;
                            continue;
                        }
                    } catch (PgpGeneralException e) {
                        log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD_ERR, indent);
                        badCerts += 1;
                        continue;
                    }

                    // if this certificate says it allows signing for the key
                    if (zert.getHashedSubPackets() != null &&
                            zert.getHashedSubPackets().hasSubpacket(SignatureSubpacketTags.KEY_FLAGS)) {

                        int flags = ((KeyFlags) zert.getHashedSubPackets()
                                .getSubpacket(SignatureSubpacketTags.KEY_FLAGS)).getFlags();
                        if ((flags & PGPKeyFlags.CAN_SIGN) == PGPKeyFlags.CAN_SIGN) {
                            boolean ok = false;
                            // it MUST have an embedded primary key binding signature
                            try {
                                PGPSignatureList list = zert.getUnhashedSubPackets().getEmbeddedSignatures();
                                for (int i = 0; i < list.size(); i++) {
                                    WrappedSignature subsig = new WrappedSignature(list.get(i));
                                    if (subsig.getSignatureType() == PGPSignature.PRIMARYKEY_BINDING) {
                                        subsig.init(key);
                                        if (subsig.verifySignature(masterKey, key)) {
                                            ok = true;
                                        } else {
                                            log.add(LogLevel.WARN, LogType.MSG_KC_SUB_PRIMARY_BAD, indent);
                                            badCerts += 1;
                                            continue uids;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                log.add(LogLevel.WARN, LogType.MSG_KC_SUB_PRIMARY_BAD_ERR, indent);
                                badCerts += 1;
                                continue;
                            }
                            // if it doesn't, get rid of this!
                            if (!ok) {
                                log.add(LogLevel.WARN, LogType.MSG_KC_SUB_PRIMARY_NONE, indent);
                                badCerts += 1;
                                continue;
                            }
                        }

                    }

                    // if we already have a cert, and this one is older: skip it
                    if (selfCert != null && cert.getCreationTime().before(selfCert.getCreationTime())) {
                        log.add(LogLevel.DEBUG, LogType.MSG_KC_SUB_DUP, indent);
                        redundantCerts += 1;
                        continue;
                    }

                    selfCert = zert;
                    // if this is newer than a possibly existing revocation, drop that one
                    if (revocation != null && selfCert.getCreationTime().after(revocation.getCreationTime())) {
                        log.add(LogLevel.DEBUG, LogType.MSG_KC_SUB_REVOKE_DUP, indent);
                        redundantCerts += 1;
                        revocation = null;
                    }

                // it must be a revocation, then (we made sure above)
                } else {

                    // make sure the certificate checks out
                    try {
                        cert.init(masterKey);
                        if (!cert.verifySignature(masterKey, key)) {
                            log.add(LogLevel.WARN, LogType.MSG_KC_SUB_REVOKE_BAD, indent);
                            badCerts += 1;
                            continue;
                        }
                    } catch (PgpGeneralException e) {
                        log.add(LogLevel.WARN, LogType.MSG_KC_SUB_REVOKE_BAD_ERR, indent);
                        badCerts += 1;
                        continue;
                    }

                    // if there is a certification that is newer than this revocation, don't bother
                    if (selfCert != null && selfCert.getCreationTime().after(cert.getCreationTime())) {
                        log.add(LogLevel.DEBUG, LogType.MSG_KC_SUB_REVOKE_DUP, indent);
                        redundantCerts += 1;
                        continue;
                    }

                    revocation = zert;
                }
            }

            // it is not properly bound? error!
            if (selfCert == null) {
                ring = removeSubKey(ring, key);

                log.add(LogLevel.ERROR, LogType.MSG_KC_SUB_NO_CERT,
                        indent, PgpKeyHelper.convertKeyIdToHex(key.getKeyID()));
                indent -= 1;
                continue;
            }

            // re-add certification
            modified = PGPPublicKey.addCertification(modified, selfCert);
            // add revocation, if any
            if (revocation != null) {
                modified = PGPPublicKey.addCertification(modified, revocation);
            }
            // replace pubkey in keyring
            ring = replacePublicKey(ring, modified);
            if (ring == null) {
                log.add(LogLevel.ERROR, LogType.MSG_MG_ERROR_SECRET_DUMMY, indent);
                return null;
            }
            indent -= 1;
        }

        if (badCerts > 0 && redundantCerts > 0) {
            // multi plural would make this complex, just leaving this as is...
            log.add(LogLevel.OK, LogType.MSG_KC_SUCCESS_BAD_AND_RED,
                    indent, Integer.toString(badCerts), Integer.toString(redundantCerts));
        } else if (badCerts > 0) {
            log.add(LogLevel.OK, LogType.MSG_KC_SUCCESS_BAD,
                    indent, badCerts);
        } else if (redundantCerts > 0) {
            log.add(LogLevel.OK, LogType.MSG_KC_SUCCESS_REDUNDANT,
                    indent, redundantCerts);
        } else {
            log.add(LogLevel.OK, LogType.MSG_KC_SUCCESS, indent);
        }

        return isSecret() ? new CanonicalizedSecretKeyRing((PGPSecretKeyRing) ring, 1)
                          : new CanonicalizedPublicKeyRing((PGPPublicKeyRing) ring, 0);
    }

    /** This operation merges information from a different keyring, returning a combined
     * UncachedKeyRing.
     *
     * The combined keyring contains the subkeys and user ids of both input keyrings, but it does
     * not necessarily have the canonicalized property.
     *
     * @param other The UncachedKeyRing to merge. Must not be empty, and of the same masterKeyId
     * @return A consolidated UncachedKeyRing with the data of both input keyrings. Same type as
     * this object, or null on error.
     *
     */
    public UncachedKeyRing merge(UncachedKeyRing other, OperationLog log, int indent) {

        log.add(LogLevel.DEBUG, isSecret() ? LogType.MSG_MG_SECRET : LogType.MSG_MG_PUBLIC,
                indent, PgpKeyHelper.convertKeyIdToHex(getMasterKeyId()));
        indent += 1;

        long masterKeyId = other.getMasterKeyId();

        if (getMasterKeyId() != masterKeyId
                || !Arrays.equals(getFingerprint(), other.getFingerprint())) {
            log.add(LogLevel.ERROR, LogType.MSG_MG_ERROR_HETEROGENEOUS, indent);
            return null;
        }

        // remember which certs we already added. this is cheaper than semantic deduplication
        Set<byte[]> certs = new TreeSet<byte[]>(new Comparator<byte[]>() {
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
                    log.add(LogLevel.DEBUG, LogType.MSG_MG_NEW_SUBKEY, indent);
                    // special case: if both rings are secret, copy over the secret key
                    if (isSecret() && other.isSecret()) {
                        PGPSecretKey sKey = ((PGPSecretKeyRing) candidate).getSecretKey(key.getKeyID());
                        result = PGPSecretKeyRing.insertSecretKey((PGPSecretKeyRing) result, sKey);
                    } else {
                        // otherwise, just insert the public key
                        result = replacePublicKey(result, key);
                        if (result == null) {
                            log.add(LogLevel.ERROR, LogType.MSG_MG_ERROR_SECRET_DUMMY, indent);
                            return null;
                        }
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
                        if (result == null) {
                            log.add(LogLevel.ERROR, LogType.MSG_MG_ERROR_SECRET_DUMMY, indent);
                            return null;
                        }
                    }
                    continue;
                }

                // Copy over all user id certificates
                for (String userId : new IterableIterator<String>(key.getUserIDs())) {
                    for (PGPSignature cert : new IterableIterator<PGPSignature>(key.getSignaturesForID(userId))) {
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
                        modified = PGPPublicKey.addCertification(modified, userId, cert);
                    }
                }
                // If anything changed, save the updated (sub)key
                if (modified != resultKey) {
                    result = replacePublicKey(result, modified);
                    if (result == null) {
                        log.add(LogLevel.ERROR, LogType.MSG_MG_ERROR_SECRET_DUMMY, indent);
                        return null;
                    }
                }

            }

            if (newCerts > 0) {
                log.add(LogLevel.DEBUG, LogType.MSG_MG_FOUND_NEW, indent,
                        Integer.toString(newCerts));
            } else {
                log.add(LogLevel.DEBUG, LogType.MSG_MG_UNCHANGED, indent);
            }

            return new UncachedKeyRing(result);

        } catch (IOException e) {
            log.add(LogLevel.ERROR, LogType.MSG_MG_ERROR_ENCODE, indent);
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
            // TODO generate secret key with S2K dummy, if none exists!
            if (sKey == null) {
                Log.e(Constants.TAG, "dummy secret key generation not yet implemented");
                return null;
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

}

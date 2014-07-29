package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.S2K;
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
import org.sufficientlysecure.keychain.service.OperationResultParcel.OperationLog;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogLevel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogType;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.Vector;

/** Wrapper around PGPKeyRing class, to be constructed from bytes.
 *
 * This class and its relatives UncachedPublicKey and UncachedSecretKey are
 * used to move around pgp key rings in non crypto related (UI, mostly) code.
 * It should be used for simple inspection only until it saved in the database,
 * all actual crypto operations should work with WrappedKeyRings exclusively.
 *
 * This class is also special in that it can hold either the PGPPublicKeyRing
 * or PGPSecretKeyRing derivate of the PGPKeyRing class, since these are
 * treated equally for most purposes in UI code. It is up to the programmer to
 * take care of the differences.
 *
 * @see org.sufficientlysecure.keychain.pgp.WrappedKeyRing
 * @see org.sufficientlysecure.keychain.pgp.UncachedPublicKey
 * @see org.sufficientlysecure.keychain.pgp.UncachedSecretKey
 *
 */
@SuppressWarnings("unchecked")
public class UncachedKeyRing {

    final PGPKeyRing mRing;
    final boolean mIsSecret;
    final boolean mIsCanonicalized;

    UncachedKeyRing(PGPKeyRing ring) {
        mRing = ring;
        mIsSecret = ring instanceof PGPSecretKeyRing;
        mIsCanonicalized = false;
    }

    private UncachedKeyRing(PGPKeyRing ring, boolean canonicalized) {
        mRing = ring;
        mIsSecret = ring instanceof PGPSecretKeyRing;
        mIsCanonicalized = canonicalized;
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

    public boolean isCanonicalized() {
        return mIsCanonicalized;
    }

    public byte[] getEncoded() throws IOException {
        return mRing.getEncoded();
    }

    public byte[] getFingerprint() {
        return mRing.getPublicKey().getFingerprint();
    }

    public static UncachedKeyRing decodeFromData(byte[] data)
            throws PgpGeneralException, IOException {

        List<UncachedKeyRing> parsed = fromStream(new ByteArrayInputStream(data));

        if (parsed.isEmpty()) {
            throw new PgpGeneralException("Object not recognized as PGPKeyRing!");
        }
        if (parsed.size() > 1) {
            throw new PgpGeneralException(
                    "Expected single keyring in stream, found " + parsed.size());
        }

        return parsed.get(0);

    }

    public static List<UncachedKeyRing> fromStream(InputStream stream)
            throws PgpGeneralException, IOException {

        List<UncachedKeyRing> result = new Vector<UncachedKeyRing>();

        while(stream.available() > 0) {
            PGPObjectFactory objectFactory = new PGPObjectFactory(PGPUtil.getDecoderStream(stream));

            // go through all objects in this block
            Object obj;
            while ((obj = objectFactory.nextObject()) != null) {
                Log.d(Constants.TAG, "Found class: " + obj.getClass());
                if (!(obj instanceof PGPKeyRing)) {
                    throw new PgpGeneralException(
                            "Bad object of type " + obj.getClass().getName() + " in stream!");
                }
                result.add(new UncachedKeyRing((PGPKeyRing) obj));
            }
        }

        return result;
    }

    public void encodeArmored(OutputStream out, String version) throws IOException {
        ArmoredOutputStream aos = new ArmoredOutputStream(out);
        aos.setHeader("Version", version);
        aos.write(mRing.getEncoded());
        aos.close();
    }

    public HashSet<Long> getAvailableSubkeys() {
        if(!isSecret()) {
            throw new RuntimeException("Tried to find available subkeys from non-secret keys. " +
                    "This is a programming error and should never happen!");
        }

        HashSet<Long> result = new HashSet<Long>();
        // then, mark exactly the keys we have available
        for (PGPSecretKey sub : new IterableIterator<PGPSecretKey>(
                ((PGPSecretKeyRing) mRing).getSecretKeys())) {
            S2K s2k = sub.getS2K();
            // Set to 1, except if the encryption type is GNU_DUMMY_S2K
            if(s2k == null || s2k.getType() != S2K.GNU_DUMMY_S2K) {
                result.add(sub.getKeyID());
            }
        }
        return result;
    }

    /** "Canonicalizes" a public key, removing inconsistencies in the process. This variant can be
     * applied to public keyrings only.
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
     * This operation writes an OperationLog which can be used as part of a OperationResultParcel.
     *
     * @return A canonicalized key, or null on fatal error
     *
     */
    @SuppressWarnings("ConstantConditions")
    public UncachedKeyRing canonicalize(OperationLog log, int indent) {

        log.add(LogLevel.START, isSecret() ? LogType.MSG_KC_SECRET : LogType.MSG_KC_PUBLIC,
                indent, PgpKeyHelper.convertKeyIdToHex(getMasterKeyId()));
        indent += 1;

        final Date now = new Date();

        int redundantCerts = 0, badCerts = 0;

        PGPKeyRing ring = mRing;
        PGPPublicKey masterKey = mRing.getPublicKey();
        final long masterKeyId = masterKey.getKeyID();

        {
            log.add(LogLevel.DEBUG, LogType.MSG_KC_MASTER,
                    indent, PgpKeyHelper.convertKeyIdToHex(masterKey.getKeyID()));
            indent += 1;

            PGPPublicKey modified = masterKey;
            PGPSignature revocation = null;
            for (PGPSignature zert : new IterableIterator<PGPSignature>(masterKey.getKeySignatures())) {
                int type = zert.getSignatureType();

                // Disregard certifications on user ids, we will deal with those later
                if (type == PGPSignature.NO_CERTIFICATION
                        || type == PGPSignature.DEFAULT_CERTIFICATION
                        || type == PGPSignature.CASUAL_CERTIFICATION
                        || type == PGPSignature.POSITIVE_CERTIFICATION
                        || type == PGPSignature.CERTIFICATION_REVOCATION) {
                    // These should not be here...
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
                    modified = PGPPublicKey.removeCertification(modified, revocation);
                    redundantCerts += 1;
                    log.add(LogLevel.INFO, LogType.MSG_KC_REVOKE_DUP, indent);
                    revocation = zert;
                } else {
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    redundantCerts += 1;
                    log.add(LogLevel.INFO, LogType.MSG_KC_REVOKE_DUP, indent);
                }
            }

            for (String userId : new IterableIterator<String>(masterKey.getUserIDs())) {
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
                    }

                    if (cert.getCreationTime().after(now)) {
                        // Creation date in the future? No way!
                        log.add(LogLevel.WARN, LogType.MSG_KC_UID_BAD_TIME, indent);
                        modified = PGPPublicKey.removeCertification(modified, zert);
                        badCerts += 1;
                        continue;
                    }

                    if (cert.isLocal()) {
                        // Creation date in the future? No way!
                        log.add(LogLevel.WARN, LogType.MSG_KC_UID_BAD_LOCAL, indent);
                        modified = PGPPublicKey.removeCertification(modified, zert);
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
                                modified = PGPPublicKey.removeCertification(modified, userId, selfCert);
                                redundantCerts += 1;
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_DUP,
                                        indent, userId);
                                selfCert = zert;
                            } else {
                                modified = PGPPublicKey.removeCertification(modified, userId, zert);
                                redundantCerts += 1;
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_DUP,
                                        indent, userId);
                            }
                            // If there is a revocation certificate, and it's older than this, drop it
                            if (revocation != null
                                    && revocation.getCreationTime().before(selfCert.getCreationTime())) {
                                modified = PGPPublicKey.removeCertification(modified, userId, revocation);
                                revocation = null;
                                redundantCerts += 1;
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_REVOKE_OLD,
                                        indent, userId);
                            }
                            break;

                        case PGPSignature.CERTIFICATION_REVOCATION:
                            // If this is older than the (latest) self cert, drop it
                            if (selfCert != null && selfCert.getCreationTime().after(zert.getCreationTime())) {
                                modified = PGPPublicKey.removeCertification(modified, userId, zert);
                                redundantCerts += 1;
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_REVOKE_OLD,
                                        indent, userId);
                                continue;
                            }
                            // first revocation? remember it.
                            if (revocation == null) {
                                revocation = zert;
                                // more revocations? at least one is superfluous, then.
                            } else if (revocation.getCreationTime().before(cert.getCreationTime())) {
                                modified = PGPPublicKey.removeCertification(modified, userId, revocation);
                                redundantCerts += 1;
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_REVOKE_DUP,
                                        indent, userId);
                                revocation = zert;
                            } else {
                                modified = PGPPublicKey.removeCertification(modified, userId, zert);
                                redundantCerts += 1;
                                log.add(LogLevel.DEBUG, LogType.MSG_KC_UID_REVOKE_DUP,
                                        indent, userId);
                            }
                            break;

                    }

                }

                // If no valid certificate (if only a revocation) remains, drop it
                if (selfCert == null && revocation == null) {
                    modified = PGPPublicKey.removeCertification(modified, userId);
                    log.add(LogLevel.ERROR, LogType.MSG_KC_UID_REMOVE,
                            indent, userId);
                }
            }

            // If NO user ids remain, error out!
            if (!modified.getUserIDs().hasNext()) {
                log.add(LogLevel.ERROR, LogType.MSG_KC_FATAL_NO_UID, indent);
                return null;
            }

            // Replace modified key in the keyring
            ring = replacePublicKey(ring, modified);
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

                    // if we already have a cert, and this one is not newer: skip it
                    if (selfCert != null && selfCert.getCreationTime().before(cert.getCreationTime())) {
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

        return new UncachedKeyRing(ring, true);
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

        if (getMasterKeyId() != masterKeyId) {
            log.add(LogLevel.ERROR, LogType.MSG_MG_HETEROGENEOUS, indent);
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
            log.add(LogLevel.ERROR, LogType.MSG_MG_FATAL_ENCODE, indent);
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
            return PGPPublicKeyRing.insertPublicKey((PGPPublicKeyRing) ring, key);
        }
        PGPSecretKeyRing secRing = (PGPSecretKeyRing) ring;
        PGPSecretKey sKey = secRing.getSecretKey(key.getKeyID());
        // TODO generate secret key with S2K dummy, if none exists! for now, just die.
        if (sKey == null) {
            throw new RuntimeException("dummy secret key generation not yet implemented");
        }
        sKey = PGPSecretKey.replacePublicKey(sKey, key);
        return PGPSecretKeyRing.insertSecretKey(secRing, sKey);
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

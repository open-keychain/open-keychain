package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.S2K;
import org.spongycastle.openpgp.PGPKeyRing;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.service.OperationResultParcel.OperationLog;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogLevel;
import org.sufficientlysecure.keychain.service.OperationResultParcel.LogType;
import org.sufficientlysecure.keychain.util.IterableIterator;
import org.sufficientlysecure.keychain.util.Log;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

    /* TODO don't use this */
    @Deprecated
    public PGPKeyRing getRing() {
        return mRing;
    }

    public UncachedPublicKey getPublicKey() {
        return new UncachedPublicKey(mRing.getPublicKey());
    }

    public Iterator<UncachedPublicKey> getPublicKeys() {
        final Iterator<PGPPublicKey> it = mRing.getPublicKeys();
        return new Iterator<UncachedPublicKey>() {
            public void remove() {
                it.remove();
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

    public static UncachedKeyRing decodePublicFromData(byte[] data)
            throws PgpGeneralException, IOException {
        UncachedKeyRing ring = decodeFromData(data);
        if(ring.isSecret()) {
            throw new PgpGeneralException("Object not recognized as PGPPublicKeyRing!");
        }
        return ring;
    }

    public static UncachedKeyRing decodeFromData(byte[] data)
            throws PgpGeneralException, IOException {
        BufferedInputStream bufferedInput =
                new BufferedInputStream(new ByteArrayInputStream(data));
        if (bufferedInput.available() > 0) {
            InputStream in = PGPUtil.getDecoderStream(bufferedInput);
            PGPObjectFactory objectFactory = new PGPObjectFactory(in);

            // get first object in block
            Object obj;
            if ((obj = objectFactory.nextObject()) != null && obj instanceof PGPKeyRing) {
                return new UncachedKeyRing((PGPKeyRing) obj);
            } else {
                throw new PgpGeneralException("Object not recognized as PGPKeyRing!");
            }
        } else {
            throw new PgpGeneralException("Object not recognized as PGPKeyRing!");
        }
    }

    public static List<UncachedKeyRing> fromStream(InputStream stream)
            throws PgpGeneralException, IOException {

        PGPObjectFactory objectFactory = new PGPObjectFactory(PGPUtil.getDecoderStream(stream));

        List<UncachedKeyRing> result = new Vector<UncachedKeyRing>();

        // go through all objects in this block
        Object obj;
        while ((obj = objectFactory.nextObject()) != null) {
            Log.d(Constants.TAG, "Found class: " + obj.getClass());

            if (obj instanceof PGPKeyRing) {
                result.add(new UncachedKeyRing((PGPKeyRing) obj));
            } else {
                Log.e(Constants.TAG, "Object not recognized as PGPKeyRing!");
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

    /** "Canonicalizes" a key, removing inconsistencies in the process. This operation can be
     * applied to public keyrings only.
     *
     * More specifically:
     *  - Remove all non-verifying self-certificates
     *  - Remove all certificates flagged as "local"
     *  - Remove all certificates which are superseded by a newer one on the same target
     *
     * After this cleaning, a number of checks are done: TODO implement
     *  - See if each subkey retains a valid self certificate
     *  - See if each user id retains a valid self certificate
     *
     * This operation writes an OperationLog which can be used as part of a OperationResultParcel.
     *
     * @return A canonicalized key
     *
     */
    public UncachedKeyRing canonicalize(OperationLog log, int indent) {
        if (isSecret()) {
            throw new RuntimeException("Tried to canonicalize non-secret keyring. " +
                    "This is a programming error and should never happen!");
        }

        log.add(LogLevel.START, LogType.MSG_KC,
                new String[]{PgpKeyHelper.convertKeyIdToHex(getMasterKeyId())}, indent);
        indent += 1;

        int removedCerts = 0;

        PGPPublicKeyRing ring = (PGPPublicKeyRing) mRing;
        PGPPublicKey masterKey = mRing.getPublicKey();
        final long masterKeyId = masterKey.getKeyID();

        {
            log.add(LogLevel.DEBUG, LogType.MSG_KC_MASTER,
                    new String[]{PgpKeyHelper.convertKeyIdToHex(masterKey.getKeyID())}, indent);
            indent += 1;

            PGPPublicKey modified = masterKey;
            PGPSignature revocation = null;
            for (PGPSignature zert : new IterableIterator<PGPSignature>(masterKey.getSignatures())) {
                int type = zert.getSignatureType();
                // Disregard certifications on user ids, we will deal with those later
                if (type == PGPSignature.NO_CERTIFICATION
                        || type == PGPSignature.DEFAULT_CERTIFICATION
                        || type == PGPSignature.CASUAL_CERTIFICATION
                        || type == PGPSignature.POSITIVE_CERTIFICATION
                        || type == PGPSignature.CERTIFICATION_REVOCATION) {
                    continue;
                }
                WrappedSignature cert = new WrappedSignature(zert);

                if (type != PGPSignature.KEY_REVOCATION) {
                    // Unknown type, just remove
                    log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD_TYPE, new String[]{
                            "0x" + Integer.toString(type, 16)
                    }, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    removedCerts += 1;
                    continue;
                }

                try {
                    cert.init(masterKey);
                    if (!cert.verifySignature(masterKey)) {
                        log.add(LogLevel.WARN, LogType.MSG_KC_REVOKE_BAD, null, indent);
                        modified = PGPPublicKey.removeCertification(modified, zert);
                        removedCerts += 1;
                        continue;
                    }
                } catch (PgpGeneralException e) {
                    log.add(LogLevel.WARN, LogType.MSG_KC_REVOKE_BAD_ERR, null, indent);
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    removedCerts += 1;
                    continue;
                }

                // first revocation? fine then.
                if (revocation == null) {
                    revocation = zert;
                    // more revocations? at least one is superfluous, then.
                } else if (revocation.getCreationTime().before(zert.getCreationTime())) {
                    modified = PGPPublicKey.removeCertification(modified, revocation);
                    removedCerts += 1;
                    log.add(LogLevel.INFO, LogType.MSG_KC_REVOKE_DUP, null, indent);
                    revocation = zert;
                } else {
                    modified = PGPPublicKey.removeCertification(modified, zert);
                    removedCerts += 1;
                    log.add(LogLevel.INFO, LogType.MSG_KC_REVOKE_DUP, null, indent);
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

                    // If this is a foreign signature, never mind
                    if (certId != masterKeyId) {
                        continue;
                    }

                    // Otherwise, first make sure it checks out
                    try {
                        cert.init(masterKey);
                        if (!cert.verifySignature(masterKey, userId)) {
                            log.add(LogLevel.WARN, LogType.MSG_KC_UID_BAD,
                                    new String[] { userId }, indent);
                            modified = PGPPublicKey.removeCertification(modified, userId, zert);
                            removedCerts += 1;
                            continue;
                        }
                    } catch (PgpGeneralException e) {
                        log.add(LogLevel.WARN, LogType.MSG_KC_UID_BAD_ERR,
                                new String[] { userId }, indent);
                        modified = PGPPublicKey.removeCertification(modified, userId, zert);
                        removedCerts += 1;
                        continue;
                    }

                    switch (zert.getSignatureType()) {
                        case PGPSignature.DEFAULT_CERTIFICATION:
                        case PGPSignature.NO_CERTIFICATION:
                        case PGPSignature.CASUAL_CERTIFICATION:
                        case PGPSignature.POSITIVE_CERTIFICATION:
                            if (selfCert == null) {
                                selfCert = zert;
                            } else if (selfCert.getCreationTime().before(cert.getCreationTime())) {
                                modified = PGPPublicKey.removeCertification(modified, userId, selfCert);
                                removedCerts += 1;
                                log.add(LogLevel.INFO, LogType.MSG_KC_UID_DUP,
                                        new String[] { userId }, indent);
                                selfCert = zert;
                            } else {
                                modified = PGPPublicKey.removeCertification(modified, userId, zert);
                                removedCerts += 1;
                                log.add(LogLevel.INFO, LogType.MSG_KC_UID_DUP,
                                        new String[] { userId }, indent);
                            }
                            // If there is a revocation certificate, and it's older than this, drop it
                            if (revocation != null
                                    && revocation.getCreationTime().before(selfCert.getCreationTime())) {
                                modified = PGPPublicKey.removeCertification(modified, userId, revocation);
                                revocation = null;
                                removedCerts += 1;
                                log.add(LogLevel.INFO, LogType.MSG_KC_UID_REVOKE_OLD,
                                        new String[] { userId }, indent);
                            }
                            break;

                        case PGPSignature.CERTIFICATION_REVOCATION:
                            // If this is older than the (latest) self cert, drop it
                            if (selfCert != null && selfCert.getCreationTime().after(zert.getCreationTime())) {
                                modified = PGPPublicKey.removeCertification(modified, userId, zert);
                                removedCerts += 1;
                                log.add(LogLevel.INFO, LogType.MSG_KC_UID_REVOKE_OLD,
                                        new String[] { userId }, indent);
                                continue;
                            }
                            // first revocation? remember it.
                            if (revocation == null) {
                                revocation = zert;
                                // more revocations? at least one is superfluous, then.
                            } else if (revocation.getCreationTime().before(cert.getCreationTime())) {
                                modified = PGPPublicKey.removeCertification(modified, userId, revocation);
                                removedCerts += 1;
                                log.add(LogLevel.INFO, LogType.MSG_KC_UID_REVOKE_DUP,
                                        new String[] { userId }, indent);
                                revocation = zert;
                            } else {
                                modified = PGPPublicKey.removeCertification(modified, userId, zert);
                                removedCerts += 1;
                                log.add(LogLevel.INFO, LogType.MSG_KC_UID_REVOKE_DUP,
                                        new String[] { userId }, indent);
                            }
                            break;

                        default:
                            log.add(LogLevel.WARN, LogType.MSG_KC_UID_UNKNOWN_CERT,
                                    new String[] {
                                            "0x" + Integer.toString(zert.getSignatureType(), 16),
                                            userId
                                    }, indent);
                            modified = PGPPublicKey.removeCertification(modified, userId, zert);
                            removedCerts += 1;
                    }

                }
            }

            // Replace modified key in the keyring
            ring = PGPPublicKeyRing.insertPublicKey(ring, modified);

            log.add(LogLevel.DEBUG, LogType.MSG_KC_MASTER_SUCCESS, null, indent);
            indent -= 1;

        }

        // Process all keys
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(ring.getPublicKeys())) {
            // Don't care about the master key here, that one gets special treatment above
            if (key.isMasterKey()) {
                continue;
            }
            log.add(LogLevel.DEBUG, LogType.MSG_KC_SUB,
                    new String[]{PgpKeyHelper.convertKeyIdToHex(key.getKeyID())}, indent);
            indent += 1;
            // A subkey needs exactly one subkey binding certificate, and optionally one revocation
            // certificate.
            PGPPublicKey modified = key;
            PGPSignature selfCert = null, revocation = null;
            for (PGPSignature zig : new IterableIterator<PGPSignature>(key.getSignatures())) {
                // remove from keyring (for now)
                modified = PGPPublicKey.removeCertification(modified, zig);
                WrappedSignature cert = new WrappedSignature(zig);
                int type = cert.getSignatureType();

                // filter out bad key types...
                if (cert.getKeyId() != masterKey.getKeyID()) {
                    log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD_KEYID, null, indent);
                    continue;
                }
                if (type != PGPSignature.SUBKEY_BINDING && type != PGPSignature.SUBKEY_REVOCATION) {
                    log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD_TYPE, new String[]{
                            "0x" + Integer.toString(type, 16)
                    }, indent);
                    continue;
                }

                if (type == PGPSignature.SUBKEY_BINDING) {
                    // TODO verify primary key binding signature for signing keys!

                    // make sure the certificate checks out
                    try {
                        cert.init(masterKey);
                        if (!cert.verifySignature(masterKey, key)) {
                            log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD, null, indent);
                            log.add(LogLevel.WARN, LogType.MSG_KC_SUB, new String[] {
                                cert.getCreationTime().toString()
                            }, indent);
                            continue;
                        }
                    } catch (PgpGeneralException e) {
                        log.add(LogLevel.WARN, LogType.MSG_KC_SUB_BAD_ERR, null, indent);
                        continue;
                    }

                    // if we already have a cert, and this one is not newer: skip it
                    if (selfCert != null && selfCert.getCreationTime().before(cert.getCreationTime())) {
                        continue;
                    }
                    selfCert = zig;
                    // if this is newer than a possibly existing revocation, drop that one
                    if (revocation != null && selfCert.getCreationTime().after(revocation.getCreationTime())) {
                        revocation = null;
                    }

                // it must be a revocation, then (we made sure above)
                } else {

                    // make sure the certificate checks out
                    try {
                        cert.init(masterKey);
                        if (!cert.verifySignature(key)) {
                            log.add(LogLevel.WARN, LogType.MSG_KC_SUB_REVOKE_BAD, null, indent);
                            continue;
                        }
                    } catch (PgpGeneralException e) {
                        log.add(LogLevel.WARN, LogType.MSG_KC_SUB_REVOKE_BAD_ERR, null, indent);
                        continue;
                    }

                    // if there is no binding (yet), or the revocation is newer than the binding: keep it
                    if (selfCert == null || selfCert.getCreationTime().before(cert.getCreationTime())) {
                        revocation = zig;
                    }
                }
            }

            // it is not properly bound? error!
            if (selfCert == null) {
                ring = PGPPublicKeyRing.removePublicKey(ring, modified);

                log.add(LogLevel.ERROR, LogType.MSG_KC_SUB_NO_CERT,
                        new String[]{PgpKeyHelper.convertKeyIdToHex(key.getKeyID())}, indent);
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
            ring = PGPPublicKeyRing.insertPublicKey(ring, modified);

            log.add(LogLevel.DEBUG, LogType.MSG_KC_SUB_SUCCESS, null, indent);
            indent -= 1;
        }

        if (removedCerts > 0) {
            log.add(LogLevel.OK, LogType.MSG_KC_SUCCESS_REMOVED,
                    new String[] { Integer.toString(removedCerts) }, indent);
        } else {
            log.add(LogLevel.OK, LogType.MSG_KC_SUCCESS, null, indent);
        }

        return new UncachedKeyRing(ring);
    }


}

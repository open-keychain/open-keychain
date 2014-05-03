package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPOnePassSignature;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.spongycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class CachedPublicKey {

    // this is the parent key ring
    final CachedKeyRing mRing;

    private final PGPPublicKey mKey;

    CachedPublicKey(CachedKeyRing ring, PGPPublicKey key) {
        mRing = ring;
        mKey = key;
    }

    public long getKeyId() {
        return mKey.getKeyID();
    }

    public boolean isRevoked() {
        return mKey.isRevoked();
    }

    public Date getCreationTime() {
        return mKey.getCreationTime();
    }

    public Date getExpiryTime() {
        Date creationDate = getCreationTime();
        if (mKey.getValidDays() == 0) {
            // no expiry
            return null;
        }
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.DATE, mKey.getValidDays());

        return calendar.getTime();
    }

    public boolean isExpired() {
        Date creationDate = mKey.getCreationTime();
        Date expiryDate = mKey.getValidSeconds() > 0
                ? new Date(creationDate.getTime() + mKey.getValidSeconds() * 1000) : null;

        Date now = new Date();
        return creationDate.after(now) || (expiryDate != null && expiryDate.before(now));
    }

    public boolean isMasterKey() {
        return mKey.isMasterKey();
    }

    public int getAlgorithm() {
        return mKey.getAlgorithm();
    }

    public IterableIterator<String> getUserIds() {
        return new IterableIterator<String>(mKey.getUserIDs());
    }

    private Integer mCacheUsage = null;
    @SuppressWarnings("unchecked")
    public int getKeyUsage() {
        if(mCacheUsage == null) {
            mCacheUsage = 0;
            if (mKey.getVersion() >= 4) {
                for (PGPSignature sig : new IterableIterator<PGPSignature>(mKey.getSignatures())) {
                    if (mKey.isMasterKey() && sig.getKeyID() != mKey.getKeyID()) {
                        continue;
                    }

                    PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();
                    if (hashed != null) {
                        mCacheUsage |= hashed.getKeyFlags();
                    }

                    PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();
                    if (unhashed != null) {
                        mCacheUsage |= unhashed.getKeyFlags();
                    }
                }
            }
        }
        return mCacheUsage;
    }

    public boolean canAuthenticate() {
        return mKey.getVersion() <= 3 || (getKeyUsage() & KeyFlags.AUTHENTICATION) != 0;
    }

    public boolean canCertify() {
        return mKey.getVersion() <= 3 || (getKeyUsage() & KeyFlags.CERTIFY_OTHER) != 0;
    }

    public boolean canEncrypt() {
        if (!mKey.isEncryptionKey()) {
            return false;
        }

        // special cases
        if (mKey.getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT) {
            return true;
        }

        if (mKey.getAlgorithm() == PGPPublicKey.RSA_ENCRYPT) {
            return true;
        }

        return mKey.getVersion() <= 3 ||
                (getKeyUsage() & (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0;

    }

    public boolean canSign() {
        // special case
        if (mKey.getAlgorithm() == PGPPublicKey.RSA_SIGN) {
            return true;
        }

        return mKey.getVersion() <= 3 || (getKeyUsage() & KeyFlags.SIGN_DATA) != 0;
    }

    public CachedKeyRing getKeyRing() {
        return mRing;
    }

    JcePublicKeyKeyEncryptionMethodGenerator getPubKeyEncryptionGenerator() {
        return  new JcePublicKeyKeyEncryptionMethodGenerator(mKey);
    }

    public void initSignature(PGPSignature sig) throws PGPException {
        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                new JcaPGPContentVerifierBuilderProvider()
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        sig.init(contentVerifierBuilderProvider, mKey);
    }

    public void initSignature(PGPOnePassSignature sig) throws PGPException {
        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                new JcaPGPContentVerifierBuilderProvider()
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        sig.init(contentVerifierBuilderProvider, mKey);
    }

    public byte[] getFingerprint() {
        return mKey.getFingerprint();
    }

    // Note that this method has package visibility - no access outside the pgp package!
    PGPPublicKey getKey() {
        return mKey;
    }
}

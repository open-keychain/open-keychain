package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

public class UncachedPublicKey {
    protected final PGPPublicKey mPublicKey;
    private Integer mCacheUsage = null;

    public UncachedPublicKey(PGPPublicKey key) {
        mPublicKey = key;
    }

    public long getKeyId() {
        return mPublicKey.getKeyID();
    }

    /** The revocation signature is NOT checked here, so this may be false! */
    public boolean maybeRevoked() {
        return mPublicKey.isRevoked();
    }

    public Date getCreationTime() {
        return mPublicKey.getCreationTime();
    }

    public Date getExpiryTime() {
        Date creationDate = getCreationTime();
        if (mPublicKey.getValidDays() == 0) {
            // no expiry
            return null;
        }
        Calendar calendar = GregorianCalendar.getInstance();
        calendar.setTime(creationDate);
        calendar.add(Calendar.DATE, mPublicKey.getValidDays());

        return calendar.getTime();
    }

    public boolean isExpired() {
        Date creationDate = mPublicKey.getCreationTime();
        Date expiryDate = mPublicKey.getValidSeconds() > 0
                ? new Date(creationDate.getTime() + mPublicKey.getValidSeconds() * 1000) : null;

        Date now = new Date();
        return creationDate.after(now) || (expiryDate != null && expiryDate.before(now));
    }

    public boolean isMasterKey() {
        return mPublicKey.isMasterKey();
    }

    public int getAlgorithm() {
        return mPublicKey.getAlgorithm();
    }

    public int getBitStrength() {
        return mPublicKey.getBitStrength();
    }

    public String getPrimaryUserId() {
        for (String userId : new IterableIterator<String>(mPublicKey.getUserIDs())) {
            for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignaturesForID(userId))) {
                if (sig.getHashedSubPackets() != null
                        && sig.getHashedSubPackets().hasSubpacket(SignatureSubpacketTags.PRIMARY_USER_ID)) {
                    try {
                        // make sure it's actually valid
                        sig.init(new JcaPGPContentVerifierBuilderProvider().setProvider(
                                Constants.BOUNCY_CASTLE_PROVIDER_NAME), mPublicKey);
                        if (sig.verifyCertification(userId, mPublicKey)) {
                            return userId;
                        }
                    } catch (Exception e) {
                        // nothing bad happens, the key is just not considered the primary key id
                    }
                }

            }
        }
        return null;
    }

    public ArrayList<String> getUnorderedUserIds() {
        ArrayList<String> userIds = new ArrayList<String>();
        for (String userId : new IterableIterator<String>(mPublicKey.getUserIDs())) {
            userIds.add(userId);
        }
        return userIds;
    }

    public boolean isElGamalEncrypt() {
        return getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT;
    }

    public boolean isDSA() {
        return getAlgorithm() == PGPPublicKey.DSA;
    }

    @SuppressWarnings("unchecked")
    public int getKeyUsage() {
        if(mCacheUsage == null) {
            mCacheUsage = 0;
            if (mPublicKey.getVersion() >= 4) {
                for (PGPSignature sig : new IterableIterator<PGPSignature>(mPublicKey.getSignatures())) {
                    if (mPublicKey.isMasterKey() && sig.getKeyID() != mPublicKey.getKeyID()) {
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
        return mPublicKey.getVersion() <= 3 || (getKeyUsage() & KeyFlags.AUTHENTICATION) != 0;
    }

    public boolean canCertify() {
        return mPublicKey.getVersion() <= 3 || (getKeyUsage() & KeyFlags.CERTIFY_OTHER) != 0;
    }

    public boolean canEncrypt() {
        if (!mPublicKey.isEncryptionKey()) {
            return false;
        }

        // special cases
        if (mPublicKey.getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT) {
            return true;
        }

        if (mPublicKey.getAlgorithm() == PGPPublicKey.RSA_ENCRYPT) {
            return true;
        }

        return mPublicKey.getVersion() <= 3 ||
                (getKeyUsage() & (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0;

    }

    public boolean canSign() {
        // special case
        if (mPublicKey.getAlgorithm() == PGPPublicKey.RSA_SIGN) {
            return true;
        }

        return mPublicKey.getVersion() <= 3 || (getKeyUsage() & KeyFlags.SIGN_DATA) != 0;
    }

    public byte[] getFingerprint() {
        return mPublicKey.getFingerprint();
    }

    // TODO This method should have package visibility - no access outside the pgp package!
    // (It's still used in ProviderHelper at this point)
    public PGPPublicKey getPublicKey() {
        return mPublicKey;
    }

    public Iterator<WrappedSignature> getSignaturesForId(String userId) {
        final Iterator<PGPSignature> it = mPublicKey.getSignaturesForID(userId);
        return new Iterator<WrappedSignature>() {
            public void remove() {
                it.remove();
            }
            public WrappedSignature next() {
                return new WrappedSignature(it.next());
            }
            public boolean hasNext() {
                return it.hasNext();
            }
        };
    }
}

package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.SignatureSubpacketTags;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.openpgp.PGPException;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.openpgp.PGPSignatureList;
import org.spongycastle.openpgp.PGPSignatureSubpacketVector;
import org.spongycastle.openpgp.operator.jcajce.JcaPGPContentVerifierBuilderProvider;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.util.IterableIterator;

import java.io.IOException;
import java.security.SignatureException;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;

public class CachedPublicKeyRing extends CachedKeyRing {

    private PGPPublicKeyRing mRing;
    private final byte[] mPubKey;

    public CachedPublicKeyRing(long masterKeyId, int keySize, boolean isRevoked,
                                boolean canCertify, long creation, long expiry, int algorithm,
                                byte[] fingerprint, String userId, int verified, boolean hasSecret,
                                byte[] pubkey)
    {
        super(masterKeyId, keySize, isRevoked, canCertify, creation, expiry,
                algorithm, fingerprint, userId, verified, hasSecret);

        mPubKey = pubkey;
    }

    private PGPPublicKeyRing getRing() {
        if(mRing == null) {
            mRing = (PGPPublicKeyRing) PgpConversionHelper.BytesToPGPKeyRing(mPubKey);
        }
        return mRing;
    }

    public void encode(ArmoredOutputStream stream) throws IOException {
        getRing().encode(stream);
    }

    public CachedPublicKey getSubkey(long id) {
        return new CachedPublicKey(this, getRing().getPublicKey(id));
    }

    public CachedPublicKey getFirstEncryptSubkey() throws PgpGeneralException {
        // only return master key if no other encryption key is available
        PGPPublicKey masterKey = null;
        for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(getRing().getPublicKeys())) {
            if (key.isRevoked() || !isEncryptionKey(key) || isExpired(key)) {
                continue;
            }
            if (key.isMasterKey()) {
                masterKey = key;
            } else {
                return new CachedPublicKey(this, key);
            }
        }
        if(masterKey == null) {
            // TODO proper exception
            throw new PgpGeneralException("key not found");
        }
        return new CachedPublicKey(this, masterKey);
    }

    public boolean verifySubkeyBinding(CachedPublicKey cachedSubkey) {
        boolean validSubkeyBinding = false;
        boolean validTempSubkeyBinding = false;
        boolean validPrimaryKeyBinding = false;

        PGPPublicKey masterKey = getRing().getPublicKey();
        PGPPublicKey subKey = cachedSubkey.getKey();

        // Is this the master key? Match automatically, then.
        if(Arrays.equals(masterKey.getFingerprint(), subKey.getFingerprint())) {
            return true;
        }

        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                new JcaPGPContentVerifierBuilderProvider()
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        Iterator<PGPSignature> itr = subKey.getSignatures();

        while (itr.hasNext()) { //what does gpg do if the subkey binding is wrong?
            //gpg has an invalid subkey binding error on key import I think, but doesn't shout
            //about keys without subkey signing. Can't get it to import a slightly broken one
            //either, so we will err on bad subkey binding here.
            PGPSignature sig = itr.next();
            if (sig.getKeyID() == masterKey.getKeyID() &&
                    sig.getSignatureType() == PGPSignature.SUBKEY_BINDING) {
                //check and if ok, check primary key binding.
                try {
                    sig.init(contentVerifierBuilderProvider, masterKey);
                    validTempSubkeyBinding = sig.verifyCertification(masterKey, subKey);
                } catch (PGPException e) {
                    continue;
                } catch (SignatureException e) {
                    continue;
                }

                if (validTempSubkeyBinding) {
                    validSubkeyBinding = true;
                }
                if (validTempSubkeyBinding) {
                    validPrimaryKeyBinding = verifyPrimaryKeyBinding(sig.getUnhashedSubPackets(),
                            masterKey, subKey);
                    if (validPrimaryKeyBinding) {
                        break;
                    }
                    validPrimaryKeyBinding = verifyPrimaryKeyBinding(sig.getHashedSubPackets(),
                            masterKey, subKey);
                    if (validPrimaryKeyBinding) {
                        break;
                    }
                }
            }
        }
        return validSubkeyBinding && validPrimaryKeyBinding;

    }


    static boolean isEncryptionKey(PGPPublicKey key) {
        if (!key.isEncryptionKey()) {
            return false;
        }

        if (key.getVersion() <= 3) {
            // this must be true now
            return key.isEncryptionKey();
        }

        // special cases
        if (key.getAlgorithm() == PGPPublicKey.ELGAMAL_ENCRYPT) {
            return true;
        }

        if (key.getAlgorithm() == PGPPublicKey.RSA_ENCRYPT) {
            return true;
        }

        for (PGPSignature sig : new IterableIterator<PGPSignature>(key.getSignatures())) {
            if (key.isMasterKey() && sig.getKeyID() != key.getKeyID()) {
                continue;
            }
            PGPSignatureSubpacketVector hashed = sig.getHashedSubPackets();

            if (hashed != null
                    && (hashed.getKeyFlags() & (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                return true;
            }

            PGPSignatureSubpacketVector unhashed = sig.getUnhashedSubPackets();

            if (unhashed != null
                    && (unhashed.getKeyFlags() & (KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE)) != 0) {
                return true;
            }
        }
        return false;
    }

    static boolean isExpired(PGPPublicKey key) {
        Date creationDate = key.getCreationTime();
        Date expiryDate = key.getValidSeconds() > 0
                ? new Date(creationDate.getTime() + key.getValidSeconds() * 1000) : null;

        Date now = new Date();
        return creationDate.after(now) || (expiryDate != null && expiryDate.before(now));
    }

    static boolean verifyPrimaryKeyBinding(PGPSignatureSubpacketVector pkts,
                                            PGPPublicKey masterPublicKey,
                                            PGPPublicKey signingPublicKey) {
        boolean validPrimaryKeyBinding = false;
        JcaPGPContentVerifierBuilderProvider contentVerifierBuilderProvider =
                new JcaPGPContentVerifierBuilderProvider()
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);
        PGPSignatureList eSigList;

        if (pkts.hasSubpacket(SignatureSubpacketTags.EMBEDDED_SIGNATURE)) {
            try {
                eSigList = pkts.getEmbeddedSignatures();
            } catch (IOException e) {
                return false;
            } catch (PGPException e) {
                return false;
            }
            for (int j = 0; j < eSigList.size(); ++j) {
                PGPSignature emSig = eSigList.get(j);
                if (emSig.getSignatureType() == PGPSignature.PRIMARYKEY_BINDING) {
                    try {
                        emSig.init(contentVerifierBuilderProvider, signingPublicKey);
                        validPrimaryKeyBinding = emSig.verifyCertification(masterPublicKey, signingPublicKey);
                        if (validPrimaryKeyBinding) {
                            break;
                        }
                    } catch (PGPException e) {
                        continue;
                    } catch (SignatureException e) {
                        continue;
                    }
                }
            }
        }

        return validPrimaryKeyBinding;
    }

}
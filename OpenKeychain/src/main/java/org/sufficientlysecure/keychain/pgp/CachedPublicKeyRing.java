package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.ArmoredOutputStream;
import org.spongycastle.bcpg.SignatureSubpacketTags;
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
import java.util.Iterator;

public class CachedPublicKeyRing extends CachedKeyRing {

    private PGPPublicKeyRing mRing;
    private final byte[] mPubKey;

    public CachedPublicKeyRing(long masterKeyId, String userId, boolean hasAnySecret,
                               boolean isRevoked, boolean canCertify, long hasEncryptId, long hasSignId,
                               int verified, byte[] blob)
    {
        super(masterKeyId, userId, hasAnySecret, isRevoked, canCertify,
                hasEncryptId, hasSignId, verified);

        mPubKey = blob;
    }

    PGPPublicKeyRing getRing() {
        if(mRing == null) {
            mRing = (PGPPublicKeyRing) PgpConversionHelper.BytesToPGPKeyRing(mPubKey);
        }
        return mRing;
    }

    public void encode(ArmoredOutputStream stream) throws IOException {
        getRing().encode(stream);
    }

    public CachedPublicKey getSubkey() {
        return new CachedPublicKey(this, getRing().getPublicKey());
    }

    public CachedPublicKey getSubkey(long id) {
        return new CachedPublicKey(this, getRing().getPublicKey(id));
    }

    /** Getter that returns the subkey that should be used for signing. */
    CachedPublicKey getEncryptionSubKey() throws PgpGeneralException {
        PGPPublicKey key = getRing().getPublicKey(getEncryptId());
        if(key != null) {
            CachedPublicKey cKey = new CachedPublicKey(this, key);
            if(!cKey.canEncrypt()) {
                throw new PgpGeneralException("key error");
            }
            return cKey;
        }
        // TODO handle with proper exception
        throw new PgpGeneralException("no encryption key available");
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

    public IterableIterator<CachedPublicKey> iterator() {
        final Iterator<PGPPublicKey> it = getRing().getPublicKeys();
        return new IterableIterator<CachedPublicKey>(new Iterator<CachedPublicKey>() {
            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public CachedPublicKey next() {
                return new CachedPublicKey(CachedPublicKeyRing.this, it.next());
            }

            @Override
            public void remove() {
                it.remove();
            }
        });
    }

}
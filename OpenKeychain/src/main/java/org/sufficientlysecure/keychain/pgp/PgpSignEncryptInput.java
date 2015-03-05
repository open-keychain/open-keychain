package org.sufficientlysecure.keychain.pgp;

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;

import java.util.Date;

public class PgpSignEncryptInput {

    protected String mVersionHeader = null;
    protected boolean mEnableAsciiArmorOutput = false;
    protected int mCompressionId = CompressionAlgorithmTags.UNCOMPRESSED;
    protected long[] mEncryptionMasterKeyIds = null;
    protected String mSymmetricPassphrase = null;
    protected int mSymmetricEncryptionAlgorithm = Constants.OpenKeychainSymmetricKeyAlgorithmTags.USE_PREFERRED;
    protected long mSignatureMasterKeyId = Constants.key.none;
    protected Long mSignatureSubKeyId = null;
    protected int mSignatureHashAlgorithm = Constants.OpenKeychainHashAlgorithmTags.USE_PREFERRED;
    protected String mSignaturePassphrase = null;
    protected long mAdditionalEncryptId = Constants.key.none;
    protected byte[] mNfcSignedHash = null;
    protected Date mNfcCreationTimestamp = null;
    protected boolean mFailOnMissingEncryptionKeyIds = false;
    protected String mCharset;
    protected boolean mCleartextSignature;
    protected boolean mDetachedSignature;

    public String getCharset() {
        return mCharset;
    }

    public void setCharset(String mCharset) {
        this.mCharset = mCharset;
    }

    public boolean ismFailOnMissingEncryptionKeyIds() {
        return mFailOnMissingEncryptionKeyIds;
    }

    public Date getNfcCreationTimestamp() {
        return mNfcCreationTimestamp;
    }

    public byte[] getNfcSignedHash() {
        return mNfcSignedHash;
    }

    public long getAdditionalEncryptId() {
        return mAdditionalEncryptId;
    }

    public PgpSignEncryptInput setAdditionalEncryptId(long additionalEncryptId) {
        mAdditionalEncryptId = additionalEncryptId;
        return this;
    }

    public String getSignaturePassphrase() {
        return mSignaturePassphrase;
    }

    public PgpSignEncryptInput setSignaturePassphrase(String signaturePassphrase) {
        mSignaturePassphrase = signaturePassphrase;
        return this;
    }

    public int getSignatureHashAlgorithm() {
        return mSignatureHashAlgorithm;
    }

    public PgpSignEncryptInput setSignatureHashAlgorithm(int signatureHashAlgorithm) {
        mSignatureHashAlgorithm = signatureHashAlgorithm;
        return this;
    }

    public Long getSignatureSubKeyId() {
        return mSignatureSubKeyId;
    }

    public PgpSignEncryptInput setSignatureSubKeyId(long signatureSubKeyId) {
        mSignatureSubKeyId = signatureSubKeyId;
        return this;
    }

    public long getSignatureMasterKeyId() {
        return mSignatureMasterKeyId;
    }

    public PgpSignEncryptInput setSignatureMasterKeyId(long signatureMasterKeyId) {
        mSignatureMasterKeyId = signatureMasterKeyId;
        return this;
    }

    public int getSymmetricEncryptionAlgorithm() {
        return mSymmetricEncryptionAlgorithm;
    }

    public PgpSignEncryptInput setSymmetricEncryptionAlgorithm(int symmetricEncryptionAlgorithm) {
        mSymmetricEncryptionAlgorithm = symmetricEncryptionAlgorithm;
        return this;
    }

    public String getSymmetricPassphrase() {
        return mSymmetricPassphrase;
    }

    public PgpSignEncryptInput setSymmetricPassphrase(String symmetricPassphrase) {
        mSymmetricPassphrase = symmetricPassphrase;
        return this;
    }

    public long[] getEncryptionMasterKeyIds() {
        return mEncryptionMasterKeyIds;
    }

    public PgpSignEncryptInput setEncryptionMasterKeyIds(long[] encryptionMasterKeyIds) {
        mEncryptionMasterKeyIds = encryptionMasterKeyIds;
        return this;
    }

    public int getCompressionId() {
        return mCompressionId;
    }

    public PgpSignEncryptInput setCompressionId(int compressionId) {
        mCompressionId = compressionId;
        return this;
    }

    public boolean ismEnableAsciiArmorOutput() {
        return mEnableAsciiArmorOutput;
    }

    public String getVersionHeader() {
        return mVersionHeader;
    }

    public PgpSignEncryptInput setVersionHeader(String versionHeader) {
        mVersionHeader = versionHeader;
        return this;
    }

    public PgpSignEncryptInput setEnableAsciiArmorOutput(boolean enableAsciiArmorOutput) {
        mEnableAsciiArmorOutput = enableAsciiArmorOutput;
        return this;
    }

    public PgpSignEncryptInput setFailOnMissingEncryptionKeyIds(boolean failOnMissingEncryptionKeyIds) {
        mFailOnMissingEncryptionKeyIds = failOnMissingEncryptionKeyIds;
        return this;
    }

    public PgpSignEncryptInput setNfcState(byte[] signedHash, Date creationTimestamp) {
        mNfcSignedHash = signedHash;
        mNfcCreationTimestamp = creationTimestamp;
        return this;
    }

    public PgpSignEncryptInput setCleartextSignature(boolean cleartextSignature) {
        this.mCleartextSignature = cleartextSignature;
        return this;
    }

    public boolean isCleartextSignature() {
        return mCleartextSignature;
    }

    public PgpSignEncryptInput setDetachedSignature(boolean detachedSignature) {
        this.mDetachedSignature = detachedSignature;
        return this;
    }

    public boolean isDetachedSignature() {
        return mDetachedSignature;
    }
}


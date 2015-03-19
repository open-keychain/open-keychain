/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.spongycastle.bcpg.CompressionAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.util.Date;

public class PgpSignEncryptInput {

    protected String mVersionHeader = null;
    protected boolean mEnableAsciiArmorOutput = false;
    protected int mCompressionId = CompressionAlgorithmTags.UNCOMPRESSED;
    protected long[] mEncryptionMasterKeyIds = null;
    protected Passphrase mSymmetricPassphrase = null;
    protected int mSymmetricEncryptionAlgorithm = PgpConstants.OpenKeychainSymmetricKeyAlgorithmTags.USE_PREFERRED;
    protected long mSignatureMasterKeyId = Constants.key.none;
    protected Long mSignatureSubKeyId = null;
    protected int mSignatureHashAlgorithm = PgpConstants.OpenKeychainHashAlgorithmTags.USE_PREFERRED;
    protected Passphrase mSignaturePassphrase = null;
    protected long mAdditionalEncryptId = Constants.key.none;
    protected byte[] mNfcSignedHash = null;
    protected Date mNfcCreationTimestamp = null;
    protected boolean mFailOnMissingEncryptionKeyIds = false;
    protected String mCharset;
    protected boolean mCleartextSignature;
    protected boolean mDetachedSignature = false;
    protected boolean mHiddenRecipients = false;

    public String getCharset() {
        return mCharset;
    }

    public void setCharset(String mCharset) {
        this.mCharset = mCharset;
    }

    public boolean isFailOnMissingEncryptionKeyIds() {
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

    public Passphrase getSignaturePassphrase() {
        return mSignaturePassphrase;
    }

    public PgpSignEncryptInput setSignaturePassphrase(Passphrase signaturePassphrase) {
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

    public Passphrase getSymmetricPassphrase() {
        return mSymmetricPassphrase;
    }

    public PgpSignEncryptInput setSymmetricPassphrase(Passphrase symmetricPassphrase) {
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

    public boolean isEnableAsciiArmorOutput() {
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

    public PgpSignEncryptInput setHiddenRecipients(boolean hiddenRecipients) {
        this.mHiddenRecipients = hiddenRecipients;
        return this;
    }

    public boolean isHiddenRecipients() {
        return mHiddenRecipients;
    }
}


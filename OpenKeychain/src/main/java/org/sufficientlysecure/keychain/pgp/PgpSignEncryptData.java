/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2017 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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


import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Passphrase;

@AutoValue
public abstract class PgpSignEncryptData implements Parcelable {
    @Nullable
    public abstract String getCharset();
    abstract long getAdditionalEncryptId();
    abstract int getSignatureHashAlgorithm();
    @Nullable
    public abstract Long getSignatureSubKeyId();
    public abstract long getSignatureMasterKeyId();
    public abstract int getSymmetricEncryptionAlgorithm();
    @Nullable
    public abstract Passphrase getSymmetricPassphrase();
    @Nullable
    public abstract long[] getEncryptionMasterKeyIds();
    public abstract int getCompressionAlgorithm();
    @Nullable
    public abstract String getVersionHeader();

    public abstract boolean isEnableAsciiArmorOutput();
    public abstract boolean isCleartextSignature();
    public abstract boolean isDetachedSignature();
    public abstract boolean isAddBackupHeader();
    public abstract boolean isHiddenRecipients();

    public static Builder builder() {
        return new AutoValue_PgpSignEncryptData.Builder()
                .setCompressionAlgorithm(CompressionAlgorithmTags.UNCOMPRESSED)
                .setSymmetricEncryptionAlgorithm(PgpSecurityConstants.DEFAULT_SYMMETRIC_ALGORITHM)
                .setSignatureMasterKeyId(Constants.key.none)
                .setSignatureHashAlgorithm(PgpSecurityConstants.DEFAULT_HASH_ALGORITHM)
                .setAdditionalEncryptId(Constants.key.none)
                .setEnableAsciiArmorOutput(false)
                .setCleartextSignature(false)
                .setDetachedSignature(false)
                .setAddBackupHeader(false)
                .setHiddenRecipients(false);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract PgpSignEncryptData build();

        public abstract Builder setCharset(String charset);
        public abstract Builder setAdditionalEncryptId(long additionalEncryptId);
        public abstract Builder setSignatureHashAlgorithm(int signatureHashAlgorithm);
        public abstract Builder setSignatureSubKeyId(Long signatureSubKeyId);
        public abstract Builder setSignatureMasterKeyId(long signatureMasterKeyId);
        public abstract Builder setSymmetricEncryptionAlgorithm(int symmetricEncryptionAlgorithm);
        public abstract Builder setSymmetricPassphrase(Passphrase symmetricPassphrase);
        public abstract Builder setEncryptionMasterKeyIds(long[] encryptionMasterKeyIds);
        public abstract Builder setCompressionAlgorithm(int compressionAlgorithm);
        public abstract Builder setVersionHeader(String versionHeader);

        public abstract Builder setAddBackupHeader(boolean isAddBackupHeader);
        public abstract Builder setEnableAsciiArmorOutput(boolean enableAsciiArmorOutput);
        public abstract Builder setCleartextSignature(boolean isCleartextSignature);
        public abstract Builder setDetachedSignature(boolean isDetachedSignature);
        public abstract Builder setHiddenRecipients(boolean isHiddenRecipients);
    }
}


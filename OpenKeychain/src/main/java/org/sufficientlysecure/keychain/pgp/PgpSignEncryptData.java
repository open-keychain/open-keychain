/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
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


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import android.os.Parcelable;
import androidx.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants.OpenKeychainCompressionAlgorithmTags;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants.OpenKeychainHashAlgorithmTags;
import org.sufficientlysecure.keychain.pgp.PgpSecurityConstants.OpenKeychainSymmetricKeyAlgorithmTags;
import org.sufficientlysecure.keychain.util.Passphrase;

@AutoValue
public abstract class PgpSignEncryptData implements Parcelable {
    @Nullable
    public abstract String getCharset();
    abstract long getAdditionalEncryptId();
    @Nullable
    public abstract Long getSignatureSubKeyId();
    public abstract long getSignatureMasterKeyId();
    @Nullable
    public abstract Passphrase getSymmetricPassphrase();
    @Nullable
    @SuppressWarnings("mutable")
    public abstract long[] getEncryptionMasterKeyIds();
    @Nullable
    public abstract List<Long> getAllowedSigningKeyIds();
    @Nullable
    public abstract String getVersionHeader();

    public abstract int getCompressionAlgorithm();
    public abstract int getSignatureHashAlgorithm();
    public abstract int getSymmetricEncryptionAlgorithm();

    public abstract boolean isEnableAsciiArmorOutput();
    public abstract boolean isCleartextSignature();
    public abstract boolean isDetachedSignature();
    public abstract boolean isHiddenRecipients();

    @Nullable
    public abstract String getPassphraseFormat();
    @Nullable
    public abstract String getPassphraseBegin();

    public static Builder builder() {
        return new AutoValue_PgpSignEncryptData.Builder()
                .setSignatureMasterKeyId(Constants.key.none)
                .setAdditionalEncryptId(Constants.key.none)
                .setEnableAsciiArmorOutput(false)
                .setCleartextSignature(false)
                .setDetachedSignature(false)
                .setHiddenRecipients(false)
                .setCompressionAlgorithm(OpenKeychainCompressionAlgorithmTags.USE_DEFAULT)
                .setSignatureHashAlgorithm(OpenKeychainHashAlgorithmTags.USE_DEFAULT)
                .setSymmetricEncryptionAlgorithm(OpenKeychainSymmetricKeyAlgorithmTags.USE_DEFAULT);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract PgpSignEncryptData build();

        public abstract Builder setCharset(String charset);
        public abstract Builder setAdditionalEncryptId(long additionalEncryptId);
        public abstract Builder setSignatureSubKeyId(Long signatureSubKeyId);
        public abstract Builder setSignatureMasterKeyId(long signatureMasterKeyId);
        public abstract Builder setSymmetricPassphrase(Passphrase symmetricPassphrase);
        public abstract Builder setEncryptionMasterKeyIds(long[] encryptionMasterKeyIds);
        public abstract Builder setVersionHeader(String versionHeader);

        public abstract Builder setCompressionAlgorithm(int compressionAlgorithm);
        public abstract Builder setSignatureHashAlgorithm(int signatureHashAlgorithm);
        public abstract Builder setSymmetricEncryptionAlgorithm(int symmetricEncryptionAlgorithm);

        public abstract Builder setEnableAsciiArmorOutput(boolean enableAsciiArmorOutput);
        public abstract Builder setCleartextSignature(boolean isCleartextSignature);
        public abstract Builder setDetachedSignature(boolean isDetachedSignature);
        public abstract Builder setHiddenRecipients(boolean isHiddenRecipients);

        abstract Builder setAllowedSigningKeyIds(List<Long> allowedSigningKeyIds);
        public Builder setAllowedSigningKeyIds(Collection<Long> allowedSigningKeyIds) {
            setAllowedSigningKeyIds(Collections.unmodifiableList(new ArrayList<>(allowedSigningKeyIds)));
            return this;
        }

        public abstract Builder setPassphraseFormat(String passphraseFormat);
        public abstract Builder setPassphraseBegin(String passphraseBegin);
    }
}


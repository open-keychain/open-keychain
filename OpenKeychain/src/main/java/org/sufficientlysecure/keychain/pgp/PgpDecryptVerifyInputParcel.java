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


import java.util.Collections;
import java.util.List;

import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;


@AutoValue
public abstract class PgpDecryptVerifyInputParcel implements Parcelable {
    @Nullable
    @SuppressWarnings("mutable")
    abstract byte[] getInputBytes();

    @Nullable
    abstract Uri getInputUri();
    @Nullable
    abstract Uri getOutputUri();

    abstract boolean isAllowSymmetricDecryption();
    abstract boolean isDecryptMetadataOnly();

    @Nullable
    abstract List<Long> getAllowedKeyIds();
    @Nullable
    @SuppressWarnings("mutable")
    abstract byte[] getDetachedSignature();
    @Nullable
    abstract String getSenderAddress();

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_PgpDecryptVerifyInputParcel.Builder()
                .setAllowSymmetricDecryption(false)
                .setDecryptMetadataOnly(false);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder setInputBytes(byte[] inputBytes);
        public abstract Builder setInputUri(Uri inputUri);
        public abstract Builder setOutputUri(Uri outputUri);

        public abstract Builder setAllowSymmetricDecryption(boolean allowSymmetricDecryption);
        public abstract Builder setDecryptMetadataOnly(boolean decryptMetadataOnly);
        public abstract Builder setDetachedSignature(byte[] detachedSignature);
        public abstract Builder setSenderAddress(String senderAddress);

        public abstract Builder setAllowedKeyIds(List<Long> allowedKeyIds);
        abstract List<Long> getAllowedKeyIds();

        abstract PgpDecryptVerifyInputParcel autoBuild();
        public PgpDecryptVerifyInputParcel build() {
            List<Long> allowedKeyIds = getAllowedKeyIds();
            if (allowedKeyIds != null) {
                setAllowedKeyIds(Collections.unmodifiableList(allowedKeyIds));
            }
            return autoBuild();
        }
    }
}

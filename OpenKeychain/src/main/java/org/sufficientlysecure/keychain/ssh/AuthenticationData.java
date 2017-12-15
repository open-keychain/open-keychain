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

package org.sufficientlysecure.keychain.ssh;

import android.os.Parcelable;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.sufficientlysecure.keychain.Constants;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * AuthenticationData holds metadata pertaining to the signing of a
 * AuthenticationParcel via a AuthenticationOperation
 */
@AutoValue
public abstract class AuthenticationData implements Parcelable {
    public abstract long getAuthenticationMasterKeyId();
    public abstract Long getAuthenticationSubKeyId();
    @Nullable
    public abstract List<Long> getAllowedAuthenticationKeyIds();

    public abstract int getHashAlgorithm();

    public static Builder builder() {
        return new AutoValue_AuthenticationData.Builder()
                .setAuthenticationMasterKeyId(Constants.key.none)
                .setAuthenticationSubKeyId(Constants.key.none)
                .setHashAlgorithm(HashAlgorithmTags.SHA512);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract AuthenticationData build();

        public abstract Builder setAuthenticationMasterKeyId(long authenticationMasterKeyId);
        public abstract Builder setAuthenticationSubKeyId(Long authenticationSubKeyId);

        public abstract Builder setHashAlgorithm(int hashAlgorithm);


        abstract Builder setAllowedAuthenticationKeyIds(List<Long> allowedAuthenticationKeyIds);
        public Builder setAllowedAuthenticationKeyIds(Collection<Long> allowedAuthenticationKeyIds) {
            setAllowedAuthenticationKeyIds(Collections.unmodifiableList(new ArrayList<>(allowedAuthenticationKeyIds)));
            return this;
        }
    }
}

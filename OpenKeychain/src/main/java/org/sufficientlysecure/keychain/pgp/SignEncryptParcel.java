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

import android.net.Uri;
import android.os.Parcelable;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.auto.value.AutoValue;


/**
 * This parcel stores the input of one or more PgpSignEncrypt operations.
 * All operations will use the same general parameters, differing only in
 * input and output. Each input/output set depends on the parameters:
 * <p/>
 * - Each input uri is individually encrypted/signed
 * - If a byte array is supplied, it is treated as an input before uris are processed
 * - The number of output uris must match the number of input uris, plus one more
 * if there is a byte array present.
 * - Once the output uris are empty, there must be exactly one input (uri xor bytes)
 * left, which will be returned in a byte array as part of the result parcel.
 */
@AutoValue
public abstract class SignEncryptParcel implements Parcelable {
    public abstract PgpSignEncryptData getSignEncryptData();
    public abstract List<Uri> getInputUris();
    public abstract List<Uri> getOutputUris();
    @SuppressWarnings("mutable")
    @Nullable
    public abstract byte[] getBytes();


    public boolean isIncomplete() {
        List<Uri> inputUris = getInputUris();
        List<Uri> outputUris = getOutputUris();
        if (inputUris == null || outputUris == null) {
            throw new IllegalStateException("Invalid operation for bytes-backed SignEncryptParcel!");
        }
        return inputUris.size() > outputUris.size();
    }


    public static SignEncryptParcel createSignEncryptParcel(PgpSignEncryptData signEncryptData, byte[] bytes) {
        // noinspection unchecked, it's ok for the empty list
        return new AutoValue_SignEncryptParcel(signEncryptData, Collections.EMPTY_LIST, Collections.EMPTY_LIST, bytes);
    }

    public static Builder builder(SignEncryptParcel signEncryptParcel) {
        return new Builder(signEncryptParcel.getSignEncryptData())
                .addInputUris(signEncryptParcel.getInputUris())
                .addOutputUris(signEncryptParcel.getOutputUris());
    }

    public static Builder builder(PgpSignEncryptData signEncryptData) {
        return new Builder(signEncryptData);
    }


    public static class Builder {
        private final PgpSignEncryptData signEncryptData;
        private ArrayList<Uri> inputUris = new ArrayList<>();
        private ArrayList<Uri> outputUris = new ArrayList<>();

        private Builder(PgpSignEncryptData signEncryptData) {
            this.signEncryptData = signEncryptData;
        }

        public SignEncryptParcel build() {
            return new AutoValue_SignEncryptParcel(signEncryptData,
                    Collections.unmodifiableList(inputUris),
                    Collections.unmodifiableList(outputUris),
                    null);
        }

        public Builder addOutputUris(Collection<Uri> outputUris) {
            this.outputUris.addAll(outputUris);
            return this;
        }
        public Builder addInputUris(Collection<Uri> inputUris) {
            this.inputUris.addAll(inputUris);
            return this;
        }
    }
}

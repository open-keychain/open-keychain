/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
 * Copyright (C) 2017 Christian Hagau <ach@hagau.se>
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

import com.google.auto.value.AutoValue;

/**
 * AuthenticationParcel holds the challenge to be signed for authentication
 */
@AutoValue
public abstract class AuthenticationParcel implements Parcelable {

    public abstract AuthenticationData getAuthenticationData();

    @SuppressWarnings("mutable")
    public abstract byte[] getChallenge();

    public static AuthenticationParcel createAuthenticationParcel(AuthenticationData authenticationData,
                                                                  byte[] challenge) {
        return new AutoValue_AuthenticationParcel(authenticationData, challenge);
    }
}

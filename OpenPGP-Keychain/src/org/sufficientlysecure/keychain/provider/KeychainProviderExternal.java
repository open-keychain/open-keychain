/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.provider;

/**
 * The same content provider as ApgProviderInternal except that it does not give out keyRing and key
 * blob data when querying.
 * 
 * This provider is exported with a readPermission in AndroidManifest.xml
 */
public class KeychainProviderExternal extends KeychainProvider {

    public KeychainProviderExternal() {
        mInternalProvider = false;
    }

}

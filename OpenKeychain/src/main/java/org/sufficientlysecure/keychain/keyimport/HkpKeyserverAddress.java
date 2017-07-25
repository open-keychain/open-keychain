/*
 * Copyright (C) 2016-2017 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
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

package org.sufficientlysecure.keychain.keyimport;


import java.net.URI;
import java.net.URISyntaxException;

import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class HkpKeyserverAddress implements Parcelable {
    private static final short PORT_DEFAULT = 11371;
    private static final short PORT_DEFAULT_HKPS = 443;

    public abstract String getUrl();
    @Nullable
    public abstract String getOnion();


    public static HkpKeyserverAddress createWithOnionProxy(@NonNull String url, @Nullable String onion) {
        return new AutoValue_HkpKeyserverAddress(url, onion == null ? null : onion.trim());
    }

    public static HkpKeyserverAddress createFromUri(@NonNull String url) {
        return new AutoValue_HkpKeyserverAddress(url, null);
    }


    public URI getUrlURI() throws URISyntaxException {
        return getURI(getUrl());
    }

    public URI getOnionURI() throws URISyntaxException {
        return getOnion() != null ? getURI(getOnion()) : null;
    }

    /**
     * @param keyserverUrl "<code>hostname</code>" (eg. "<code>pool.sks-keyservers.net</code>"), then it will
     *                     connect using {@link #PORT_DEFAULT}. However, port may be specified after colon
     *                     ("<code>hostname:port</code>", eg. "<code>p80.pool.sks-keyservers.net:80</code>").
     */
    private URI getURI(String keyserverUrl) throws URISyntaxException {
        URI originalURI = new URI(keyserverUrl);

        String scheme = originalURI.getScheme();
        if (scheme == null) {
            throw new URISyntaxException("", "scheme null!");
        }
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)
                && !"hkp".equalsIgnoreCase(scheme) && !"hkps".equalsIgnoreCase(scheme)) {
            throw new URISyntaxException(scheme, "unsupported scheme!");
        }

        int port = originalURI.getPort();

        if ("hkps".equalsIgnoreCase(scheme)) {
            scheme = "https";
            port = port == -1 ? PORT_DEFAULT_HKPS : port;
        } else if ("hkp".equalsIgnoreCase(scheme)) {
            scheme = "http";
            port = port == -1 ? PORT_DEFAULT : port;
        }

        return new URI(scheme, originalURI.getUserInfo(), originalURI.getHost(), port,
                originalURI.getPath(), originalURI.getQuery(), originalURI.getFragment());
    }

}

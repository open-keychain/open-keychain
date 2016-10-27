/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.util;


import java.net.InetSocketAddress;
import java.net.Proxy;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

/**
 * used to simply transport java.net.Proxy objects created using InetSockets between services/activities
 */
public class ParcelableProxy implements Parcelable {
    private String mProxyHost;
    private int mProxyPort;
    private Proxy.Type mProxyType;
    private int mProxyMode;

    public static final int PROXY_MODE_NORMAL = 0;
    public static final int PROXY_MODE_TOR = 1;

    public ParcelableProxy(String hostName, int port, Proxy.Type type, int proxyMode) {
        mProxyHost = hostName;
        if (mProxyHost == null) {
            return; // represents a null proxy
        }

        mProxyPort = port;
        mProxyType = type;
        mProxyMode = proxyMode;
    }

    public static ParcelableProxy getForNoProxy() {
        return new ParcelableProxy(null, -1, null, PROXY_MODE_NORMAL);
    }

    public boolean isTorEnabled() {
        return (mProxyMode == PROXY_MODE_TOR);
    }

    @NonNull
    public Proxy getProxy() {
        if (mProxyHost == null) {
            return Proxy.NO_PROXY;
        }
        /*
        * InetSocketAddress.createUnresolved so we can use this method even in the main thread
        * (no network call)
        */
        return new Proxy(mProxyType, InetSocketAddress.createUnresolved(mProxyHost, mProxyPort));
    }

    protected ParcelableProxy(Parcel in) {
        mProxyHost = in.readString();
        mProxyPort = in.readInt();
        mProxyType = (Proxy.Type) in.readSerializable();
        mProxyMode = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mProxyHost);
        dest.writeInt(mProxyPort);
        dest.writeSerializable(mProxyType);
        dest.writeInt(mProxyMode);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<ParcelableProxy> CREATOR = new Parcelable.Creator<ParcelableProxy>() {
        @Override
        public ParcelableProxy createFromParcel(Parcel in) {
            return new ParcelableProxy(in);
        }

        @Override
        public ParcelableProxy[] newArray(int size) {
            return new ParcelableProxy[size];
        }
    };
}

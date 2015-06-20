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

import android.os.Parcel;
import android.os.Parcelable;

import java.net.InetSocketAddress;
import java.net.Proxy;

/**
 * used to simply transport java.net.Proxy objects created using InetSockets between services/activities
 */
public class ParcelableProxy implements Parcelable {
    private String mProxyHost;
    private int mProxyPort;
    private int mProxyType;

    private final int TYPE_HTTP = 1;
    private final int TYPE_SOCKS = 2;

    public ParcelableProxy(String hostName, int port, Proxy.Type type) {
        mProxyHost = hostName;

        if (hostName == null) return; // represents a null proxy

        mProxyPort = port;

        switch (type) {
            case HTTP: {
                mProxyType = TYPE_HTTP;
                break;
            }
            case SOCKS: {
                mProxyType = TYPE_SOCKS;
                break;
            }
        }
    }

    public Proxy getProxy() {
        if (mProxyHost == null) return null;

        Proxy.Type type = null;
        switch (mProxyType) {
            case TYPE_HTTP:
                type = Proxy.Type.HTTP;
                break;
            case TYPE_SOCKS:
                type = Proxy.Type.SOCKS;
                break;
        }
        return new Proxy(type, new InetSocketAddress(mProxyHost, mProxyPort));
    }

    public static ParcelableProxy getForNoProxy() {
        return new ParcelableProxy(null, -1, null);
    }

    protected ParcelableProxy(Parcel in) {
        mProxyHost = in.readString();
        mProxyPort = in.readInt();
        mProxyType = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mProxyHost);
        dest.writeInt(mProxyPort);
        dest.writeInt(mProxyType);
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

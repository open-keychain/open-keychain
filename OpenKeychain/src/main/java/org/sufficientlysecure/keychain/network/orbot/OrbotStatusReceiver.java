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

package org.sufficientlysecure.keychain.network.orbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

/**
 * BroadcastReceiver that receives Orbots status
 */
public class OrbotStatusReceiver extends BroadcastReceiver {

    // TODO: These two Strings are missing in older versions of NetCipher.
    // TODO: Once they are present in OrbotHelper (not ProxyHelper) point to OrbotHelpers Strings instead.
    public final static String EXTRA_PROXY_PORT_HTTP = "org.torproject.android.intent.extra.HTTP_PROXY_PORT";
    public final static String EXTRA_PROXY_PORT_SOCKS = "org.torproject.android.intent.extra.SOCKS_PROXY_PORT";

    private boolean torRunning;
    private int proxyPortHttp;
    private int proxyPortSocks;

    private static OrbotStatusReceiver instance;

    public OrbotStatusReceiver() {
        instance = this;
    }

    public static OrbotStatusReceiver getInstance() {
        if (instance == null) {
            instance = new OrbotStatusReceiver();
        }
        return instance;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (OrbotHelper.ACTION_STATUS.equals(intent.getAction())) {
            String status = intent.getStringExtra(OrbotHelper.EXTRA_STATUS);
            this.torRunning = (intent.getStringExtra(OrbotHelper.EXTRA_STATUS).equals(OrbotHelper.STATUS_ON));

            Log.d(Constants.TAG, "Receiver: Orbot status: " + status);
            if (torRunning) {
                Bundle extras = intent.getExtras();

                if (extras.containsKey(EXTRA_PROXY_PORT_HTTP)) {
                    this.proxyPortHttp = extras.getInt(EXTRA_PROXY_PORT_HTTP, -1);
                    Log.i(Constants.TAG, "Receiver: Orbot Http proxy at " + proxyPortHttp);
                }

                if (extras.containsKey(EXTRA_PROXY_PORT_SOCKS)) {
                    this.proxyPortSocks = extras.getInt(EXTRA_PROXY_PORT_SOCKS, -1);
                    Log.i(Constants.TAG, "Receiver: Orbot Socks proxy at " + proxyPortSocks);
                }
            }
        }
    }

    public boolean isTorRunning(Context context) {
        OrbotHelper.requestStartTor(context);
        return this.torRunning;
    }

    public int getProxyPortHttp(Context context) {
        OrbotHelper.requestStartTor(context);
        return this.proxyPortHttp;
    }

    public int getProxyPortSocks(Context context) {
        OrbotHelper.requestStartTor(context);
        return this.proxyPortSocks;
    }
}

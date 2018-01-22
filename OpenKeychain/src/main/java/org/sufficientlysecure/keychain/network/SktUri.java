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

package org.sufficientlysecure.keychain.network;

import java.net.URISyntaxException;
import java.nio.charset.Charset;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.auto.value.AutoValue;
import org.bouncycastle.util.encoders.DecoderException;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import timber.log.Timber;


@AutoValue
abstract class SktUri {
    private static final String QRCODE_URI_FORMAT = Constants.SKT_SCHEME + ":%s/%d/%s";
    private static final String QRCODE_URI_FORMAT_SSID = Constants.SKT_SCHEME + ":%s/%d/%s/SSID:%s";


    public abstract String getHost();
    public abstract int getPort();
    @SuppressWarnings("mutable")
    public abstract byte[] getPresharedKey();

    @Nullable
    public abstract String getWifiSsid();

    @NonNull
    public static SktUri parse(String input) throws URISyntaxException {
        if (!input.startsWith(Constants.SKT_SCHEME + ":")) {
            throw new URISyntaxException(input, "invalid scheme");
        }

        String[] pieces = input.substring(input.indexOf(":") +1).split("/");
        if (pieces.length < 3) {
            throw new URISyntaxException(input, "invalid syntax");
        }

        String address = pieces[0];
        int port;
        try {
            port = Integer.parseInt(pieces[1]);
        } catch (NumberFormatException e) {
            throw new URISyntaxException(input, "error parsing port");
        }
        byte[] psk;
        try {
            psk = Hex.decode(pieces[2]);
        } catch (DecoderException e) {
            throw new URISyntaxException(input, "error parsing hex psk");
        }

        String wifiSsid = null;
        for (int i = 3; i < pieces.length; i++) {
            String[] optarg = pieces[i].split(":", 2);
            if (optarg.length == 2 && "SSID".equals(optarg[0])) {
                try {
                    wifiSsid = new String(Hex.decode(optarg[1]));
                } catch (DecoderException e) {
                    Timber.d("error parsing ssid in skt uri, ignoring: " + input);
                }
            }
        }

        return new AutoValue_SktUri(address, port, psk, wifiSsid);
    }

    @SuppressLint("DefaultLocale")
    String toUriString() {
        String sktHex = Hex.toHexString(getPresharedKey());
        String wifiSsid = getWifiSsid();

        String result;
        if (wifiSsid != null) {
            String encodedWifiSsid = Hex.toHexString(getWifiSsid().getBytes(Charset.defaultCharset()));
            result = String.format(QRCODE_URI_FORMAT_SSID, getHost(), getPort(), sktHex, encodedWifiSsid);
        } else {
            result = String.format(QRCODE_URI_FORMAT, getHost(), getPort(), sktHex);
        }

        return result.toUpperCase();
    }

    static SktUri create(String host, int port, byte[] presharedKey, @Nullable String wifiSsid) {
        return new AutoValue_SktUri(host, port, presharedKey, wifiSsid);
    }
}

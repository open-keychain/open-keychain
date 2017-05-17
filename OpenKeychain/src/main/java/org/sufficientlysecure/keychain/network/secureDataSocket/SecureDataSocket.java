/*
 * Copyright (C) 2017 Jakob Bode
 * Copyright (C) 2017 Matthias Sekul
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

//Have a look at https://github.com/MrJabo/SecureDataSocket

package org.sufficientlysecure.keychain.network.secureDataSocket;


import org.sufficientlysecure.keychain.network.secureDataSocket.CryptoSocketInterface.Channel;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.List;

public class SecureDataSocket {

    private int mPort;
    private FDESocket mSocket;
    public SecureDataSocket(int port) {
        this.mPort = port;
    }


    /**
     * setup client with already known sharedSecret.
     * connectionDetails have to look like ipAddress:port:sharedSecret
     * <p>
     * Connection established afterwards.
     */
    public void setupClient(String connectionDetails) throws SecureDataSocketException {
        try {
            this.mSocket = new FDESocket(new Channel(connectionDetails));
            this.mSocket.connect();
        } catch (Exception e) {
            throw new SecureDataSocketException(e.toString(), e);
        }
    }


    /**
     * call setupServerWithClientCamera() afterwards.
     * <p>
     * returns the connectiondetails and the sharedSecret, that has to be transferred
     * securely to the client by the user.
     */
    public String prepareServer() throws SecureDataSocketException {
        try {
            this.mSocket = new FDESocket(new Channel("::"));
            return getIPAddress(true) + ":" + this.mPort + ":" + this.mSocket.createSharedSecret();
        } catch (Exception e) {
            throw new SecureDataSocketException(e.toString(), e);
        }
    }


    /**
     * Method blocks until a client connected
     */
    public void setupServer() throws SecureDataSocketException {
        try {
            this.mSocket.listen(this.mPort);
        } catch (Exception e) {
            throw new SecureDataSocketException(e.toString(), e);
        }
    }

    public byte[] read() throws SecureDataSocketException {
        byte[] read;
        try {
            read = this.mSocket.read();
        } catch (Exception e) {
            throw new SecureDataSocketException(e.toString(), e);
        }
        return read;
    }

    public int write(byte[] array) throws SecureDataSocketException {
        int ret;
        try {
            ret = this.mSocket.write(array);
        } catch (Exception e) {
            throw new SecureDataSocketException(e.toString(), e);
        }
        return ret;
    }

    public void close() {
        this.mSocket.close();
    }

    /**
     * from: http://stackoverflow.com/a/13007325
     * <p>
     * Get IP address from first non-localhost interface
     *
     * @param useIPv4 true=return ipv4, false=return ipv6
     * @return address or empty string
     */
    private static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.
                    getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        boolean isIPv4 = sAddr.indexOf(':') < 0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim < 0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).
                                        toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
        } // for now eat exceptions
        return "";
    }
}

/*
 * Copyright (C) 2016 Nikita Mikhailov <nikita.s.mikhailov@gmail.com>
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

package org.sufficientlysecure.keychain.securitytoken;

import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TokenType;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TransportType;

import java.io.IOException;

/**
 * Abstraction for transmitting APDU commands
 */
public interface Transport {
    /**
     * Transmit and receive data
     * @param data data to transmit
     * @return received data
     * @throws IOException
     */
    ResponseAPDU transceive(CommandAPDU data) throws IOException;

    /**
     * Disconnect and release connection
     */
    void release();

    /**
     * Check if device is was connected to and still is connected
     * @return connection status
     */
    boolean isConnected();

    /**
     * Check if Transport supports persistent connections e.g connections which can
     * handle multiple operations in one session
     * @return true if transport supports persistent connections
     */
    boolean isPersistentConnectionAllowed();


    /**
     * Connect to device
     * @throws IOException
     */
    void connect() throws IOException;

    TransportType getTransportType();

    TokenType getTokenType();
}

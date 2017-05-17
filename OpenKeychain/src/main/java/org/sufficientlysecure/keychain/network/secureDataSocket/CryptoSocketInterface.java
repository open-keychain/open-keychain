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

import java.lang.IllegalStateException;
import java.net.SocketTimeoutException;
import java.net.SocketAddress;
import java.io.IOException;

interface CryptoSocketInterface {

    /**
     * A sharedSecret will be created and given to the user.
     * The User has to transfer it securely (!!!) to the partnerdevice.
     * Once a sharedSecret is known by both devices a networkconnection can be established.
     * I.e. a sharedSecret has not to be created on both sides.
     * Method to create the sharedSecret is called createSharedSecret().
     * Method to set the sharedSecret in the partnerdevice is called
     * setSharedSecret(byte[] sharedSecret).
     * These Methods have to be called BEFORE a networkconnection has been established
     * (via listen or connect), because the sharedSecret will be used for the Crypto.
     * The networkconnection will be fully usable afert it has been established.
     */

    class Channel {
        /**
         * definitions of id.
         * <p>
         * id                          | Description
         * -----------------------------+--------------------------------------------
         * ipAddress:Port:sharedSecret | only the networkChannel is defined here.
         * | If used as a client the sharedSecret has to
         * | be set.
         */
        public final String id;

        Channel(String id) {
            this.id = id;
        }
    }

    enum RETURN {
        SUCCESS(0),
        READ(-1),
        WRITE(-2),
        WRONG_TAG(-3),
        NOT_AVAILABLE(-4),
        INVALID_CIPHERTEXT(-5);

        private final int value;

        RETURN(int value) {
            this.value = value;
        }

        public int getValue() {
            return this.value;
        }
    }

    /**
     * Await a connection from a device, which calls connect().
     * listen is blocking, until a connection is established
     * port - On which port server listening. 0 for random
     */
    SocketAddress listen(int port) throws IOException, CryptoSocketException;

    /**
     * Connect to a device, which awaits a connection and called listen().
     * Throws SocketTimeoutException when connection timedout, CryptoSocketException when the
     * destination id is wrong or IOException if socket creation fails
     * returns if connection is established
     */
    boolean connect() throws CryptoSocketException, IOException, SocketTimeoutException;

    /**
     * Creates and sets a sharedSecret.
     * It will be returned, so the user can transfer it to the communicationpartner and set it there.
     * I.e. channel MANUAL has to be used.
     */
    String createSharedSecret() throws CryptoSocketException, IOException;

    /**
     * Sends the given bytearray to the communicatonpartner.
     * If the partnerdevice is not verified (via OOB), an UnverifiedException will be
     * thrown.
     * Return negativ value on error, for error codes please look at enum RETURN.
     * Returns RETURN.INVALID_CIPHERTEXT or RETURN.SUCCESS
     */
    int write(byte[] array) throws UnverifiedException, IllegalStateException, IOException,
            CryptoSocketException;

    /*
    * Reads bytes send from the communicationpartner and stores them in data.
    * If blocking is true, this function blocks until the next entity is
    * read.
    * If blocking is false, methods tries to read data.length byte or less bytes.
    * Returns the total number of bytes read into the buffer.
    * Negativ value on error, for error codes please look at enum RETURN.
    * 	Returns RETURN.READ, RETURN.WRONG_TAG, RETURN.NOT_AVAILABLE, RETURN.INVALID_CIPHERTEXT
    */
    int read(boolean blocking, byte[] data) throws IllegalStateException, CryptoSocketException,
            IOException;

    /**
     * Returns true, if there is something to read, false, if not.
     */
    boolean hasNext() throws IOException;

    /**
     * Close the connection.
     */
    void close();
}

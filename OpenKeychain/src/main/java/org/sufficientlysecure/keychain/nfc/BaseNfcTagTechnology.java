/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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
package org.sufficientlysecure.keychain.nfc;


/**
 * Nfc Tag Technology interface.
 *
 * All NFC Card Exceptions should be of type CardException.
 */
public interface BaseNfcTagTechnology {

    /**
     * Connects the device to the NFC tag
     *
     * @throws NfcDispatcher.CardException
     */
    void connect() throws NfcDispatcher.CardException;

    /**
     * Writes the data into the NFC Tag memory
     *
     * @param data
     * @throws NfcDispatcher.CardException
     */
    void upload(byte[] data) throws NfcDispatcher.CardException;

    /**
     * Reads the data from the NFC tag
     *
     * @return
     * @throws NfcDispatcher.CardException
     */
    byte[] read() throws NfcDispatcher.CardException;

    /**
     * Compares the computed passphrase with the one that was read from the NFC tag
     *
     * @param original
     * @param fromNFC
     * @return
     * @throws NfcDispatcher.CardException
     */
    boolean verify(byte[] original, byte[] fromNFC) throws NfcDispatcher.CardException;

    /**
     * Disconnects the NFC tag from the device
     *
     * @throws NfcDispatcher.CardException
     */
    void close() throws NfcDispatcher.CardException;

    /**
     * Checks if the NFC tag is connected to the device
     *
     * @return
     */
    boolean isConnected();
}

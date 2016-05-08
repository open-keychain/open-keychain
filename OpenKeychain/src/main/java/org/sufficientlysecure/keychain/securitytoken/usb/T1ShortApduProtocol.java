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

package org.sufficientlysecure.keychain.securitytoken.usb;

import android.support.annotation.NonNull;

import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

public class T1ShortApduProtocol implements CcidTransportProtocol {
    private CcidTransceiver mTransceiver;

    public T1ShortApduProtocol(CcidTransceiver transceiver) throws UsbTransportException {
        mTransceiver = transceiver;

        byte[] atr = mTransceiver.iccPowerOn();
        Log.d(Constants.TAG, "Usb transport connected T1/Short APDU, ATR=" + Hex.toHexString(atr));
    }

    @Override
    public byte[] transceive(@NonNull final byte[] apdu) throws UsbTransportException {
        mTransceiver.sendXfrBlock(apdu);
        return mTransceiver.receiveRaw();
    }
}

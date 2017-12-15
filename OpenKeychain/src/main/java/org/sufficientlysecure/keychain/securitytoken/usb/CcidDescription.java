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

package org.sufficientlysecure.keychain.securitytoken.usb;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import com.google.auto.value.AutoValue;
import org.sufficientlysecure.keychain.securitytoken.usb.tpdu.T1ShortApduProtocol;
import org.sufficientlysecure.keychain.securitytoken.usb.tpdu.T1TpduProtocol;


@AutoValue
abstract class CcidDescription {
    private static final int DESCRIPTOR_LENGTH = 0x36;
    private static final int DESCRIPTOR_TYPE = 0x21;

    // dwFeatures Masks
    private static final int FEATURE_AUTOMATIC_VOLTAGE = 0x00008;
    private static final int FEATURE_EXCHANGE_LEVEL_TPDU = 0x10000;
    private static final int FEATURE_EXCHAGE_LEVEL_SHORT_APDU = 0x20000;
    private static final int FEATURE_EXCHAGE_LEVEL_EXTENDED_APDU = 0x40000;

    // bVoltageSupport Masks
    private static final byte VOLTAGE_5V = 1;
    private static final byte VOLTAGE_3V = 2;
    private static final byte VOLTAGE_1_8V = 4;

    private static final int SLOT_OFFSET = 4;
    private static final int FEATURES_OFFSET = 40;
    private static final short MASK_T1_PROTO = 2;

    public abstract byte getMaxSlotIndex();
    public abstract byte getVoltageSupport();
    public abstract int getProtocols();
    public abstract int getFeatures();

    @VisibleForTesting
    static CcidDescription fromValues(byte maxSlotIndex, byte voltageSupport, int protocols, int features) {
        return new AutoValue_CcidDescription(maxSlotIndex, voltageSupport, protocols, features);
    }

    @NonNull
    static CcidDescription fromRawDescriptors(byte[] desc) throws UsbTransportException {
        int dwProtocols = 0, dwFeatures = 0;
        byte bMaxSlotIndex = 0, bVoltageSupport = 0;

        boolean hasCcidDescriptor = false;

        ByteBuffer byteBuffer = ByteBuffer.wrap(desc).order(ByteOrder.LITTLE_ENDIAN);

        while (byteBuffer.hasRemaining()) {
            byteBuffer.mark();
            byte len = byteBuffer.get(), type = byteBuffer.get();

            if (type == DESCRIPTOR_TYPE && len == DESCRIPTOR_LENGTH) {
                byteBuffer.reset();

                byteBuffer.position(byteBuffer.position() + SLOT_OFFSET);
                bMaxSlotIndex = byteBuffer.get();
                bVoltageSupport = byteBuffer.get();
                dwProtocols = byteBuffer.getInt();

                byteBuffer.reset();

                byteBuffer.position(byteBuffer.position() + FEATURES_OFFSET);
                dwFeatures = byteBuffer.getInt();
                hasCcidDescriptor = true;
                break;
            } else {
                byteBuffer.position(byteBuffer.position() + len - 2);
            }
        }

        if (!hasCcidDescriptor) {
            throw new UsbTransportException("CCID descriptor not found");
        }

        return new AutoValue_CcidDescription(bMaxSlotIndex, bVoltageSupport, dwProtocols, dwFeatures);
    }

    Voltage[] getVoltages() {
        ArrayList<Voltage> voltages = new ArrayList<>();

        if (hasFeature(FEATURE_AUTOMATIC_VOLTAGE)) {
            voltages.add(Voltage.AUTO);
        } else {
            for (Voltage v : Voltage.values()) {
                if ((v.mask & getVoltageSupport()) != 0) {
                    voltages.add(v);
                }
            }
        }

        return voltages.toArray(new Voltage[voltages.size()]);
    }

    CcidTransportProtocol getSuitableTransportProtocol() throws UsbTransportException {
        boolean hasT1Protocol = (getProtocols() & MASK_T1_PROTO) != 0;
        if (!hasT1Protocol) {
            throw new UsbTransportException("T=0 protocol is not supported!");
        }

        if (hasFeature(CcidDescription.FEATURE_EXCHANGE_LEVEL_TPDU)) {
            return new T1TpduProtocol();
        } else if (hasFeature(CcidDescription.FEATURE_EXCHAGE_LEVEL_SHORT_APDU) ||
                hasFeature(CcidDescription.FEATURE_EXCHAGE_LEVEL_EXTENDED_APDU)) {
            return new T1ShortApduProtocol();
        } else {
            throw new UsbTransportException("Character level exchange is not supported");
        }
    }

    private boolean hasFeature(int feature) {
        return (getFeatures() & feature) != 0;
    }

    enum Voltage {
        AUTO(0, 0), _5V(1, VOLTAGE_5V), _3V(2, VOLTAGE_3V), _1_8V(3, VOLTAGE_1_8V);

        final byte mask;
        final byte powerOnValue;

        Voltage(int powerOnValue, int mask) {
            this.powerOnValue = (byte) powerOnValue;
            this.mask = (byte) mask;
        }
    }
}

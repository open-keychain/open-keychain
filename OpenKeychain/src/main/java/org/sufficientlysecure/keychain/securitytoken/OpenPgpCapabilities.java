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

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class OpenPgpCapabilities {
    private final static int MASK_SM = 1 << 7;
    private final static int MASK_KEY_IMPORT = 1 << 5;
    private final static int MASK_ATTRIBUTES_CHANGABLE = 1 << 2;

    private boolean mPw1ValidForMultipleSignatures;
    private byte[] mAid;
    private byte[] mHistoricalBytes;

    private boolean mHasSM;
    private boolean mAttriburesChangable;
    private boolean mHasKeyImport;

    private int mSMAESKeySize;
    private int mMaxCmdLen;
    private int mMaxRspLen;

    private Map<KeyType, KeyFormat> mKeyFormats;

    public OpenPgpCapabilities(byte[] data) throws IOException {
        mKeyFormats = new HashMap<>();
        updateWithData(data);
    }

    public void updateWithData(byte[] data) throws IOException {
        Iso7816TLV[] tlvs = Iso7816TLV.readList(data, true);
        if (tlvs.length == 1 && tlvs[0].mT == 0x6E) {
            tlvs = ((Iso7816TLV.Iso7816CompositeTLV) tlvs[0]).mSubs;
        }

        for (Iso7816TLV tlv : tlvs) {
            switch (tlv.mT) {
                case 0x4F:
                    mAid = tlv.mV;
                    break;
                case 0x5F52:
                    mHistoricalBytes = tlv.mV;
                    break;
                case 0x73:
                    parseDdo((Iso7816TLV.Iso7816CompositeTLV) tlv);
                    break;
                case 0xC0:
                    parseExtendedCaps(tlv.mV);
                    break;
                case 0xC1:
                    mKeyFormats.put(KeyType.SIGN, KeyFormat.fromBytes(tlv.mV));
                    break;
                case 0xC2:
                    mKeyFormats.put(KeyType.ENCRYPT, KeyFormat.fromBytes(tlv.mV));
                    break;
                case 0xC3:
                    mKeyFormats.put(KeyType.AUTH, KeyFormat.fromBytes(tlv.mV));
                    break;
                case 0xC4:
                    mPw1ValidForMultipleSignatures = tlv.mV[0] == 1;
                    break;
            }
        }
    }

    private void parseDdo(Iso7816TLV.Iso7816CompositeTLV tlvs) {
        for (Iso7816TLV tlv : tlvs.mSubs) {
            switch (tlv.mT) {
                case 0xC0:
                    parseExtendedCaps(tlv.mV);
                    break;
                case 0xC1:
                    mKeyFormats.put(KeyType.SIGN, KeyFormat.fromBytes(tlv.mV));
                    break;
                case 0xC2:
                    mKeyFormats.put(KeyType.ENCRYPT, KeyFormat.fromBytes(tlv.mV));
                    break;
                case 0xC3:
                    mKeyFormats.put(KeyType.AUTH, KeyFormat.fromBytes(tlv.mV));
                    break;
                case 0xC4:
                    mPw1ValidForMultipleSignatures = tlv.mV[0] == 1;
                    break;
            }
        }
    }

    private void parseExtendedCaps(byte[] v) {
        mHasSM = (v[0] & MASK_SM) != 0;
        mHasKeyImport = (v[0] & MASK_KEY_IMPORT) != 0;
        mAttriburesChangable = (v[0] & MASK_ATTRIBUTES_CHANGABLE) != 0;

        mSMAESKeySize = (v[1] == 1) ? 16 : 32;

        mMaxCmdLen = (v[6] << 8) + v[7];
        mMaxRspLen = (v[8] << 8) + v[9];
    }

    public boolean isPw1ValidForMultipleSignatures() {
        return mPw1ValidForMultipleSignatures;
    }

    public byte[] getAid() {
        return mAid;
    }

    public byte[] getHistoricalBytes() {
        return mHistoricalBytes;
    }

    public boolean isHasSM() {
        return mHasSM;
    }

    public boolean isAttributesChangable() {
        return mAttriburesChangable;
    }

    public boolean isHasKeyImport() {
        return mHasKeyImport;
    }

    public int getSMAESKeySize() {
        return mSMAESKeySize;
    }

    public int getMaxCmdLen() {
        return mMaxCmdLen;
    }

    public int getMaxRspLen() {
        return mMaxRspLen;
    }

    public KeyFormat getFormatForKeyType(KeyType keyType) {
        return mKeyFormats.get(keyType);
    }
}

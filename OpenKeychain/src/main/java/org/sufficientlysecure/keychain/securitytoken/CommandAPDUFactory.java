package org.sufficientlysecure.keychain.securitytoken;


import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

import android.support.annotation.NonNull;

import javax.smartcardio.CommandAPDU;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;


class CommandAPDUFactory {
    private static final int MAX_APDU_NC = 255;
    private static final int MAX_APDU_NC_EXT = 65535;

    private static final int MAX_APDU_NE = 256;
    private static final int MAX_APDU_NE_EXT = 65536;

    final int CLA = 0x00;

    final int MASK_CLA_CHAINING = 1 << 4;

    @NonNull
    CommandAPDU createPutKeyCommand(byte[] keyBytes) {
        return new CommandAPDU(CLA, 0xDB, 0x3F, 0xFF, keyBytes);
    }

    @NonNull
    CommandAPDU createComputeDigitalSignatureCommand(byte[] data) {
        return new CommandAPDU(CLA, 0x2A, 0x9E, 0x9A, data, MAX_APDU_NE_EXT);
    }


    @NonNull
    CommandAPDU createPutDataCommand(int dataObject, byte[] data) {
        return new CommandAPDU(CLA, 0xDA, (dataObject & 0xFF00) >> 8, dataObject & 0xFF, data);
    }

    @NonNull
    CommandAPDU createDecipherCommand(byte[] data) {
        return new CommandAPDU(CLA, 0x2A, 0x80, 0x86, data, MAX_APDU_NE_EXT);
    }

    @NonNull
    CommandAPDU createChangeReferenceDataCommand(int pw, byte[] newPin, byte[] pin) {
        return new CommandAPDU(CLA, 0x24, 0x00, pw, Arrays.concatenate(pin, newPin));
    }

    @NonNull
    CommandAPDU createResetRetryCounter(byte[] newPin) {
        return new CommandAPDU(CLA, 0x2C, 0x02, 0x81, newPin);
    }

    @NonNull
    CommandAPDU createGetDataCommand(int p1, int p2) {
        return new CommandAPDU(CLA, 0xCA, p1, p2, MAX_APDU_NE_EXT);
    }

    @NonNull
    CommandAPDU createGenerateKeyCommand(int slot) {
        return new CommandAPDU(CLA, 0x47, 0x80, 0x00, new byte[] { (byte) slot, 0x00 }, MAX_APDU_NE_EXT);
    }

    @NonNull
    CommandAPDU createGetResponseCommand(int lastResponseSw2) {
        return new CommandAPDU(CLA, 0xC0, 0x00, 0x00, lastResponseSw2);
    }


    @NonNull
    CommandAPDU createVerifyCommand(int mode, byte[] pin) {
        return new CommandAPDU(CLA, 0x20, 0x00, mode, pin);
    }

    @NonNull
    CommandAPDU createSelectFileCommand(String fileAid) {
        return new CommandAPDU(CLA, 0xA4, 0x04, 0x00, Hex.decode(fileAid));
    }

    @NonNull
    CommandAPDU createReactivate2Command() {
        return new CommandAPDU(CLA, 0x44, 0x00, 0x00);
    }

    @NonNull
    CommandAPDU createReactivate1Command() {
        return new CommandAPDU(CLA, 0xE6, 0x00, 0x00);
    }

    @NonNull
    CommandAPDU createInternalAuthenticateCommand(ByteArrayOutputStream pkout) {
        return new CommandAPDU(0, 0x88, 0x01, 0x0, pkout.toByteArray(), MAX_APDU_NE_EXT);
    }

    @NonNull
    CommandAPDU createRetrievePublicKeyCommand(byte[] msgCert) {
        return new CommandAPDU(0, 0x47, 0x81, 0x00, msgCert, MAX_APDU_NE_EXT);
    }

    @NonNull
    CommandAPDU createRetrieveCertificateCommand() {
        return new CommandAPDU(0, 0xA5, 0x03, 0x04,
                new byte[]{0x60, 0x04, 0x5C, 0x02, 0x7F, 0x21});
    }

    @NonNull
    CommandAPDU createShortApdu(CommandAPDU apdu) {
        int ne = Math.min(apdu.getNe(), MAX_APDU_NE);
        return new CommandAPDU(apdu.getCLA(), apdu.getINS(), apdu.getP1(), apdu.getP2(), apdu.getData(), ne);
    }

    @NonNull
    List<CommandAPDU> createChainedApdus(CommandAPDU apdu) {
        ArrayList<CommandAPDU> result = new ArrayList<>();

        int offset = 0;
        byte[] data = apdu.getData();
        int ne = Math.min(apdu.getNe(), MAX_APDU_NE);
        while (offset < data.length) {
            int curLen = Math.min(MAX_APDU_NC, data.length - offset);
            boolean last = offset + curLen >= data.length;
            int cla = apdu.getCLA() + (last ? 0 : MASK_CLA_CHAINING);

            CommandAPDU cmd =
                    new CommandAPDU(cla, apdu.getINS(), apdu.getP1(), apdu.getP2(), data, offset, curLen, ne);
            result.add(cmd);

            offset += curLen;
        }

        return result;
    }

    boolean isSuitableForShortApdu(CommandAPDU apdu) {
        return apdu.getData().length <= MAX_APDU_NC;
    }
}

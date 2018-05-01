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

package org.sufficientlysecure.keychain.ssh.signature;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.util.BigIntegers;
import org.sufficientlysecure.keychain.ssh.key.SshEncodedData;
import org.sufficientlysecure.keychain.ssh.utils.SshUtils;

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

public class SshSignatureConverter {

    private static String getRsaSignatureFormatId(int hashAlgorithm) throws NoSuchAlgorithmException {
        switch (hashAlgorithm) {
            case HashAlgorithmTags.SHA512:
                // https://tools.ietf.org/html/rfc8332
                return "rsa-sha2-512";

            case HashAlgorithmTags.SHA256:
                // https://tools.ietf.org/html/rfc8332
                return "rsa-sha2-256";

            case HashAlgorithmTags.SHA1:
                // https://tools.ietf.org/html/rfc4253
                return "ssh-rsa";

            default:
                throw new NoSuchAlgorithmException("Unknown hash algorithm");
        }
    }

    private static byte[] getEcDsaSignatureBlob(byte[] rawSignature) {
        BigInteger r = getR(rawSignature);
        BigInteger s = getS(rawSignature);

        SshEncodedData rsBlob = new SshEncodedData();
        rsBlob.putMPInt(r);
        rsBlob.putMPInt(s);

        return rsBlob.getBytes();
    }

    private static BigInteger getR(byte[] rawSignature) {
        ASN1Sequence asn1Sequence = getASN1Sequence(rawSignature);
        return ASN1Integer.getInstance(asn1Sequence.getObjectAt(0)).getValue();
    }

    private static BigInteger getS(byte[] rawSignature) {
        ASN1Sequence asn1Sequence = getASN1Sequence(rawSignature);
        return ASN1Integer.getInstance(asn1Sequence.getObjectAt(1)).getValue();
    }

    private static ASN1Sequence getASN1Sequence(byte[] rawSignature) {
        try {
            return (ASN1Sequence) ASN1Primitive.fromByteArray(rawSignature);
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not read ASN.1 object", e);
        }
    }

    private static byte[] getDsaSignatureBlob(byte[] rawSignature) {
        BigInteger r = getR(rawSignature);
        BigInteger s = getS(rawSignature);

        byte[] rByte = BigIntegers.asUnsignedByteArray(r);
        byte[] sByte = BigIntegers.asUnsignedByteArray(s);

        int integerLength = getDsaSignatureLength(rByte.length > sByte.length ? rByte.length : sByte.length);
        int rPaddingLength = integerLength - rByte.length;
        int sPaddingLength = integerLength - sByte.length;

        byte[] rsBlob = new byte[2 * integerLength];
        System.arraycopy(rByte, 0, rsBlob, rPaddingLength, rByte.length);
        System.arraycopy(sByte, 0, rsBlob, integerLength + sPaddingLength, sByte.length);

        return rsBlob;
    }

    private static int getDsaSignatureLength(int inLength) {
        if (inLength <= 20) {
            return 20;
        } else {
            return 32;
        }
    }

    public static byte[] getSshSignatureEdDsa(byte[] rawSignature) {
        SshEncodedData signature = new SshEncodedData();
        // https://tools.ietf.org/html/draft-ietf-curdle-ssh-ed25519-ed448-00
        signature.putString("ssh-ed25519");
        signature.putString(rawSignature);

        return signature.getBytes();
    }

    public static byte[] getSshSignatureDsa(byte[] rawSignature) {
        SshEncodedData signature = new SshEncodedData();
        // https://tools.ietf.org/html/rfc4253
        signature.putString("ssh-dss");
        signature.putString(getDsaSignatureBlob(rawSignature));

        return signature.getBytes();
    }

    public static byte[] getSshSignatureRsa(byte[] rawSignature, int hashAlgorithm) throws NoSuchAlgorithmException {
        SshEncodedData signature = new SshEncodedData();
        signature.putString(getRsaSignatureFormatId(hashAlgorithm));
        signature.putString(rawSignature);

        return signature.getBytes();
    }

    public static byte[] getSshSignatureEcDsa(byte[] rawSignature, String curveOid) throws NoSuchAlgorithmException {
        SshEncodedData signature = new SshEncodedData();
        // https://tools.ietf.org/html/rfc5656
        signature.putString("ecdsa-sha2-" + SshUtils.getCurveName(curveOid));
        signature.putString(getEcDsaSignatureBlob(rawSignature));

        return signature.getBytes();
    }
}

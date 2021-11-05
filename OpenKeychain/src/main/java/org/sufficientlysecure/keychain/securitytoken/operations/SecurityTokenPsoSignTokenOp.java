/*
 * Copyright (C) 2018 Schürmann & Breitmoser GbR
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

package org.sufficientlysecure.keychain.securitytoken.operations;


import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.securitytoken.CardException;
import org.sufficientlysecure.keychain.securitytoken.CommandApdu;
import org.sufficientlysecure.keychain.securitytoken.EcKeyFormat;
import org.sufficientlysecure.keychain.securitytoken.KeyFormat;
import org.sufficientlysecure.keychain.securitytoken.OpenPgpCapabilities;
import org.sufficientlysecure.keychain.securitytoken.RsaKeyFormat;
import org.sufficientlysecure.keychain.securitytoken.ResponseApdu;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenConnection;
import timber.log.Timber;


public class SecurityTokenPsoSignTokenOp {
    private final SecurityTokenConnection connection;

    public static SecurityTokenPsoSignTokenOp create(SecurityTokenConnection connection) {
        return new SecurityTokenPsoSignTokenOp(connection);
    }

    private SecurityTokenPsoSignTokenOp(SecurityTokenConnection connection) {
        this.connection = connection;
    }

    private byte[] prepareDsi(byte[] hash, int hashAlgo) throws IOException {
        byte[] dsi;

        Timber.i("Hash: " + hashAlgo);
        switch (hashAlgo) {
            case HashAlgorithmTags.SHA1:
                if (hash.length != 20) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 10!");
                }
                dsi = Arrays.concatenate(Hex.decode(
                        "3021" // Tag/Length of Sequence, the 0x21 includes all following 33 bytes
                                + "3009" // Tag/Length of Sequence, the 0x09 are the following header bytes
                                + "0605" + "2B0E03021A" // OID of SHA1
                                + "0500" // TLV coding of ZERO
                                + "0414"), hash); // 0x14 are 20 hash bytes
                break;
            case HashAlgorithmTags.RIPEMD160:
                if (hash.length != 20) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 20!");
                }
                dsi = Arrays.concatenate(Hex.decode("3021300906052B2403020105000414"), hash);
                break;
            case HashAlgorithmTags.SHA224:
                if (hash.length != 28) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 28!");
                }
                dsi = Arrays.concatenate(Hex.decode("302D300D06096086480165030402040500041C"), hash);
                break;
            case HashAlgorithmTags.SHA256:
                if (hash.length != 32) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 32!");
                }
                dsi = Arrays.concatenate(Hex.decode("3031300D060960864801650304020105000420"), hash);
                break;
            case HashAlgorithmTags.SHA384:
                if (hash.length != 48) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 48!");
                }
                dsi = Arrays.concatenate(Hex.decode("3041300D060960864801650304020205000430"), hash);
                break;
            case HashAlgorithmTags.SHA512:
                if (hash.length != 64) {
                    throw new IOException("Bad hash length (" + hash.length + ", expected 64!");
                }
                dsi = Arrays.concatenate(Hex.decode("3051300D060960864801650304020305000440"), hash);
                break;
            default:
                throw new IOException("Not supported hash algo!");
        }
        return dsi;
    }

    private byte[] prepareData(byte[] hash, int hashAlgo, KeyFormat keyFormat) throws IOException {
        if (keyFormat instanceof RsaKeyFormat) {
            return prepareDsi(hash, hashAlgo);
        } else if (keyFormat instanceof EcKeyFormat) {
            return hash;
        } else {
            throw new IOException("Not supported key type!");
        }
    }

    private byte[] encodeSignature(byte[] signature, KeyFormat keyFormat) throws IOException {
        // Make sure the signature we received is actually the expected number of bytes long!
        if (keyFormat instanceof RsaKeyFormat) {
            // no encoding necessary
            int modulusLength = ((RsaKeyFormat) keyFormat).modulusLength();
            if (signature.length != (modulusLength / 8)) {
                throw new IOException("Bad signature length! Expected " + (modulusLength / 8) +
                        " bytes, got " + signature.length);
            }

            return signature;
        } else if (keyFormat instanceof EcKeyFormat) {
            EcKeyFormat ecKeyFormat = (EcKeyFormat) keyFormat;
            if (ecKeyFormat.isEdDsa()) {
                return signature;
            }

            // "plain" encoding, see https://github.com/open-keychain/open-keychain/issues/2108
            if (signature.length % 2 != 0) {
                throw new IOException("Bad signature length!");
            }
            byte[] br = new byte[signature.length / 2];
            byte[] bs = new byte[signature.length / 2];
            for (int i = 0; i < br.length; ++i) {
                br[i] = signature[i];
                bs[i] = signature[br.length + i];
            }
            if (br[0] == 0x00 && (br[1] & 0x80) == 0) {
                br = Arrays.copyOfRange(br, 1, br.length);
            }
            if (br[0] < 0) {
                byte[] paddedBr = new byte[br.length + 1];
                paddedBr[0] = 0;
                System.arraycopy(br, 0, paddedBr, 1, br.length);
                br = paddedBr;
            }
            if (bs[0] == 0x00 && (bs[1] & 0x80) == 0) {
                bs = Arrays.copyOfRange(bs, 1, bs.length);
            }
            if (bs[0] < 0) {
                byte[] paddedBs = new byte[bs.length + 1];
                paddedBs[0] = 0;
                System.arraycopy(bs, 0, paddedBs, 1, bs.length);
                bs = paddedBs;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ASN1OutputStream out = ASN1OutputStream.create(baos);
            out.writeObject(new DERSequence(new ASN1Encodable[]{new ASN1Integer(br), new ASN1Integer(bs)}));
            out.flush();
            return baos.toByteArray();
        } else {
            throw new IOException("Not supported key format!");
        }
    }

    /**
     * Call COMPUTE DIGITAL SIGNATURE command and returns the MPI value
     *
     * @param hash the hash for signing
     * @return a big integer representing the MPI for the given hash
     */
    public byte[] calculateSignature(byte[] hash, int hashAlgo) throws IOException {
        connection.verifyPinForSignature();

        OpenPgpCapabilities openPgpCapabilities = connection.getOpenPgpCapabilities();
        KeyFormat signKeyFormat = openPgpCapabilities.getSignKeyFormat();

        byte[] data = prepareData(hash, hashAlgo, signKeyFormat);

        // Command APDU for PERFORM SECURITY OPERATION: COMPUTE DIGITAL SIGNATURE (page 37)
        CommandApdu command = connection.getCommandFactory().createComputeDigitalSignatureCommand(data);
        ResponseApdu response = connection.communicate(command);

        connection.invalidateSingleUsePw1();

        if (!response.isSuccess()) {
            throw new CardException("Failed to sign", response.getSw());
        }

        return encodeSignature(response.getData(), signKeyFormat);
    }

    /**
     * Call INTERNAL AUTHENTICATE command and returns the MPI value
     *
     * @param hash the hash for signing
     * @return a big integer representing the MPI for the given hash
     */
    public byte[] calculateAuthenticationSignature(byte[] hash, int hashAlgo) throws IOException {
        connection.verifyPinForOther();

        OpenPgpCapabilities openPgpCapabilities = connection.getOpenPgpCapabilities();
        KeyFormat authKeyFormat = openPgpCapabilities.getAuthKeyFormat();

        byte[] data = prepareData(hash, hashAlgo, authKeyFormat);

        // Command APDU for INTERNAL AUTHENTICATE (page 55)
        CommandApdu command = connection.getCommandFactory().createInternalAuthCommand(data);
        ResponseApdu response = connection.communicate(command);

        if (!response.isSuccess()) {
            throw new CardException("Failed to sign", response.getSw());
        }

        return encodeSignature(response.getData(), authKeyFormat);
    }
}

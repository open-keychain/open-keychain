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

package org.sufficientlysecure.keychain.securitytoken;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.AlgorithmParameters;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.EllipticCurve;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;

import android.content.Context;
import android.support.annotation.NonNull;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.bouncycastle.asn1.nist.NISTNamedCurves;
import org.bouncycastle.asn1.x9.ECNamedCurveTable;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.math.ec.ECCurve;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.util.Arrays;
import org.sufficientlysecure.keychain.ui.SettingsSmartPGPAuthoritiesActivity;
import org.sufficientlysecure.keychain.util.Preferences;


class SCP11bSecureMessaging implements SecureMessaging {

    private static final byte OPENPGP_SECURE_MESSAGING_CLA_MASK = (byte)0x04;
    private static final byte OPENPGP_SECURE_MESSAGING_KEY_ATTRIBUTES_TAG = (byte)0xD4;

    private static final int AES_BLOCK_SIZE = 128 / 8;

    private static final int SCP11_MAC_LENGTH = AES_BLOCK_SIZE / 2;

    private static final String SCP11_SYMMETRIC_ALGO = "AES";
    private static final String SCP11_CIPHER_ALGO = "AES/CBC/NoPadding";
    private static final String SCP11_MAC_ALGO = "AESCMAC";

    private static final String SCP11B_KEY_AGREEMENT_ALGO = "ECDH";
    private static final String SCP11B_KEY_AGREEMENT_KEY_TYPE = "ECDH";
    private static final String SCP11B_KEY_AGREEMENT_KEY_ALGO = "EC";
    private static final String SCP11B_KEY_DERIVATION_ALGO = "SHA256";

    private static final String CERTIFICATE_FORMAT = "X.509";

    private static final String PROVIDER = "BC";

    private static SecureRandom srand;
    private static KeyFactory ecdhFactory;
    private static CertificateFactory certFactory;

    private SecretKey mSEnc;
    private SecretKey mSMac;
    private SecretKey mSRMac;

    private short mEncryptionCounter;

    private byte[] mMacChaining;

    private SCP11bSecureMessaging() {
    }

    private void setKeys(@NonNull final byte[] sEnc,
                         @NonNull final byte[] sMac,
                         @NonNull final byte[] sRmac,
                         @NonNull final byte[] receipt)
            throws SecureMessagingException {

        if ((sEnc.length != sMac.length)
                || (sEnc.length != sRmac.length)
                || (receipt.length != AES_BLOCK_SIZE)) {
            throw new SecureMessagingException("incoherent SCP11b key set");
        }

        mSEnc = new SecretKeySpec(sEnc, SCP11_SYMMETRIC_ALGO);
        mSMac = new SecretKeySpec(sMac, SCP11_SYMMETRIC_ALGO);
        mSRMac = new SecretKeySpec(sRmac, SCP11_SYMMETRIC_ALGO);
        mEncryptionCounter = 0;
        mMacChaining = receipt;
    }

    @Override
    public void clearSession() {
        mSEnc = null;
        mSMac = null;
        mSRMac = null;
        mEncryptionCounter = 0;
        mMacChaining = null;
    }

    @Override
    public boolean isEstablished() {
        return (mSEnc != null)
                && (mSMac != null)
                && (mSRMac != null)
                && (mMacChaining != null);
    }

    private static ECParameterSpec getAlgorithmParameterSpec(final ECKeyFormat kf)
            throws NoSuchProviderException, NoSuchAlgorithmException, InvalidParameterSpecException {
        final AlgorithmParameters algoParams = AlgorithmParameters.getInstance(SCP11B_KEY_AGREEMENT_KEY_ALGO, PROVIDER);

        algoParams.init(new ECGenParameterSpec(ECNamedCurveTable.getName(kf.getCurveOID())));

        return algoParams.getParameterSpec(ECParameterSpec.class);
    }


    private static ECPublicKey newECDHPublicKey(final ECKeyFormat kf, byte[] data)
            throws InvalidKeySpecException, NoSuchAlgorithmException,
                   InvalidParameterSpecException, NoSuchProviderException {
        if (ecdhFactory == null) {
            ecdhFactory = KeyFactory.getInstance(SCP11B_KEY_AGREEMENT_KEY_TYPE, PROVIDER);
        }

        final X9ECParameters params = NISTNamedCurves.getByOID(kf.getCurveOID());
        if (params == null) {
            throw new InvalidParameterSpecException("unsupported curve");
        }

        final ECCurve curve = params.getCurve();
        final ECPoint p = curve.decodePoint(data);
        if (!p.isValid()) {
            throw new InvalidKeySpecException("invalid EC point");
        }

        final java.security.spec.ECPublicKeySpec pk = new java.security.spec.ECPublicKeySpec(
                new java.security.spec.ECPoint(p.getAffineXCoord().toBigInteger(), p.getAffineYCoord().toBigInteger()),
                getAlgorithmParameterSpec(kf));

        return (ECPublicKey)(ecdhFactory.generatePublic(pk));
    }

    private static KeyPair generateECDHKeyPair(final ECKeyFormat kf)
            throws NoSuchProviderException, NoSuchAlgorithmException,
                   InvalidParameterSpecException, InvalidAlgorithmParameterException {
        final KeyPairGenerator gen = KeyPairGenerator.getInstance(SCP11B_KEY_AGREEMENT_KEY_ALGO, PROVIDER);

        if (srand == null) {
            srand = new SecureRandom();
        }

        gen.initialize(getAlgorithmParameterSpec(kf), srand);

        return gen.generateKeyPair();
    }

    private static ECPublicKey verifyCertificate(final Context ctx,
                                                 final ECKeyFormat kf,
                                                 final byte[] data) throws IOException {
        try {

            if (certFactory == null) {
                certFactory = CertificateFactory.getInstance(CERTIFICATE_FORMAT, PROVIDER);
            }

            final ECParameterSpec kfParams = getAlgorithmParameterSpec(kf);

            final Certificate cert = certFactory.generateCertificate(new ByteArrayInputStream(data));
            if (!(cert instanceof X509Certificate)) {
                throw new IOException("invalid card certificate");
            }
            final X509Certificate cardCert = (X509Certificate) cert;

            final PublicKey pk = cardCert.getPublicKey();
            if (!(pk instanceof ECPublicKey)) {
                throw new IOException("invalid card public key");
            }
            final ECPublicKey cardPk = (ECPublicKey) pk;
            final ECParameterSpec cardPkParams = cardPk.getParams();

            if (!kfParams.getCurve().equals(cardPkParams.getCurve())) {
                throw new IOException("incoherent card certificate/public key format");
            }

            final KeyStore ks = SettingsSmartPGPAuthoritiesActivity.readKeystore(ctx);

            if (ks == null) {
                throw new KeyStoreException("no keystore found");
            }

            final X509CertSelector targetConstraints = new X509CertSelector();
            targetConstraints.setCertificate(cardCert);

            final ArrayList al = new ArrayList();
            al.add(cardCert);
            final CollectionCertStoreParameters certStoreParams = new CollectionCertStoreParameters(al);
            final CertStore certStore = CertStore.getInstance("Collection", certStoreParams, PROVIDER);

            final PKIXBuilderParameters pkixParams = new PKIXBuilderParameters(ks, targetConstraints);
            pkixParams.setRevocationEnabled(false);
            pkixParams.addCertStore(certStore);

            final CertPathBuilder builder = CertPathBuilder.getInstance(CertPathBuilder.getDefaultType(), PROVIDER);

            final PKIXCertPathBuilderResult result =
                    (PKIXCertPathBuilderResult) builder.build(pkixParams);

            return cardPk;

        } catch (CertificateException e) {
            throw new IOException("invalid card certificate (" + e.getMessage() + ")");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("unknown algorithm (" + e.getMessage() + ")");
        } catch (InvalidParameterSpecException e) {
            throw new IOException("invalid card key parameters (" + e.getMessage() + ")");
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            throw new IOException("illegal argument (" + e.getMessage() + ")");
        } catch (NoSuchProviderException e) {
            throw new IOException("unavailable crypto (" + e.getMessage() + ")");
        } catch (KeyStoreException e) {
            throw new IOException("failed to build keystore (" + e.getMessage() + ")");
        } catch (InvalidAlgorithmParameterException e) {
            throw new IOException("invalid algorithm parameter (" + e.getMessage() + ")");
        } catch (CertPathBuilderException e) {
            throw new IOException("invalid certificate path (" + e.getMessage() + ")");
        }
    }


    static void establish(final SecurityTokenConnection t, final Context ctx, OpenPgpCommandApduFactory commandFactory)
            throws SecureMessagingException, IOException {

        CommandApdu cmd;
        ResponseApdu resp;
        Iso7816TLV[] tlvs;

        t.clearSecureMessaging();

        // retrieving key algorithm
        cmd = commandFactory.createGetDataCommand(0x00, OPENPGP_SECURE_MESSAGING_KEY_ATTRIBUTES_TAG);
        resp = t.communicate(cmd);
        if (!resp.isSuccess()) {
            throw new SecureMessagingException("failed to retrieve secure messaging key attributes");
        }
        tlvs = Iso7816TLV.readList(resp.getData(), true);
        if ((tlvs == null)
            || (tlvs.length != 1)
            || ((byte)tlvs[0].mT != OPENPGP_SECURE_MESSAGING_KEY_ATTRIBUTES_TAG)) {
            throw new SecureMessagingException("invalid format of secure messaging key attributes");
        }

        final KeyFormat kf = KeyFormat.fromBytes(tlvs[0].mV);

        if (kf.keyFormatType() != KeyFormat.KeyFormatType.ECKeyFormatType) {
            throw new SecureMessagingException("invalid format of secure messaging key");
        }

        final ECKeyFormat eckf = (ECKeyFormat)kf;

        if (eckf.getCurveOID() == null) {
            throw new SecureMessagingException("unsupported curve");
        }

        try {
            ECPublicKey pkcard = null;

            final Preferences prefs = Preferences.getPreferences(ctx);

            if (prefs != null && prefs.getExperimentalSmartPGPAuthoritiesEnable()) {
                // retrieving certificate
                cmd = commandFactory.createSelectSecureMessagingCertificateCommand();
                resp = t.communicate(cmd);
                if (!resp.isSuccess()) {
                    throw new SecureMessagingException("failed to select secure messaging certificate");
                }
                cmd = commandFactory.createGetDataCardHolderCertCommand();
                resp = t.communicate(cmd);
                if (!resp.isSuccess()) {
                    throw new SecureMessagingException("failed to retrieve secure messaging certificate");
                }

                pkcard = verifyCertificate(ctx, eckf, resp.getData());

            } else {
                cmd = commandFactory.createRetrieveSecureMessagingPublicKeyCommand();
                resp = t.communicate(cmd);
                if (!resp.isSuccess()) {
                    throw new SecureMessagingException("failed to retrieve secure messaging public key");
                }
                tlvs = Iso7816TLV.readList(resp.getData(), true);
                if ((tlvs == null)
                        || (tlvs.length != 1)
                        || ((short)tlvs[0].mT != (short)0x7f49)) {
                    throw new SecureMessagingException("invalid format of secure messaging key");
                }
                tlvs = Iso7816TLV.readList(tlvs[0].mV, true);
                if ((tlvs == null)
                        || (tlvs.length != 1)
                        || ((byte)tlvs[0].mT != (byte)0x86)) {
                    throw new SecureMessagingException("invalid format of secure messaging key");
                }

                pkcard = newECDHPublicKey(eckf, tlvs[0].mV);
            }

            if (pkcard == null) {
                throw new SecureMessagingException("No key in token for secure messaging");
            }

            final EllipticCurve curve = pkcard.getParams().getCurve();
            final int fieldSize = curve.getField().getFieldSize();
            int keySize;

            if(fieldSize < 512) {
                keySize = 16;
            } else {
                keySize = 32;
            }

            final KeyPair ekoce = generateECDHKeyPair(eckf);
            final ECPublicKey epkoce = (ECPublicKey)ekoce.getPublic();
            final ECPrivateKey eskoce = (ECPrivateKey)ekoce.getPrivate();

            final byte[] crt_template = new byte[] {
                    (byte)0xA6, (byte)0x0D,
                    (byte)0x90, (byte)0x02, (byte)0x11, (byte)0x00,
                    (byte)0x95, (byte)0x01, (byte)0x3C,
                    (byte)0x80, (byte)0x01, (byte)0x88,
                    (byte)0x81, (byte)0x01, (byte)keySize,
                    (byte)0x5F, (byte)0x49 };

            int csize = (int)Math.ceil(epkoce.getParams().getCurve().getField().getFieldSize() / 8.0);

            ByteArrayOutputStream pkout = new ByteArrayOutputStream(), bout = new ByteArrayOutputStream();

            pkout.write((byte)0x04);
            SecurityTokenUtils.writeBits(pkout, epkoce.getW().getAffineX(), csize);
            SecurityTokenUtils.writeBits(pkout, epkoce.getW().getAffineY(), csize);

            bout.write(crt_template);
            bout.write(SecurityTokenUtils.encodeLength(pkout.size()));
            pkout.writeTo(bout);
            pkout = bout;

            cmd = commandFactory.createInternalAuthForSecureMessagingCommand(pkout.toByteArray());
            resp = t.communicate(cmd);
            if (!resp.isSuccess()) {
                throw new SecureMessagingException("failed to initiate internal authenticate");
            }

            tlvs = Iso7816TLV.readList(resp.getData(), true);
            if ((tlvs == null)
                    || (tlvs.length != 2)
                    || (tlvs[0].mT == tlvs[1].mT)) {
                throw new SecureMessagingException("invalid internal authenticate response");
            }

            byte[] receipt = null;
            ECPublicKey epkcard = null;

            for (int i = 0; i < tlvs.length; ++i) {
                switch (tlvs[i].mT) {
                    case 0x86:
                        if (tlvs[i].mL != AES_BLOCK_SIZE) {
                            throw new SecureMessagingException("invalid size for receipt");
                        }
                        receipt = tlvs[i].mV;
                        break;

                    case 0x5F49:
                        epkcard = newECDHPublicKey(eckf, tlvs[i].mV);
                        break;

                    default:
                        throw new SecureMessagingException("unexpected data in internal authenticate response");
                }
            }

            final KeyAgreement ecdhKa = KeyAgreement.getInstance(SCP11B_KEY_AGREEMENT_ALGO, PROVIDER);
            bout = new ByteArrayOutputStream();

            //compute ShSe
            ecdhKa.init(eskoce);
            ecdhKa.doPhase(epkcard, true);
            bout.write(ecdhKa.generateSecret());

            //compute ShSs
            ecdhKa.init(eskoce);
            ecdhKa.doPhase(pkcard, true);
            bout.write(ecdhKa.generateSecret());

            csize = bout.size() + 3;

            bout.write(new byte[] {
                        (byte)0, (byte)0, (byte)0, (byte)0,
                        crt_template[8], crt_template[11],
                        (byte)keySize });

            byte[] shs =  bout.toByteArray();

            //key derivation
            final MessageDigest h = MessageDigest.getInstance(SCP11B_KEY_DERIVATION_ALGO, PROVIDER);

            bout = new ByteArrayOutputStream();
            while (bout.size() < 4 * keySize) {
                ++shs[csize];
                bout.write(h.digest(shs));
            }

            shs =  bout.toByteArray();

            final byte[] rkey = Arrays.copyOfRange(shs, 0, keySize);
            final byte[] sEnc = Arrays.copyOfRange(shs, keySize, 2 * keySize);
            final byte[] sMac = Arrays.copyOfRange(shs, 2 * keySize, 3 * keySize);
            final byte[] sRmac = Arrays.copyOfRange(shs, 3 * keySize, 4 * keySize);

            //receipt computation
            final Mac mac = Mac.getInstance(SCP11_MAC_ALGO, PROVIDER);

            mac.init(new SecretKeySpec(rkey, SCP11_SYMMETRIC_ALGO));

            shs = resp.getData();
            mac.update(pkout.toByteArray());
            mac.update(shs, 0, shs.length - 2 - AES_BLOCK_SIZE);
            shs = mac.doFinal();

            for(int i = 0; i < AES_BLOCK_SIZE; ++i) {
                if (shs[i] != receipt[i]) {
                    throw new SecureMessagingException("corrupted receipt!");
                }
            }

            final SCP11bSecureMessaging sm = new SCP11bSecureMessaging();
            sm.setKeys(sEnc, sMac, sRmac, receipt);

            t.setSecureMessaging(sm);

        } catch (InvalidKeySpecException e) {
            throw new SecureMessagingException("invalid key specification : " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            throw new SecureMessagingException("unknown EC key algorithm : " + e.getMessage());
        } catch (InvalidParameterSpecException e) {
            throw new SecureMessagingException("invalid ECDH parameters : " + e.getMessage());
        } catch (NoSuchProviderException e) {
            throw  new SecureMessagingException("unknown provider " + PROVIDER);
        } catch (InvalidAlgorithmParameterException e) {
            throw  new SecureMessagingException("invalid algorithm parameters : " + e.getMessage());
        } catch (InvalidKeyException e) {
            throw  new SecureMessagingException("invalid key : " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new SecureMessagingException("illegal argument (" + e.getMessage() + ")");
        }
    }



    @Override
    public CommandApdu encryptAndSign(CommandApdu apdu)
            throws SecureMessagingException {

        if (!isEstablished()) {
            throw new SecureMessagingException("not established");
        }

        ++mEncryptionCounter;
        if(mEncryptionCounter <= 0) {
            throw new SecureMessagingException("exhausted encryption counter");
        }

        try {

            byte[] data = apdu.getData();

            if (data.length > 0) {
                final Cipher cipher = Cipher.getInstance(SCP11_CIPHER_ALGO);

                byte[] iv = new byte[AES_BLOCK_SIZE];
                Arrays.fill(iv, (byte)0);
                cipher.init(Cipher.ENCRYPT_MODE, mSEnc, new IvParameterSpec(iv));

                iv[AES_BLOCK_SIZE - 2] = (byte)((mEncryptionCounter >> 8) & 0xff);
                iv[AES_BLOCK_SIZE - 1] = (byte)(mEncryptionCounter & 0xff);

                iv = cipher.doFinal(iv);

                cipher.init(Cipher.ENCRYPT_MODE, mSEnc, new IvParameterSpec(iv));

                final byte[] pdata = new byte[data.length + AES_BLOCK_SIZE - (data.length % AES_BLOCK_SIZE)];
                System.arraycopy(data, 0, pdata, 0, data.length);
                pdata[data.length] = (byte)0x80;

                Arrays.fill(data, (byte)0);

                data = cipher.doFinal(pdata);

                Arrays.fill(pdata, (byte)0);
                Arrays.fill(iv, (byte)0);
            }


            final int lcc = data.length + SCP11_MAC_LENGTH;

            final byte[] odata = new byte[4 + 3 + lcc + 3];
            int ooff = 0;

            odata[ooff++] = (byte) (((byte) apdu.getCLA()) | OPENPGP_SECURE_MESSAGING_CLA_MASK);
            odata[ooff++] = (byte) apdu.getINS();
            odata[ooff++] = (byte) apdu.getP1();
            odata[ooff++] = (byte) apdu.getP2();

            if (lcc > 0xff) {
                odata[ooff++] = (byte) 0;
                odata[ooff++] = (byte) ((lcc >> 8) & 0xff);
            }
            odata[ooff++] = (byte) (lcc & 0xff);

            System.arraycopy(data, 0, odata, ooff, data.length);
            ooff += data.length;

            Arrays.fill(data, (byte)0);

            final Mac mac = Mac.getInstance(SCP11_MAC_ALGO, PROVIDER);
            mac.init(mSMac);
            mac.update(mMacChaining);
            mac.update(odata, 0, ooff);
            mMacChaining = mac.doFinal();

            System.arraycopy(mMacChaining, 0, odata, ooff, SCP11_MAC_LENGTH);
            ooff += SCP11_MAC_LENGTH;

            if (lcc > 0xff) {
                odata[ooff++] = (byte) 0;
            }
            odata[ooff++] = (byte) 0;

            apdu = CommandApdu.fromBytes(odata, 0, ooff);

            Arrays.fill(odata, (byte)0);

            return apdu;

        } catch (NoSuchAlgorithmException e) {
            throw new SecureMessagingException("unavailable algorithm : " + e.getMessage());
        } catch (NoSuchProviderException e) {
            throw new SecureMessagingException("unavailable provider : " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            throw new SecureMessagingException("unavailable padding algorithm : " + e.getMessage());
        } catch (InvalidKeyException e) {
            throw new SecureMessagingException("invalid key : " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecureMessagingException("invalid IV : " + e.getMessage());
        } catch (BadPaddingException e) {
            throw new SecureMessagingException("invalid IV : " + e.getMessage());
        } catch (IllegalBlockSizeException e) {
            throw new SecureMessagingException("invalid block size : " + e.getMessage());
        }
    }


    @Override
    public ResponseApdu verifyAndDecrypt(ResponseApdu apdu)
            throws SecureMessagingException {

        if (!isEstablished()) {
            throw new SecureMessagingException("not established");
        }

        byte[] data = apdu.getData();

        if ((data.length == 0) && !apdu.isSuccess() &&
                (apdu.getSw1() != 0x62) &&
                (apdu.getSw1() != 0x63)) {
            return apdu;
        }

        if (data.length < SCP11_MAC_LENGTH) {
            throw new SecureMessagingException("missing or incomplete MAC in response");
        }

        try {

            final Mac mac = Mac.getInstance(SCP11_MAC_ALGO, PROVIDER);
            mac.init(mSRMac);

            mac.update(mMacChaining);
            if ((data.length - SCP11_MAC_LENGTH) > 0) {
                mac.update(data, 0, data.length - SCP11_MAC_LENGTH);
            }
            mac.update((byte) apdu.getSw1());
            mac.update((byte) apdu.getSw2());

            final byte[] sig = mac.doFinal();

            for (int i = 0; i < SCP11_MAC_LENGTH; ++i) {
                if ((i >= sig.length)
                        || (sig[i] != data[data.length - SCP11_MAC_LENGTH + i])) {
                    throw new SecureMessagingException("corrupted integrity");
                }
            }

            if (((data.length - SCP11_MAC_LENGTH) % AES_BLOCK_SIZE) != 0) {
                throw new SecureMessagingException("invalid encrypted data size");
            }

            if (data.length > SCP11_MAC_LENGTH) {
                final Cipher cipher = Cipher.getInstance(SCP11_CIPHER_ALGO);

                byte[] iv = new byte[AES_BLOCK_SIZE];
                Arrays.fill(iv,(byte)0);
                cipher.init(Cipher.ENCRYPT_MODE, mSEnc, new IvParameterSpec(iv));

                iv[0] = (byte) 0x80;
                iv[AES_BLOCK_SIZE - 2] = (byte) ((mEncryptionCounter >> 8) & 0xff);
                iv[AES_BLOCK_SIZE - 1] = (byte) (mEncryptionCounter & 0xff);

                iv = cipher.doFinal(iv);

                cipher.init(Cipher.DECRYPT_MODE, mSEnc, new IvParameterSpec(iv));
                data = cipher.doFinal(data, 0, data.length - SCP11_MAC_LENGTH);

                int i = data.length - 1;
                while ((0 < i) && (data[i] == (byte) 0)) --i;

                if ((i <= 0) || (data[i] != (byte) 0x80)) {
                    throw new SecureMessagingException("invalid data padding after decryption");
                }

                final byte[] datasw = new byte[i + 2];
                System.arraycopy(data, 0, datasw, 0, i);
                datasw[datasw.length - 2] = (byte) apdu.getSw1();
                datasw[datasw.length - 1] = (byte) apdu.getSw2();

                Arrays.fill(data, (byte) 0);

                data = datasw;
            } else {
                data = new byte[2];
                data[0] = (byte) apdu.getSw1();
                data[1] = (byte) apdu.getSw2();
            }

            apdu = ResponseApdu.fromBytes(data);

            return apdu;

        } catch (NoSuchAlgorithmException e) {
            throw new SecureMessagingException("unavailable algorithm : " + e.getMessage());
        } catch (NoSuchProviderException e) {
            throw new SecureMessagingException("unknown provider : " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            throw new SecureMessagingException("unavailable padding algorithm : " + e.getMessage());
        } catch (InvalidKeyException e) {
            throw new SecureMessagingException("invalid key : " + e.getMessage());
        } catch (BadPaddingException e) {
            throw new SecureMessagingException("invalid IV : " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            throw new SecureMessagingException("invalid IV : " + e.getMessage());
        } catch (IllegalBlockSizeException e) {
            throw new SecureMessagingException("invalid block size : " + e.getMessage());
        }
    }

}

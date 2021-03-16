package org.sufficientlysecure.keychain.securitytoken;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cryptlib.CryptlibObjectIdentifiers;
import org.bouncycastle.asn1.gnu.GNUObjectIdentifiers;
import org.bouncycastle.asn1.sec.SECObjectIdentifiers;
import org.bouncycastle.asn1.teletrust.TeleTrusTObjectIdentifiers;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;

import timber.log.Timber;

public class EcObjectIdentifiers {

    public static final ASN1ObjectIdentifier NIST_P_256 = SECObjectIdentifiers.secp256r1;
    public static final ASN1ObjectIdentifier NIST_P_384 = SECObjectIdentifiers.secp384r1;
    public static final ASN1ObjectIdentifier NIST_P_521 = SECObjectIdentifiers.secp521r1;
    public static final ASN1ObjectIdentifier BRAINPOOL_P256_R1 = TeleTrusTObjectIdentifiers.brainpoolP256r1;
    public static final ASN1ObjectIdentifier BRAINPOOL_P512_R1 = TeleTrusTObjectIdentifiers.brainpoolP512r1;
    public static final ASN1ObjectIdentifier ED25519 = GNUObjectIdentifiers.Ed25519; // for use with EdDSA
    public static final ASN1ObjectIdentifier X25519 = CryptlibObjectIdentifiers.curvey25519; // for use with ECDH

    public static HashSet<ASN1ObjectIdentifier> sOids = new HashSet<>(Arrays.asList(
            NIST_P_256, NIST_P_384, NIST_P_521, BRAINPOOL_P256_R1, BRAINPOOL_P512_R1, ED25519, X25519
    ));

    public static ASN1ObjectIdentifier parseOid(byte[] oidField) {
        ASN1ObjectIdentifier asn1CurveOid = oidFieldToOidAsn1(oidField);
        if (sOids.contains(asn1CurveOid)) {
            return asn1CurveOid;
        }
        Timber.w("Unknown curve OID: %s. Could be YubiKey firmware bug < 5.2.8. Trying again with last byte removed.", asn1CurveOid.getId());

        // https://bugs.chromium.org/p/chromium/issues/detail?id=1120933#c10
        // The OpenPGP applet of a Yubikey with firmware version below 5.2.8 appends
        // a potentially arbitrary byte to the intended byte representation of an ECC
        // curve OID. This case is handled by retrying the decoding with the last
        // byte stripped if the resulting OID does not label a known curve.
        byte[] oidRemoveLastByte = Arrays.copyOf(oidField, oidField.length - 1);
        ASN1ObjectIdentifier asn1CurveOidYubikey = oidFieldToOidAsn1(oidRemoveLastByte);
        if (sOids.contains(asn1CurveOidYubikey)) {
            Timber.w("Detected curve OID: %s", asn1CurveOidYubikey.getId());
        } else {
            Timber.e("Still Unknown curve OID: %s", asn1CurveOidYubikey.getId());
        }
        return asn1CurveOidYubikey;
    }

    public static byte[] asn1ToOidField(ASN1ObjectIdentifier oidAsn1) {
        byte[] encodedAsn1Oid;
        try {
            encodedAsn1Oid = oidAsn1.getEncoded();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to encode curve OID!");
        }
        byte[] oidField = new byte[encodedAsn1Oid.length - 2];
        System.arraycopy(encodedAsn1Oid, 2, oidField, 0, encodedAsn1Oid.length - 2);

        return oidField;
    }

    public static ASN1ObjectIdentifier oidFieldToOidAsn1(byte[] oidField) {
        final byte[] boid = new byte[2 + oidField.length];
        boid[0] = (byte) 0x06;
        boid[1] = (byte) oidField.length;
        System.arraycopy(oidField, 0, boid, 2, oidField.length);
        return ASN1ObjectIdentifier.getInstance(boid);
    }

}
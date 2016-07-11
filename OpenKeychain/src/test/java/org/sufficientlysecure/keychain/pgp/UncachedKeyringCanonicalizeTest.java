/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2014 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.pgp;

import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.Packet;
import org.bouncycastle.bcpg.PacketTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.UserIDPacket;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;
import org.bouncycastle.util.Strings;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper.RawPacket;
import org.sufficientlysecure.keychain.util.Passphrase;

import java.io.ByteArrayInputStream;
import java.security.Security;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;


/** Tests for the UncachedKeyring.canonicalize method.
 *
 * This is a complex and crypto-relevant method, which takes care of sanitizing keyrings.
 * Test cases are made for all its assertions.
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 23, manifest = "src/main/AndroidManifest.xml")
public class UncachedKeyringCanonicalizeTest {

    static UncachedKeyRing staticRing;
    static int totalPackets;
    UncachedKeyRing ring;
    ArrayList<RawPacket> onlyA = new ArrayList<>();
    ArrayList<RawPacket> onlyB = new ArrayList<>();
    OperationResult.OperationLog log = new OperationResult.OperationLog();
    PGPSignatureSubpacketGenerator subHashedPacketsGen;
    PGPSecretKey secretKey;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        ShadowLog.stream = System.out;

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Algorithm.ECDH, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.ENCRYPT_COMMS, 0L));

        parcel.mAddUserIds.add("twi");
        parcel.mAddUserIds.add("pink");
        {
            WrappedUserAttribute uat = WrappedUserAttribute.fromSubpacket(100,
                    "sunshine, sunshine, ladybugs awake~".getBytes());
            parcel.mAddUserAttribute.add(uat);
        }

        // passphrase is tested in PgpKeyOperationTest, just use empty here
        parcel.mPassphrase = new Passphrase();
        PgpKeyOperation op = new PgpKeyOperation(null);

        PgpEditKeyResult result = op.createSecretKeyRing(parcel);
        Assert.assertTrue("initial test key creation must succeed", result.success());
        staticRing = result.getRing();
        Assert.assertNotNull("initial test key creation must succeed", staticRing);

        staticRing = staticRing.canonicalize(new OperationLog(), 0).getUncachedKeyRing();

        // just for later reference
        totalPackets = 11;

        // we sleep here for a second, to make sure all new certificates have different timestamps
        Thread.sleep(1000);
    }

    @Before public void setUp() throws Exception {
        // show Log.x messages in system.out
        ShadowLog.stream = System.out;
        ring = staticRing;

        subHashedPacketsGen = new PGPSignatureSubpacketGenerator();
        secretKey = new PGPSecretKeyRing(ring.getEncoded(), new JcaKeyFingerprintCalculator())
                .getSecretKey();
    }

    /** Make sure the assumptions made about the generated ring packet structure are valid. */
    @Test public void testGeneratedRingStructure() throws Exception {

        Iterator<RawPacket> it = KeyringTestingHelper.parseKeyring(ring.getEncoded());

        Assert.assertEquals("packet #0 should be secret key",
                PacketTags.SECRET_KEY, it.next().tag);

        Assert.assertEquals("packet #1 should be user id",
                PacketTags.USER_ID, it.next().tag);
        Assert.assertEquals("packet #2 should be signature",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertEquals("packet #3 should be user id",
                PacketTags.USER_ID, it.next().tag);
        Assert.assertEquals("packet #4 should be signature",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertEquals("packet #5 should be user id",
                PacketTags.USER_ATTRIBUTE, it.next().tag);
        Assert.assertEquals("packet #6 should be signature",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertEquals("packet #7 should be secret subkey",
                PacketTags.SECRET_SUBKEY, it.next().tag);
        Assert.assertEquals("packet #8 should be signature",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertEquals("packet #9 should be secret subkey",
                PacketTags.SECRET_SUBKEY, it.next().tag);
        Assert.assertEquals("packet #10 should be signature",
                PacketTags.SIGNATURE, it.next().tag);

        Assert.assertFalse("exactly 11 packets total", it.hasNext());

        Assert.assertArrayEquals("created keyring should be constant through canonicalization",
                ring.getEncoded(), ring.canonicalize(log, 0).getEncoded());

    }

    @Test public void testUidSignature() throws Exception {

        UncachedPublicKey masterKey = ring.getPublicKey();
        final WrappedSignature sig = masterKey.getSignaturesForRawId(Strings.toUTF8ByteArray("twi")).next();

        byte[] raw = sig.getEncoded();
        // destroy the signature
        raw[raw.length - 5] += 1;
        final WrappedSignature brokenSig = WrappedSignature.fromBytes(raw);

        { // bad certificates get stripped
            UncachedKeyRing modified = KeyringTestingHelper.injectPacket(ring, brokenSig.getEncoded(), 3);
            CanonicalizedKeyRing canonicalized = modified.canonicalize(log, 0);

            Assert.assertTrue("canonicalized keyring with invalid extra sig must be same as original one",
                    !KeyringTestingHelper.diffKeyrings(
                        ring.getEncoded(), canonicalized.getEncoded(), onlyA, onlyB));
        }

        // remove user id certificate for one user
        final UncachedKeyRing base = KeyringTestingHelper.removePacket(ring, 2);

        { // user id without certificate should be removed
            CanonicalizedKeyRing modified = base.canonicalize(log, 0);
            Assert.assertTrue("canonicalized keyring must differ", KeyringTestingHelper.diffKeyrings(
                    ring.getEncoded(), modified.getEncoded(), onlyA, onlyB));

            Assert.assertEquals("two packets should be stripped after canonicalization", 2, onlyA.size());
            Assert.assertEquals("no new packets after canonicalization", 0, onlyB.size());

            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyA.get(0).buf)).readPacket();
            Assert.assertTrue("first stripped packet must be user id", p instanceof UserIDPacket);
            Assert.assertEquals("missing user id must be the expected one",
                    "twi", ((UserIDPacket) p).getID());

            Assert.assertArrayEquals("second stripped packet must be signature we removed",
                    sig.getEncoded(), onlyA.get(1).buf);

        }

        { // add error to signature

            UncachedKeyRing modified = KeyringTestingHelper.injectPacket(base, brokenSig.getEncoded(), 3);
            CanonicalizedKeyRing canonicalized = modified.canonicalize(log, 0);

            Assert.assertTrue("canonicalized keyring must differ", KeyringTestingHelper.diffKeyrings(
                    ring.getEncoded(), canonicalized.getEncoded(), onlyA, onlyB));

            Assert.assertEquals("two packets should be missing after canonicalization", 2, onlyA.size());
            Assert.assertEquals("no new packets after canonicalization", 0, onlyB.size());

            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyA.get(0).buf)).readPacket();
            Assert.assertTrue("first stripped packet must be user id", p instanceof UserIDPacket);
            Assert.assertEquals("missing user id must be the expected one",
                    "twi", ((UserIDPacket) p).getID());

            Assert.assertArrayEquals("second stripped packet must be signature we removed",
                    sig.getEncoded(), onlyA.get(1).buf);
        }

    }

    @Test public void testUidDestroy() throws Exception {

        // signature for "twi"
        ring = KeyringTestingHelper.removePacket(ring, 2);
        // signature for "pink"
        ring = KeyringTestingHelper.removePacket(ring, 3);

        // canonicalization should fail, because there are no valid uids left
        CanonicalizedKeyRing canonicalized = ring.canonicalize(log, 0);
        Assert.assertNull("canonicalization of keyring with no valid uids should fail", canonicalized);

    }

    @Test public void testRevocationRedundant() throws Exception {

        PGPSignature revocation = forgeSignature(
                secretKey, PGPSignature.KEY_REVOCATION, subHashedPacketsGen, secretKey.getPublicKey());

        UncachedKeyRing modified = KeyringTestingHelper.injectPacket(ring, revocation.getEncoded(), 1);

        // try to add the same packet again, it should be rejected in all positions
        injectEverywhere(modified, revocation.getEncoded());

        // an older (but different!) revocation should be rejected as well
        subHashedPacketsGen.setSignatureCreationTime(false, new Date(new Date().getTime() -1000*1000));
        revocation = forgeSignature(
                secretKey, PGPSignature.KEY_REVOCATION, subHashedPacketsGen, secretKey.getPublicKey());

        injectEverywhere(modified, revocation.getEncoded());

    }

    @Test public void testUidRedundant() throws Exception {

        // an older uid certificate should be rejected
        subHashedPacketsGen.setSignatureCreationTime(false, new Date(new Date().getTime() -1000*1000));
        PGPSignature revocation = forgeSignature(
                secretKey, PGPSignature.DEFAULT_CERTIFICATION, subHashedPacketsGen, "twi", secretKey.getPublicKey());

        injectEverywhere(ring, revocation.getEncoded());

    }

    @Test public void testUidRevocationOutdated() throws Exception {
        // an older uid revocation cert should be rejected
        subHashedPacketsGen.setSignatureCreationTime(false, new Date(new Date().getTime() -1000*1000));
        PGPSignature revocation = forgeSignature(
                secretKey, PGPSignature.CERTIFICATION_REVOCATION, subHashedPacketsGen, "twi", secretKey.getPublicKey());

        injectEverywhere(ring, revocation.getEncoded());

    }

    @Test public void testUidRevocationRedundant() throws Exception {

        PGPSignature revocation = forgeSignature(
                secretKey, PGPSignature.CERTIFICATION_REVOCATION, subHashedPacketsGen, "twi", secretKey.getPublicKey());

        // add that revocation to the base, and check if the redundant one will be rejected as well
        UncachedKeyRing modified = KeyringTestingHelper.injectPacket(ring, revocation.getEncoded(), 2);

        injectEverywhere(modified, revocation.getEncoded());

        // an older (but different!) uid revocation should be rejected as well
        subHashedPacketsGen.setSignatureCreationTime(false, new Date(new Date().getTime() -1000*1000));
        revocation = forgeSignature(
                secretKey, PGPSignature.CERTIFICATION_REVOCATION, subHashedPacketsGen, "twi", secretKey.getPublicKey());

        injectEverywhere(modified, revocation.getEncoded());

    }

    @Test public void testDuplicateUid() throws Exception {

        // get subkey packets
        Iterator<RawPacket> it = KeyringTestingHelper.parseKeyring(ring.getEncoded());
        RawPacket uidPacket = KeyringTestingHelper.getNth(it, 3);
        RawPacket uidSig = it.next();

        // inject at a second position
        UncachedKeyRing modified = ring;
        modified = KeyringTestingHelper.injectPacket(modified, uidPacket.buf, 5);
        modified = KeyringTestingHelper.injectPacket(modified, uidSig.buf, 6);

        // canonicalize, and check if we lose the bad signature
        OperationLog log = new OperationLog();
        CanonicalizedKeyRing canonicalized = modified.canonicalize(log, 0);
        Assert.assertNotNull("canonicalization with duplicate user id should succeed", canonicalized);
        Assert.assertTrue("log should contain uid_dup event", log.containsType(LogType.MSG_KC_UID_DUP));
        /* TODO actually test ths, and fix behavior
        Assert.assertTrue("duplicate user id packets should be gone after canonicalization",
                KeyringTestingHelper.diffKeyrings(modified.getEncoded(), canonicalized.getEncoded(),
                        onlyA, onlyB)
        );
        Assert.assertEquals("canonicalized keyring should have lost the two duplicate packets",
                2, onlyA.size());
        Assert.assertTrue("canonicalized keyring should still contain the user id",
                canonicalized.getUnorderedUserIds().contains(new UserIDPacket(uidPacket.buf).getID()));
        */

    }

    @Test public void testSignatureBroken() throws Exception {

        injectEverytype(secretKey, ring, subHashedPacketsGen, true);

    }

    @Test public void testForeignSignature() throws Exception {

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
        parcel.mAddUserIds.add("trix");
        PgpKeyOperation op = new PgpKeyOperation(null);

        OperationResult.OperationLog log = new OperationResult.OperationLog();
        UncachedKeyRing foreign = op.createSecretKeyRing(parcel).getRing();

        Assert.assertNotNull("initial test key creation must succeed", foreign);
        PGPSecretKey foreignSecretKey =
                new PGPSecretKeyRing(foreign.getEncoded(), new JcaKeyFingerprintCalculator())
                .getSecretKey();

        injectEverytype(foreignSecretKey, ring, subHashedPacketsGen);

    }

    @Test public void testSignatureFuture() throws Exception {

        // generate future timestamp (we allow up to one day future timestamps)
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, 2);
        subHashedPacketsGen.setSignatureCreationTime(false, cal.getTime());

        injectEverytype(secretKey, ring, subHashedPacketsGen);

    }

    @Test public void testSignatureLocal() throws Exception {

        // make key local only
        subHashedPacketsGen.setExportable(false, false);

        injectEverytype(secretKey, ring, subHashedPacketsGen);

    }

    @Test public void testSubkeyDestroy() throws Exception {

        // signature for second key (first subkey)
        UncachedKeyRing modified = KeyringTestingHelper.removePacket(ring, 8);

        // canonicalization should fail, because there are no valid uids left
        CanonicalizedKeyRing canonicalized = modified.canonicalize(log, 0);
        Assert.assertTrue("keyring with missing subkey binding sig should differ from intact one after canonicalization",
                KeyringTestingHelper.diffKeyrings(ring.getEncoded(), canonicalized.getEncoded(),
                        onlyA, onlyB)
        );

        Assert.assertEquals("canonicalized keyring should have two extra packets", 2, onlyA.size());
        Assert.assertEquals("canonicalized keyring should have no extra packets", 0, onlyB.size());

        Assert.assertEquals("first missing packet should be the subkey",
                PacketTags.SECRET_SUBKEY, onlyA.get(0).tag);
        Assert.assertEquals("second missing packet should be subkey's signature",
                PacketTags.SIGNATURE, onlyA.get(1).tag);
        Assert.assertEquals("second missing packet should be next to subkey",
                onlyA.get(0).position + 1, onlyA.get(1).position);

    }

    @Test public void testSubkeyBindingNoPKB() throws Exception {

        UncachedPublicKey pKey = KeyringTestingHelper.getNth(ring.getPublicKeys(), 1);
        PGPSignature sig;

        subHashedPacketsGen.setKeyFlags(false, KeyFlags.SIGN_DATA);

        {
            // forge a (newer) signature, which has the sign flag but no primary key binding sig
            PGPSignatureSubpacketGenerator unhashedSubs = new PGPSignatureSubpacketGenerator();

            // just add any random signature, because why not
            unhashedSubs.setEmbeddedSignature(false, forgeSignature(
                            secretKey, PGPSignature.POSITIVE_CERTIFICATION, subHashedPacketsGen,
                            secretKey.getPublicKey()
                    )
            );

            sig = forgeSignature(
                    secretKey, PGPSignature.SUBKEY_BINDING, subHashedPacketsGen, unhashedSubs,
                    secretKey.getPublicKey(), pKey.getPublicKey());

            // inject in the right position
            UncachedKeyRing modified = KeyringTestingHelper.injectPacket(ring, sig.getEncoded(), 8);

            // canonicalize, and check if we lose the bad signature
            CanonicalizedKeyRing canonicalized = modified.canonicalize(log, 0);
            Assert.assertFalse("subkey binding signature should be gone after canonicalization",
                    KeyringTestingHelper.diffKeyrings(ring.getEncoded(), canonicalized.getEncoded(),
                            onlyA, onlyB)
            );
        }

        { // now try one with a /bad/ primary key binding signature

            PGPSignatureSubpacketGenerator unhashedSubs = new PGPSignatureSubpacketGenerator();
            // this one is signed by the primary key itself, not the subkey - but it IS primary binding
            unhashedSubs.setEmbeddedSignature(false, forgeSignature(
                            secretKey, PGPSignature.PRIMARYKEY_BINDING, subHashedPacketsGen,
                            secretKey.getPublicKey(), pKey.getPublicKey()
                    )
            );

            sig = forgeSignature(
                    secretKey, PGPSignature.SUBKEY_BINDING, subHashedPacketsGen, unhashedSubs,
                    secretKey.getPublicKey(), pKey.getPublicKey());

            // inject in the right position
            UncachedKeyRing modified = KeyringTestingHelper.injectPacket(ring, sig.getEncoded(), 8);

            // canonicalize, and check if we lose the bad signature
            CanonicalizedKeyRing canonicalized = modified.canonicalize(log, 0);
            Assert.assertFalse("subkey binding signature should be gone after canonicalization",
                    KeyringTestingHelper.diffKeyrings(ring.getEncoded(), canonicalized.getEncoded(),
                            onlyA, onlyB)
            );
        }

    }

    @Test public void testSubkeyBindingRedundant() throws Exception {

        UncachedPublicKey pKey = KeyringTestingHelper.getNth(ring.getPublicKeys(), 2);

        subHashedPacketsGen.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS);
        PGPSignature sig2 = forgeSignature(
                secretKey, PGPSignature.SUBKEY_BINDING, subHashedPacketsGen,
                secretKey.getPublicKey(), pKey.getPublicKey());

        subHashedPacketsGen.setSignatureCreationTime(false, new Date(new Date().getTime() -1000*1000));
        PGPSignature sig1 = forgeSignature(
                secretKey, PGPSignature.SUBKEY_REVOCATION, subHashedPacketsGen,
                secretKey.getPublicKey(), pKey.getPublicKey());

        subHashedPacketsGen = new PGPSignatureSubpacketGenerator();
        subHashedPacketsGen.setSignatureCreationTime(false, new Date(new Date().getTime() -100*1000));
        PGPSignature sig3 = forgeSignature(
                secretKey, PGPSignature.SUBKEY_BINDING, subHashedPacketsGen,
                secretKey.getPublicKey(), pKey.getPublicKey());

        UncachedKeyRing modified = KeyringTestingHelper.injectPacket(ring, sig1.getEncoded(), 10);
        modified = KeyringTestingHelper.injectPacket(modified, sig2.getEncoded(), 11);
        modified = KeyringTestingHelper.injectPacket(modified, sig1.getEncoded(), 12);
        modified = KeyringTestingHelper.injectPacket(modified, sig3.getEncoded(), 13);

        // canonicalize, and check if we lose the bad signature
        CanonicalizedKeyRing canonicalized = modified.canonicalize(log, 0);
        Assert.assertTrue("subkey binding signature should be gone after canonicalization",
                KeyringTestingHelper.diffKeyrings(modified.getEncoded(), canonicalized.getEncoded(),
                        onlyA, onlyB)
        );

        Assert.assertEquals("canonicalized keyring should have lost two packets", 3, onlyA.size());
        Assert.assertEquals("canonicalized keyring should have no extra packets", 0, onlyB.size());

        Assert.assertEquals("first missing packet should be the subkey",
                PacketTags.SIGNATURE, onlyA.get(0).tag);
        Assert.assertEquals("second missing packet should be a signature",
                PacketTags.SIGNATURE, onlyA.get(1).tag);
        Assert.assertEquals("second missing packet should be a signature",
                PacketTags.SIGNATURE, onlyA.get(2).tag);

    }

    @Test
    public void testDuplicateSubkey() throws Exception {

        { // duplicate subkey

            // get subkey packets
            Iterator<RawPacket> it = KeyringTestingHelper.parseKeyring(ring.getEncoded());
            RawPacket subKey = KeyringTestingHelper.getNth(it, 7);
            RawPacket subSig = it.next();

            // inject at a second position
            UncachedKeyRing modified = ring;
            modified = KeyringTestingHelper.injectPacket(modified, subKey.buf, 9);
            modified = KeyringTestingHelper.injectPacket(modified, subSig.buf, 10);

            // canonicalize, and check if we lose the bad signature
            OperationLog log = new OperationLog();
            CanonicalizedKeyRing canonicalized = modified.canonicalize(log, 0);
            Assert.assertNull("canonicalization with duplicate subkey should fail", canonicalized);
            Assert.assertTrue("log should contain dup_key event", log.containsType(LogType.MSG_KC_ERROR_DUP_KEY));
        }

        { // duplicate subkey, which is the same as the master key

            // We actually encountered one of these in the wild:
            // https://www.sparkasse-holstein.de/firmenkunden/electronic_banking/secure-e-mail/pdf/Spk_Holstein_PGP_Domain-Zertifikat.asc

            CanonicalizedSecretKeyRing canonicalized = (CanonicalizedSecretKeyRing) ring.canonicalize(log, 0);

            CanonicalizedSecretKey masterSecretKey = canonicalized.getSecretKey();
            masterSecretKey.unlock(new Passphrase());
            PGPPublicKey masterPublicKey = masterSecretKey.getPublicKey();
            CryptoInputParcel cryptoInput = new CryptoInputParcel(new Date());
            PGPSignature cert = PgpKeyOperation.generateSubkeyBindingSignature(
                    PgpKeyOperation.getSignatureGenerator(masterSecretKey.getSecretKey(), cryptoInput),
                    cryptoInput.getSignatureTime(),
                    masterPublicKey, masterSecretKey.getPrivateKey(),
                    PgpKeyOperation.getSignatureGenerator(masterSecretKey.getSecretKey(), null),
                    masterSecretKey.getPrivateKey(), masterPublicKey, masterSecretKey.getKeyUsage(), 0);
            PGPPublicKey subPubKey = PGPPublicKey.addSubkeyBindingCertification(masterPublicKey, cert);

            PGPSecretKey sKey;
            {
                // Build key encrypter and decrypter based on passphrase
                PGPDigestCalculator encryptorHashCalc = new JcaPGPDigestCalculatorProviderBuilder()
                        .build().get(HashAlgorithmTags.SHA256);
                PBESecretKeyEncryptor keyEncryptor = new JcePBESecretKeyEncryptorBuilder(
                        SymmetricKeyAlgorithmTags.AES_256, encryptorHashCalc, 10)
                        .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME).build("".toCharArray());

                // NOTE: only SHA1 is supported for key checksum calculations.
                PGPDigestCalculator sha1Calc = new JcaPGPDigestCalculatorProviderBuilder()
                        .build().get(HashAlgorithmTags.SHA1);
                sKey = new PGPSecretKey(masterSecretKey.getPrivateKey(), subPubKey, sha1Calc, false, keyEncryptor);
            }

            UncachedKeyRing modified = KeyringTestingHelper.injectPacket(ring, sKey.getEncoded(), 7);

            // canonicalize, and check if we lose the bad signature
            OperationLog log = new OperationLog();
            CanonicalizedKeyRing result = modified.canonicalize(log, 0);
            Assert.assertNull("canonicalization with duplicate subkey (from master) should fail", result);
            Assert.assertTrue("log should contain dup_key event", log.containsType(LogType.MSG_KC_ERROR_DUP_KEY));
        }

    }

    private static final int[] sigtypes_direct = new int[] {
        PGPSignature.KEY_REVOCATION,
        PGPSignature.DIRECT_KEY,
    };
    private static final int[] sigtypes_uid = new int[] {
        PGPSignature.DEFAULT_CERTIFICATION,
        PGPSignature.NO_CERTIFICATION,
        PGPSignature.CASUAL_CERTIFICATION,
        PGPSignature.POSITIVE_CERTIFICATION,
        PGPSignature.CERTIFICATION_REVOCATION,
    };
    private static final int[] sigtypes_subkey = new int[] {
        PGPSignature.SUBKEY_BINDING,
        PGPSignature.PRIMARYKEY_BINDING,
        PGPSignature.SUBKEY_REVOCATION,
    };

    private static void injectEverytype(PGPSecretKey secretKey,
                                        UncachedKeyRing ring,
                                        PGPSignatureSubpacketGenerator subHashedPacketsGen)
            throws Exception {
        injectEverytype(secretKey, ring, subHashedPacketsGen, false);
    }

    private static void injectEverytype(PGPSecretKey secretKey,
                                        UncachedKeyRing ring,
                                        PGPSignatureSubpacketGenerator subHashedPacketsGen,
                                        boolean breakSig)
            throws Exception {

        for (int sigtype : sigtypes_direct) {
            PGPSignature sig = forgeSignature(
                    secretKey, sigtype, subHashedPacketsGen, secretKey.getPublicKey());
            byte[] encoded = sig.getEncoded();
            if (breakSig) {
                encoded[encoded.length-10] += 1;
            }
            injectEverywhere(ring, encoded);
        }

        for (int sigtype : sigtypes_uid) {
            PGPSignature sig = forgeSignature(
                    secretKey, sigtype, subHashedPacketsGen, "twi", secretKey.getPublicKey());

            byte[] encoded = sig.getEncoded();
            if (breakSig) {
                encoded[encoded.length-10] += 1;
            }
            injectEverywhere(ring, encoded);
        }

        for (int sigtype : sigtypes_subkey) {
            PGPSignature sig = forgeSignature(
                    secretKey, sigtype, subHashedPacketsGen,
                    secretKey.getPublicKey(), secretKey.getPublicKey());

            byte[] encoded = sig.getEncoded();
            if (breakSig) {
                encoded[encoded.length-10] += 1;
            }
            injectEverywhere(ring, encoded);
        }

    }

    private static void injectEverywhere(UncachedKeyRing ring, byte[] packet) throws Exception {

        OperationResult.OperationLog log = new OperationResult.OperationLog();

        byte[] encodedRing = ring.getEncoded();

        for(int i = 0; i < totalPackets; i++) {

            byte[] brokenEncoded = KeyringTestingHelper.injectPacket(encodedRing, packet, i);

            try {

                UncachedKeyRing brokenRing = UncachedKeyRing.decodeFromData(brokenEncoded);

                CanonicalizedKeyRing canonicalized = brokenRing.canonicalize(log, 0);
                if (canonicalized == null) {
                    System.out.println("ok, canonicalization failed.");
                    continue;
                }

                Assert.assertArrayEquals("injected bad signature must be gone after canonicalization",
                        ring.getEncoded(), canonicalized.getEncoded());

            } catch (Exception e) {
                System.out.println("ok, rejected with: " + e.getMessage());
            }
        }

    }

    private static PGPSignature forgeSignature(PGPSecretKey key, int type,
                                               PGPSignatureSubpacketGenerator subpackets,
                                               PGPPublicKey publicKey)
            throws Exception {

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                Constants.BOUNCY_CASTLE_PROVIDER_NAME).build("".toCharArray());
        PGPPrivateKey privateKey = key.extractPrivateKey(keyDecryptor);

        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                publicKey.getAlgorithm(), PGPUtil.SHA1)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        sGen.setHashedSubpackets(subpackets.generate());
        sGen.init(type, privateKey);
        return sGen.generateCertification(publicKey);

    }

    private static PGPSignature forgeSignature(PGPSecretKey key, int type,
                                               PGPSignatureSubpacketGenerator subpackets,
                                               String userId, PGPPublicKey publicKey)
            throws Exception {

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                Constants.BOUNCY_CASTLE_PROVIDER_NAME).build("".toCharArray());
        PGPPrivateKey privateKey = key.extractPrivateKey(keyDecryptor);

        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                publicKey.getAlgorithm(), PGPUtil.SHA1)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        sGen.setHashedSubpackets(subpackets.generate());
        sGen.init(type, privateKey);
        return sGen.generateCertification(userId, publicKey);

    }

    private static PGPSignature forgeSignature(PGPSecretKey key, int type,
                                               PGPSignatureSubpacketGenerator subpackets,
                                               PGPPublicKey publicKey, PGPPublicKey signedKey)
            throws Exception {

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                Constants.BOUNCY_CASTLE_PROVIDER_NAME).build("".toCharArray());
        PGPPrivateKey privateKey = key.extractPrivateKey(keyDecryptor);

        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                publicKey.getAlgorithm(), PGPUtil.SHA1)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        sGen.setHashedSubpackets(subpackets.generate());
        sGen.init(type, privateKey);
        return sGen.generateCertification(publicKey, signedKey);

    }

    private static PGPSignature forgeSignature(PGPSecretKey key, int type,
                                               PGPSignatureSubpacketGenerator hashedSubs,
                                               PGPSignatureSubpacketGenerator unhashedSubs,
                                               PGPPublicKey publicKey, PGPPublicKey signedKey)
            throws Exception {

        PBESecretKeyDecryptor keyDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider(
                Constants.BOUNCY_CASTLE_PROVIDER_NAME).build("".toCharArray());
        PGPPrivateKey privateKey = key.extractPrivateKey(keyDecryptor);

        PGPContentSignerBuilder signerBuilder = new JcaPGPContentSignerBuilder(
                publicKey.getAlgorithm(), PGPUtil.SHA1)
                .setProvider(Constants.BOUNCY_CASTLE_PROVIDER_NAME);

        PGPSignatureGenerator sGen = new PGPSignatureGenerator(signerBuilder);
        sGen.setHashedSubpackets(hashedSubs.generate());
        sGen.setUnhashedSubpackets(unhashedSubs.generate());
        sGen.init(type, privateKey);
        return sGen.generateCertification(publicKey, signedKey);

    }

}

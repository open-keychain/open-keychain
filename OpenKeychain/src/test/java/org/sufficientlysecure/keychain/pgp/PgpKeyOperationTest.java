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


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import junit.framework.AssertionFailedError;
import org.bouncycastle.bcpg.BCPGInputStream;
import org.bouncycastle.bcpg.Packet;
import org.bouncycastle.bcpg.PacketTags;
import org.bouncycastle.bcpg.S2K;
import org.bouncycastle.bcpg.SecretKeyPacket;
import org.bouncycastle.bcpg.SecretSubkeyPacket;
import org.bouncycastle.bcpg.SignaturePacket;
import org.bouncycastle.bcpg.UserAttributePacket;
import org.bouncycastle.bcpg.UserAttributeSubpacket;
import org.bouncycastle.bcpg.UserIDPacket;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPSignature;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.operations.results.OperationResult.LogType;
import org.sufficientlysecure.keychain.operations.results.OperationResult.OperationLog;
import org.sufficientlysecure.keychain.operations.results.PgpEditKeyResult;
import org.sufficientlysecure.keychain.service.ChangeUnlockParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.service.input.RequiredInputParcel.RequiredInputType;
import org.sufficientlysecure.keychain.support.KeyringBuilder;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper.RawPacket;
import org.sufficientlysecure.keychain.support.TestDataUtil;
import org.sufficientlysecure.keychain.util.Passphrase;
import org.sufficientlysecure.keychain.util.ProgressScaler;
import org.sufficientlysecure.keychain.util.TestingUtils;

import static org.bouncycastle.bcpg.sig.KeyFlags.CERTIFY_OTHER;
import static org.bouncycastle.bcpg.sig.KeyFlags.SIGN_DATA;
import static org.sufficientlysecure.keychain.operations.results.OperationResult.LogType.MSG_MF_ERROR_FINGERPRINT;
import static org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm.ECDSA;
import static org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm.RSA;
import static org.sufficientlysecure.keychain.service.SaveKeyringParcel.Curve.NIST_P256;
import static org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd.createSubkeyAdd;
import static org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange.createFlagsOrExpiryChange;
import static org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange.createRecertifyChange;
import static org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange.createStripChange;
import static org.sufficientlysecure.keychain.service.SaveKeyringParcel.buildChangeKeyringParcel;


@RunWith(KeychainTestRunner.class)
public class PgpKeyOperationTest {

    static UncachedKeyRing staticRing;
    final static Passphrase passphrase = TestingUtils.genPassphrase();

    UncachedKeyRing ring;
    PgpKeyOperation op;
    SaveKeyringParcel.Builder builder;
    ArrayList<RawPacket> onlyA = new ArrayList<>();
    ArrayList<RawPacket> onlyB = new ArrayList<>();

    static CryptoInputParcel cryptoInput;

    @BeforeClass
    public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);
        ShadowLog.stream = System.out;

        SaveKeyringParcel.Builder builder = SaveKeyringParcel.buildNewKeyringParcel();
        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.ECDH, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.ENCRYPT_COMMS, 0L));

        builder.addUserId("twi");
        builder.addUserId("pink");

        {
            int type = 42;
            byte[] data = new byte[] { 0, 1, 2, 3, 4 };
            WrappedUserAttribute uat = WrappedUserAttribute.fromSubpacket(type, data);
            builder.addUserAttribute(uat);
        }

        builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(passphrase));
        PgpKeyOperation op = new PgpKeyOperation(null);

        PgpEditKeyResult result = op.createSecretKeyRing(builder.build());
        Assert.assertTrue("initial test key creation must succeed", result.success());
        Assert.assertNotNull("initial test key creation must succeed", result.getRing());

        staticRing = result.getRing();
        staticRing = staticRing.canonicalize(new OperationLog(), 0).getUncachedKeyRing();

        // we sleep here for a second, to make sure all new certificates have different timestamps
        Thread.sleep(1000);

        cryptoInput = CryptoInputParcel.createCryptoInputParcel(new Date(), passphrase);

    }

    @Before
    public void setUp() throws Exception {
        // show Log.x messages in system.out
        ShadowLog.stream = System.out;
        ring = staticRing;

        // setting up some parameters just to reduce code duplication
        op = new PgpKeyOperation(new ProgressScaler(null, 0, 100, 100));

        resetBuilder();
    }

    private void resetBuilder() {
        builder = SaveKeyringParcel.buildChangeKeyringParcel(ring.getMasterKeyId(), ring.getFingerprint());
    }

    @Test
    public void createSecretKeyRingTests() {

        {
            resetBuilder();
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.RSA, new Random().nextInt(256)+255, null, KeyFlags.CERTIFY_OTHER, 0L));
            builder.addUserId("shy");
            builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(passphrase));

            assertFailure("creating ring with < 2048 bit keysize should fail", builder.build(),
                    LogType.MSG_CR_ERROR_KEYSIZE_2048);
        }

        {
            resetBuilder();
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.ELGAMAL, 2048, null, KeyFlags.CERTIFY_OTHER, 0L));
            builder.addUserId("shy");
            builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(passphrase));

            assertFailure("creating ring with ElGamal master key should fail", builder.build(),
                    LogType.MSG_CR_ERROR_FLAGS_ELGAMAL);
        }

        {
            resetBuilder();
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, null));
            builder.addUserId("lotus");
            builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(passphrase));

            assertFailure("creating master key with null expiry should fail", builder.build(),
                    LogType.MSG_CR_ERROR_NULL_EXPIRY);
        }

        {
            resetBuilder();
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.SIGN_DATA, 0L));
            builder.addUserId("shy");
            builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(passphrase));

            assertFailure("creating ring with non-certifying master key should fail", builder.build(),
                    LogType.MSG_CR_ERROR_NO_CERTIFY);
        }

        {
            resetBuilder();
            builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                    Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER, 0L));
            builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(passphrase));

            assertFailure("creating ring without user ids should fail", builder.build(),
                    LogType.MSG_CR_ERROR_NO_USER_ID);
        }

        {
            resetBuilder();
            builder.addUserId("shy");
            builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(passphrase));

            assertFailure("creating ring with no master key should fail", builder.build(),
                    LogType.MSG_CR_ERROR_NO_MASTER);
        }

    }

    @Test
    // this is a special case since the flags are in user id certificates rather than
    // subkey binding certificates
    public void testMasterFlags() throws Exception {
        SaveKeyringParcel.Builder builder = SaveKeyringParcel.buildNewKeyringParcel();
        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA, 0L));
        builder.addUserId("luna");
        ring = assertCreateSuccess("creating ring with master key flags must succeed", builder.build());

        Assert.assertEquals("the keyring should contain only the master key",
                1, KeyringTestingHelper.itToList(ring.getPublicKeys()).size());
        Assert.assertEquals("first (master) key must have both flags",
                KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA, (long) ring.getPublicKey().getKeyUsage());

    }

    @Test
    public void testCreatedKey() throws Exception {

        // an empty modification should change nothing. this also ensures the keyring
        // is constant through canonicalization.
        // applyModificationWithChecks(parcel, ring, onlyA, onlyB);

        Assert.assertNotNull("key creation failed", ring);

        Assert.assertNull("primary user id must be empty",
                ring.getPublicKey().getPrimaryUserId());

        Assert.assertEquals("number of user ids must be two",
                2, ring.getPublicKey().getUnorderedUserIds().size());

        ArrayList<WrappedUserAttribute> attributes =
            ring.getPublicKey().getUnorderedUserAttributes();
        Assert.assertEquals("number of user attributes must be one",
                1, attributes.size());
        Assert.assertEquals("user attribute must be correct type",
                42, attributes.get(0).getType());
        Assert.assertEquals("user attribute must have one subpacket",
                1, attributes.get(0).getSubpackets().length);
        Assert.assertArrayEquals("user attribute must have correct data",
                new byte[] { 0, 1, 2, 3, 4 }, attributes.get(0).getSubpackets()[0]);

        List<UncachedPublicKey> subkeys = KeyringTestingHelper.itToList(ring.getPublicKeys());
        Assert.assertEquals("number of subkeys must be three", 3, subkeys.size());

        Assert.assertTrue("key ring should have been created in the last 360 seconds",
                ring.getPublicKey().getCreationTime().after(new Date(new Date().getTime()-1000*360)));

        Assert.assertNull("key ring should not expire",
                ring.getPublicKey().getUnsafeExpiryTimeForTesting());

        Assert.assertEquals("first (master) key can certify",
                KeyFlags.CERTIFY_OTHER, (long) subkeys.get(0).getKeyUsage());

        Assert.assertEquals("second key can sign",
                KeyFlags.SIGN_DATA, (long) subkeys.get(1).getKeyUsage());
        ArrayList<WrappedSignature> sigs = subkeys.get(1).getSignatures().next().getEmbeddedSignatures();
        Assert.assertEquals("signing key signature should have one embedded signature",
                1, sigs.size());
        Assert.assertEquals("embedded signature should be of primary key binding type",
                PGPSignature.PRIMARYKEY_BINDING, sigs.get(0).getSignatureType());
        Assert.assertEquals("primary key binding signature issuer should be signing subkey",
                subkeys.get(1).getKeyId(), sigs.get(0).getKeyId());

        Assert.assertEquals("third key can encrypt",
                KeyFlags.ENCRYPT_COMMS, (long) subkeys.get(2).getKeyUsage());

    }

    @Test
    public void testBadKeyModification() throws Exception {

        {
            SaveKeyringParcel.Builder builder = SaveKeyringParcel.buildChangeKeyringParcel(
                    ring.getMasterKeyId() -1, ring.getFingerprint());

            assertModifyFailure("keyring modification with bad master key id should fail",
                    ring, builder.build(), LogType.MSG_MF_ERROR_KEYID);
        }

        {
            byte[] fingerprint = Arrays.copyOf(ring.getFingerprint(), ring.getFingerprint().length);
            fingerprint[5] += 1;

            SaveKeyringParcel.Builder builder = buildChangeKeyringParcel(ring.getMasterKeyId(), fingerprint);

            assertModifyFailure("keyring modification with bad fingerprint should fail",
                    ring, builder.build(), MSG_MF_ERROR_FINGERPRINT);
        }

        {
            SaveKeyringParcel.Builder builder = buildChangeKeyringParcel(ring.getMasterKeyId(), null);
            assertModifyFailure("keyring modification with null fingerprint should fail",
                    ring, builder.build(), MSG_MF_ERROR_FINGERPRINT);
        }

        {
            Passphrase badphrase = new Passphrase();
            if (badphrase.equals(passphrase)) {
                badphrase = new Passphrase("a");
            }
            builder.addUserId("allure");

            assertModifyFailure("keyring modification with bad passphrase should fail",
                    ring, builder.build(), CryptoInputParcel.createCryptoInputParcel(badphrase), LogType.MSG_MF_UNLOCK_ERROR);
        }

        {
            resetBuilder();
            assertModifyFailure("no-op should fail",
                    ring, builder.build(), cryptoInput, LogType.MSG_MF_ERROR_NOOP);
        }

    }

    @Test
    public void testSubkeyAdd() throws Exception {

        long expiry = new Date().getTime() / 1000 + 159;
        int flags = KeyFlags.SIGN_DATA;
        builder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.ECDSA, 0, SaveKeyringParcel.Curve.NIST_P256, flags, expiry));

        UncachedKeyRing modified = applyModificationWithChecks(builder.build(), ring, onlyA, onlyB);

        Assert.assertEquals("no extra packets in original", 0, onlyA.size());
        Assert.assertEquals("exactly two extra packets in modified", 2, onlyB.size());

        Packet p;

        p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
        Assert.assertTrue("first new packet must be secret subkey", p instanceof SecretSubkeyPacket);

        p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(1).buf)).readPacket();
        Assert.assertTrue("second new packet must be signature", p instanceof SignaturePacket);
        Assert.assertEquals("signature type must be subkey binding certificate",
                PGPSignature.SUBKEY_BINDING, ((SignaturePacket) p).getSignatureType());
        Assert.assertEquals("signature must have been created by master key",
                ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

        // get new key from ring. it should be the last one (add a check to make sure?)
        UncachedPublicKey newKey = null;
        {
            Iterator<UncachedPublicKey> it = modified.getPublicKeys();
            while (it.hasNext()) {
                newKey = it.next();
            }
        }

        Assert.assertNotNull("new key is not null", newKey);
        Assert.assertNotNull("added key must have an expiry date",
                newKey.getUnsafeExpiryTimeForTesting());
        Assert.assertEquals("added key must have expected expiry date",
                expiry, newKey.getUnsafeExpiryTimeForTesting().getTime()/1000);
        Assert.assertEquals("added key must have expected flags",
                flags, (long) newKey.getKeyUsage());

        { // bad keysize should fail
            resetBuilder();
            builder.addSubkeyAdd(createSubkeyAdd(
                    RSA, new Random().nextInt(512), null, SIGN_DATA, 0L));
            assertModifyFailure("creating a subkey with keysize < 2048 should fail", ring, builder.build(),
                    LogType.MSG_CR_ERROR_KEYSIZE_2048);
        }

        { // null expiry should fail
            resetBuilder();
            builder.addSubkeyAdd(createSubkeyAdd(
                    ECDSA, 0, NIST_P256, SIGN_DATA, null));
            assertModifyFailure("creating master key with null expiry should fail", ring, builder.build(),
                    LogType.MSG_MF_ERROR_NULL_EXPIRY);
        }

        { // a past expiry should fail
            resetBuilder();
            builder.addSubkeyAdd(createSubkeyAdd(
                    ECDSA, 0, NIST_P256, SIGN_DATA,
                    new Date().getTime() / 1000 - 10));
            assertModifyFailure("creating subkey with past expiry date should fail", ring, builder.build(),
                    LogType.MSG_MF_ERROR_PAST_EXPIRY);
        }

    }

    @Test
    public void testSubkeyModify() throws Exception {

        long expiry = new Date().getTime()/1000 + 1024;
        long keyId = KeyringTestingHelper.getSubkeyId(ring, 1);

        UncachedKeyRing modified = ring;
        {
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, null, expiry));
            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            Assert.assertEquals("one extra packet in original", 1, onlyA.size());
            Assert.assertEquals("one extra packet in modified", 1, onlyB.size());

            Assert.assertEquals("old packet must be signature",
                    PacketTags.SIGNATURE, onlyA.get(0).tag);

            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertTrue("first new packet must be signature", p instanceof SignaturePacket);
            Assert.assertEquals("signature type must be subkey binding certificate",
                    PGPSignature.SUBKEY_BINDING, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            Assert.assertNotNull("modified key must have an expiry date",
                    modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting());
            Assert.assertEquals("modified key must have expected expiry date",
                    expiry, modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting().getTime()/1000);
            Assert.assertEquals("modified key must have same flags as before",
                    ring.getPublicKey(keyId).getKeyUsage(), modified.getPublicKey(keyId).getKeyUsage());
        }

        { // change expiry
            expiry += 60*60*24;

            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, null, expiry));
            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            Assert.assertNotNull("modified key must have an expiry date",
                    modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting());
            Assert.assertEquals("modified key must have expected expiry date",
                    expiry, modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting().getTime()/1000);
            Assert.assertEquals("modified key must have same flags as before",
                    ring.getPublicKey(keyId).getKeyUsage(), modified.getPublicKey(keyId).getKeyUsage());
        }

        {
            int flags = KeyFlags.SIGN_DATA | KeyFlags.ENCRYPT_COMMS;
            resetBuilder();
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, flags, null));
            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            Assert.assertEquals("old packet must be signature",
                    PacketTags.SIGNATURE, onlyA.get(0).tag);

            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertTrue("first new packet must be signature", p instanceof SignaturePacket);
            Assert.assertEquals("signature type must be subkey binding certificate",
                    PGPSignature.SUBKEY_BINDING, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            Assert.assertEquals("modified key must have expected flags",
                    flags, (long) modified.getPublicKey(keyId).getKeyUsage());
            Assert.assertNotNull("key must retain its expiry",
                    modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting());
            Assert.assertEquals("key expiry must be unchanged",
                    expiry, modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting().getTime()/1000);
        }

        { // expiry of 0 should be "no expiry"
            resetBuilder();
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, null, 0L));
            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            Assert.assertEquals("old packet must be signature",
                    PacketTags.SIGNATURE, onlyA.get(0).tag);

            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertTrue("first new packet must be signature", p instanceof SignaturePacket);
            Assert.assertEquals("signature type must be subkey binding certificate",
                    PGPSignature.SUBKEY_BINDING, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            Assert.assertNull("key must not expire anymore", modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting());
        }

        { // a past expiry should fail
            resetBuilder();
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, null, new Date().getTime() / 1000 - 10));

            assertModifyFailure("setting subkey expiry to a past date should fail", ring, builder.build(),
                    LogType.MSG_MF_ERROR_PAST_EXPIRY);
        }

        { // modifying nonexistent subkey should fail
            resetBuilder();
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(123, null, null));

            assertModifyFailure("modifying non-existent subkey should fail", ring, builder.build(),
                    LogType.MSG_MF_ERROR_SUBKEY_MISSING);
        }

    }

    @Test
    public void testMasterModify() throws Exception {

        long expiry = new Date().getTime()/1000 + 1024;
        long keyId = ring.getMasterKeyId();

        UncachedKeyRing modified = ring;

        // to make this check less trivial, we add a user id, change the primary one and revoke one
        builder.addUserId("aloe");
        builder.setChangePrimaryUserId("aloe");
        builder.addRevokeUserId("pink");
        modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

        {
            resetBuilder();
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, null, expiry));
            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            // this implies that only the two non-revoked signatures were changed!
            Assert.assertEquals("two extra packets in original", 2, onlyA.size());
            Assert.assertEquals("two extra packets in modified", 2, onlyB.size());

            Assert.assertEquals("first original packet must be a signature",
                    PacketTags.SIGNATURE, onlyA.get(0).tag);
            Assert.assertEquals("second original packet must be a signature",
                    PacketTags.SIGNATURE, onlyA.get(1).tag);
            Assert.assertEquals("first new packet must be signature",
                    PacketTags.SIGNATURE, onlyB.get(0).tag);
            Assert.assertEquals("first new packet must be signature",
                    PacketTags.SIGNATURE, onlyB.get(1).tag);

            Assert.assertNotNull("modified key must have an expiry date",
                    modified.getPublicKey().getUnsafeExpiryTimeForTesting());
            Assert.assertEquals("modified key must have expected expiry date",
                    expiry, modified.getPublicKey().getUnsafeExpiryTimeForTesting().getTime() / 1000);
            Assert.assertEquals("modified key must have same flags as before",
                    ring.getPublicKey().getKeyUsage(), modified.getPublicKey().getKeyUsage());
        }

        { // change expiry
            expiry += 60*60*24;

            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, null, expiry));
            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            Assert.assertNotNull("modified key must have an expiry date",
                    modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting());
            Assert.assertEquals("modified key must have expected expiry date",
                    expiry, modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting().getTime() / 1000);
            Assert.assertEquals("modified key must have same flags as before",
                    ring.getPublicKey(keyId).getKeyUsage(), modified.getPublicKey(keyId).getKeyUsage());

            Date date = modified.canonicalize(new OperationLog(), 0).getPublicKey().getExpiryTime();
            Assert.assertNotNull("modified key must have an expiry date", date);
            Assert.assertEquals("modified key must have expected expiry date",
                    expiry, date.getTime() / 1000);

        }

        {
            int flags = KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA;
            resetBuilder();
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, flags, null));
            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            Assert.assertEquals("modified key must have expected flags",
                    flags, (long) modified.getPublicKey(keyId).getKeyUsage());
            Assert.assertNotNull("key must retain its expiry",
                    modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting());
            Assert.assertEquals("key expiry must be unchanged",
                    expiry, modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting().getTime()/1000);
        }

        { // expiry of 0 should be "no expiry"

            // even if there is a non-expiring user id while all others are revoked, it doesn't count!
            // for this purpose we revoke one while they still have expiry times
            resetBuilder();
            builder.addRevokeUserId("aloe");
            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            resetBuilder();
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, null, 0L));
            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            // for this check, it is relevant that we DON'T use the unsafe one!
            Assert.assertNull("key must not expire anymore",
                    modified.canonicalize(new OperationLog(), 0).getPublicKey().getExpiryTime());
            // make sure the unsafe one behaves incorrectly as expected
            Assert.assertNotNull("unsafe expiry must yield wrong result from revoked user id",
                    modified.getPublicKey(keyId).getUnsafeExpiryTimeForTesting());
        }

        { // if we revoke everything, nothing is left to properly sign...
            resetBuilder();
            builder.addRevokeUserId("twi");
            builder.addRevokeUserId("pink");
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, CERTIFY_OTHER, null));

            assertModifyFailure("master key modification with all user ids revoked should fail", ring, builder.build(),
                    LogType.MSG_MF_ERROR_MASTER_NONE);
        }

        { // any flag not including CERTIFY_OTHER should fail
            resetBuilder();
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, SIGN_DATA, null));

            assertModifyFailure("setting master key flags without certify should fail", ring, builder.build(),
                    LogType.MSG_MF_ERROR_NO_CERTIFY);
        }

        { // a past expiry should fail
            resetBuilder();
            builder.addOrReplaceSubkeyChange(createFlagsOrExpiryChange(keyId, null, new Date().getTime() / 1000 - 10));

            assertModifyFailure("setting subkey expiry to a past date should fail", ring, builder.build(),
                    LogType.MSG_MF_ERROR_PAST_EXPIRY);
        }

    }

    @Test
    public void testMasterRevoke() throws Exception {

        resetBuilder();
        builder.addRevokeSubkey(ring.getMasterKeyId());

        UncachedKeyRing modified = applyModificationWithChecks(builder.build(), ring, onlyA, onlyB);

        Assert.assertEquals("no extra packets in original", 0, onlyA.size());
        Assert.assertEquals("exactly one extra packet in modified", 1, onlyB.size());

        Packet p;

        p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
        Assert.assertTrue("first new packet must be secret subkey", p instanceof SignaturePacket);
        Assert.assertEquals("signature type must be subkey binding certificate",
                PGPSignature.KEY_REVOCATION, ((SignaturePacket) p).getSignatureType());
        Assert.assertEquals("signature must have been created by master key",
                ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

        Assert.assertTrue("subkey must actually be revoked",
                modified.getPublicKey().isMaybeRevoked());

    }

    @Test
    public void testSubkeyRevoke() throws Exception {

        long keyId = KeyringTestingHelper.getSubkeyId(ring, 1);
        int flags = ring.getPublicKey(keyId).getKeyUsage();

        UncachedKeyRing modified;

        {

            resetBuilder();
            builder.addRevokeSubkey(123L);

            CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), 0);
            UncachedKeyRing otherModified = op.modifySecretKeyRing(secretRing, cryptoInput, builder.build()).getRing();

            Assert.assertNull("revoking a nonexistent subkey should fail", otherModified);

        }

        { // revoked second subkey

            resetBuilder();
            builder.addRevokeSubkey(keyId);

            modified = applyModificationWithChecks(builder.build(), ring, onlyA, onlyB,
                    CryptoInputParcel.createCryptoInputParcel(new Date(), passphrase));

            Assert.assertEquals("no extra packets in original", 0, onlyA.size());
            Assert.assertEquals("exactly one extra packet in modified", 1, onlyB.size());

            Packet p;

            p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertTrue("first new packet must be secret subkey", p instanceof SignaturePacket);
            Assert.assertEquals("signature type must be subkey binding certificate",
                    PGPSignature.SUBKEY_REVOCATION, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            Assert.assertTrue("subkey must actually be revoked",
                    modified.getPublicKey(keyId).isMaybeRevoked());
        }

        { // re-add second subkey

            resetBuilder();
            // re-certify the revoked subkey
            builder.addOrReplaceSubkeyChange(createRecertifyChange(keyId, true));

            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            Assert.assertEquals("exactly two outdated packets in original", 2, onlyA.size());
            Assert.assertEquals("exactly one extra packet in modified", 1, onlyB.size());

            Packet p;

            p = new BCPGInputStream(new ByteArrayInputStream(onlyA.get(0).buf)).readPacket();
            Assert.assertTrue("first outdated packet must be signature", p instanceof SignaturePacket);
            Assert.assertEquals("first outdated signature type must be subkey binding certification",
                    PGPSignature.SUBKEY_BINDING, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("first outdated signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            p = new BCPGInputStream(new ByteArrayInputStream(onlyA.get(1).buf)).readPacket();
            Assert.assertTrue("second outdated packet must be signature", p instanceof SignaturePacket);
            Assert.assertEquals("second outdated signature type must be subkey revocation",
                    PGPSignature.SUBKEY_REVOCATION, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("second outdated signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertTrue("new packet must be signature ", p instanceof SignaturePacket);
            Assert.assertEquals("new signature type must be subkey binding certification",
                    PGPSignature.SUBKEY_BINDING, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            Assert.assertFalse("subkey must no longer be revoked",
                    modified.getPublicKey(keyId).isMaybeRevoked());
            Assert.assertEquals("subkey must have the same usage flags as before",
                    flags, (long) modified.getPublicKey(keyId).getKeyUsage());

        }
    }

    @Test
    public void testSubkeyStrip() throws Exception {

        long keyId = KeyringTestingHelper.getSubkeyId(ring, 1);
        builder.addOrReplaceSubkeyChange(createStripChange(keyId));
        applyModificationWithChecks(builder.build(), ring, onlyA, onlyB);

        Assert.assertEquals("one extra packet in original", 1, onlyA.size());
        Assert.assertEquals("one extra packet in modified", 1, onlyB.size());

        Assert.assertEquals("old packet must be secret subkey",
                PacketTags.SECRET_SUBKEY, onlyA.get(0).tag);
        Assert.assertEquals("new packet must be secret subkey",
                PacketTags.SECRET_SUBKEY, onlyB.get(0).tag);

        Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
        Assert.assertEquals("new packet should have GNU_DUMMY S2K type",
                S2K.GNU_DUMMY_S2K, ((SecretSubkeyPacket) p).getS2K().getType());
        Assert.assertEquals("new packet should have GNU_DUMMY protection mode 0x1",
                0x1, ((SecretSubkeyPacket) p).getS2K().getProtectionMode());
        Assert.assertEquals("new packet secret key data should have length zero",
                0, ((SecretSubkeyPacket) p).getSecretKeyData().length);
        Assert.assertNull("new packet should have no iv data", ((SecretSubkeyPacket) p).getIV());

    }

    @Test
    public void testMasterStrip() throws Exception {

        long keyId = ring.getMasterKeyId();
        builder.addOrReplaceSubkeyChange(createStripChange(keyId));
        applyModificationWithChecks(builder.build(), ring, onlyA, onlyB);

        Assert.assertEquals("one extra packet in original", 1, onlyA.size());
        Assert.assertEquals("one extra packet in modified", 1, onlyB.size());

        Assert.assertEquals("old packet must be secret key",
                PacketTags.SECRET_KEY, onlyA.get(0).tag);
        Assert.assertEquals("new packet must be secret key",
                PacketTags.SECRET_KEY, onlyB.get(0).tag);

        Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
        Assert.assertEquals("new packet should have GNU_DUMMY S2K type",
                S2K.GNU_DUMMY_S2K, ((SecretKeyPacket) p).getS2K().getType());
        Assert.assertEquals("new packet should have GNU_DUMMY protection mode 0x1",
                0x1, ((SecretKeyPacket) p).getS2K().getProtectionMode());
        Assert.assertEquals("new packet secret key data should have length zero",
                0, ((SecretKeyPacket) p).getSecretKeyData().length);
        Assert.assertNull("new packet should have no iv data", ((SecretKeyPacket) p).getIV());
    }

    @Test
    public void testRestrictedStrip() throws Exception {

        long keyId = KeyringTestingHelper.getSubkeyId(ring, 1);
        UncachedKeyRing modified;

        { // we should be able to change the stripped status of subkeys without passphrase
            resetBuilder();
            builder.addOrReplaceSubkeyChange(createStripChange(keyId));
            modified = applyModificationWithChecks(builder.build(), ring, onlyA, onlyB,
                    CryptoInputParcel.createCryptoInputParcel());
            Assert.assertEquals("one extra packet in modified", 1, onlyB.size());
            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertEquals("new packet should have GNU_DUMMY S2K type",
                    S2K.GNU_DUMMY_S2K, ((SecretKeyPacket) p).getS2K().getType());
            Assert.assertEquals("new packet should have GNU_DUMMY protection mode stripped",
                    S2K.GNU_PROTECTION_MODE_NO_PRIVATE_KEY, ((SecretKeyPacket) p).getS2K().getProtectionMode());
        }

        { // trying to edit a subkey with signing capability should fail
            resetBuilder();
            builder.addOrReplaceSubkeyChange(createRecertifyChange(keyId, true));

            assertModifyFailure("subkey modification for signing-enabled but stripped subkey should fail",
                    modified, builder.build(), LogType.MSG_MF_ERROR_SUB_STRIPPED);
        }

    }

    @Test
    public void testKeyToSecurityToken() throws Exception {

        // Special keyring for security token tests with 2048 bit RSA as a subkey
        SaveKeyringParcel.Builder keyBuilder = SaveKeyringParcel.buildNewKeyringParcel();
        keyBuilder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.DSA, 2048, null, KeyFlags.CERTIFY_OTHER, 0L));
        keyBuilder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.RSA, 2048, null, KeyFlags.SIGN_DATA, 0L));
        keyBuilder.addSubkeyAdd(SubkeyAdd.createSubkeyAdd(
                Algorithm.RSA, 3072, null, KeyFlags.ENCRYPT_COMMS, 0L));

        keyBuilder.addUserId("yubikey");

        keyBuilder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(passphrase));
        PgpKeyOperation opSecurityToken = new PgpKeyOperation(null);

        PgpEditKeyResult resultSecurityToken = opSecurityToken.createSecretKeyRing(keyBuilder.build());
        Assert.assertTrue("initial test key creation must succeed", resultSecurityToken.success());
        Assert.assertNotNull("initial test key creation must succeed", resultSecurityToken.getRing());

        UncachedKeyRing ringSecurityToken = resultSecurityToken.getRing();

        UncachedKeyRing modified;

        { // moveKeyToSecurityToken should fail with BAD_NFC_ALGO when presented with the DSA-1024 key
            long keyId = KeyringTestingHelper.getSubkeyId(ringSecurityToken, 0);
            SaveKeyringParcel.Builder securityTokenBuilder = SaveKeyringParcel.buildChangeKeyringParcel(
                    ringSecurityToken.getMasterKeyId(), ringSecurityToken.getFingerprint());
            securityTokenBuilder.addOrReplaceSubkeyChange(SubkeyChange.createMoveToSecurityTokenChange(keyId));

            assertModifyFailure("moveKeyToSecurityToken operation should fail on invalid key algorithm", ringSecurityToken,
                    securityTokenBuilder.build(), cryptoInput, LogType.MSG_MF_ERROR_BAD_SECURITY_TOKEN_ALGO);
        }

        long keyId = KeyringTestingHelper.getSubkeyId(ringSecurityToken, 1);

        { // moveKeyToSecurityToken should return a pending SECURITY_TOKEN_MOVE_KEY_TO_CARD result when presented with the RSA-2048
          // key, and then make key divert-to-card when it gets a serial in the cryptoInputParcel.
            SaveKeyringParcel.Builder securityTokenBuilder = SaveKeyringParcel.buildChangeKeyringParcel(
                    ringSecurityToken.getMasterKeyId(), ringSecurityToken.getFingerprint());
            securityTokenBuilder.addOrReplaceSubkeyChange(SubkeyChange.createMoveToSecurityTokenChange(keyId));

            CanonicalizedSecretKeyRing secretRing =
                    new CanonicalizedSecretKeyRing(ringSecurityToken.getEncoded(), 0);
            PgpKeyOperation op = new PgpKeyOperation(null);
            PgpEditKeyResult result = op.modifySecretKeyRing(secretRing, cryptoInput, securityTokenBuilder.build());
            Assert.assertTrue("moveKeyToSecurityToken operation should be pending", result.isPending());
            Assert.assertEquals("required input should be RequiredInputType.SECURITY_TOKEN_MOVE_KEY_TO_CARD",
                    result.getRequiredInputParcel().mType, RequiredInputType.SECURITY_TOKEN_MOVE_KEY_TO_CARD);

            // Create a cryptoInputParcel that matches what the SecurityTokenOperationActivity would return.
            byte[] keyIdBytes = new byte[8];
            ByteBuffer buf = ByteBuffer.wrap(keyIdBytes);
            buf.putLong(keyId).rewind();
            byte[] serial = new byte[] {
                    0x6a, 0x6f, 0x6c, 0x6f, 0x73, 0x77, 0x61, 0x67,
                    0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            };
            CryptoInputParcel inputParcel = CryptoInputParcel.createCryptoInputParcel();
            inputParcel = inputParcel.withCryptoData(keyIdBytes, serial);

            modified = applyModificationWithChecks(securityTokenBuilder.build(), ringSecurityToken, onlyA, onlyB, inputParcel);
            Assert.assertEquals("one extra packet in modified", 1, onlyB.size());
            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertEquals("new packet should have GNU_DUMMY S2K type",
                    S2K.GNU_DUMMY_S2K, ((SecretKeyPacket) p).getS2K().getType());
            Assert.assertEquals("new packet should have GNU_DUMMY protection mode divert-to-card",
                    S2K.GNU_PROTECTION_MODE_DIVERT_TO_CARD, ((SecretKeyPacket) p).getS2K().getProtectionMode());
            Assert.assertArrayEquals("new packet should have correct serial number as iv",
                    serial, ((SecretKeyPacket) p).getIV());
        }

        { // editing a signing subkey requires a primary key binding sig -> pendinginput
            SaveKeyringParcel.Builder securityTokenBuilder = SaveKeyringParcel.buildChangeKeyringParcel(
                    ringSecurityToken.getMasterKeyId(), ringSecurityToken.getFingerprint());
            securityTokenBuilder.addOrReplaceSubkeyChange(SubkeyChange.createRecertifyChange(keyId, true));

            CanonicalizedSecretKeyRing secretRing =
                    new CanonicalizedSecretKeyRing(modified.getEncoded(), 0);
            PgpKeyOperation op = new PgpKeyOperation(null);
            PgpEditKeyResult result = op.modifySecretKeyRing(secretRing, cryptoInput, securityTokenBuilder.build());
            Assert.assertTrue("moveKeyToSecurityToken operation should be pending", result.isPending());
            Assert.assertEquals("required input should be RequiredInputType.SECURITY_TOKEN_SIGN",
                    RequiredInputType.SECURITY_TOKEN_SIGN, result.getRequiredInputParcel().mType);
        }

    }

    @Test
    public void testUserIdRevoke() throws Exception {

        UncachedKeyRing modified;
        String uid = ring.getPublicKey().getUnorderedUserIds().get(1);

        { // revoke second user id
            builder.addRevokeUserId(uid);
            modified = applyModificationWithChecks(builder.build(), ring, onlyA, onlyB);

            Assert.assertEquals("no extra packets in original", 0, onlyA.size());
            Assert.assertEquals("exactly one extra packet in modified", 1, onlyB.size());

            Packet p;

            p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertTrue("first new packet must be secret subkey", p instanceof SignaturePacket);
            Assert.assertEquals("signature type must be subkey binding certificate",
                    PGPSignature.CERTIFICATION_REVOCATION, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

        }

        { // re-add second user id

            resetBuilder();
            builder.setChangePrimaryUserId(uid);

            assertModifyFailure("setting primary user id to a revoked user id should fail", modified, builder.build(),
                    LogType.MSG_MF_ERROR_REVOKED_PRIMARY);

        }

        { // re-add second user id

            resetBuilder();
            builder.addUserId(uid);

            applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            Assert.assertEquals("exactly two outdated packets in original", 2, onlyA.size());
            Assert.assertEquals("exactly one extra packet in modified", 1, onlyB.size());

            Packet p;

            p = new BCPGInputStream(new ByteArrayInputStream(onlyA.get(0).buf)).readPacket();
            Assert.assertTrue("first outdated packet must be signature", p instanceof SignaturePacket);
            Assert.assertEquals("first outdated signature type must be positive certification",
                    PGPSignature.POSITIVE_CERTIFICATION, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("first outdated signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            p = new BCPGInputStream(new ByteArrayInputStream(onlyA.get(1).buf)).readPacket();
            Assert.assertTrue("second outdated packet must be signature", p instanceof SignaturePacket);
            Assert.assertEquals("second outdated signature type must be certificate revocation",
                    PGPSignature.CERTIFICATION_REVOCATION, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("second outdated signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertTrue("new packet must be signature ", p instanceof SignaturePacket);
            Assert.assertEquals("new signature type must be positive certification",
                    PGPSignature.POSITIVE_CERTIFICATION, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());
        }

        { // revocation of non-existent user id should fail
            resetBuilder();
            builder.addRevokeUserId("nonexistent");

            assertModifyFailure("revocation of nonexistent user id should fail", modified, builder.build(),
                    LogType.MSG_MF_ERROR_NOEXIST_REVOKE);
        }

    }

    @Test
    public void testUserIdAdd() throws Exception {

        {
            builder.addUserId("");
            assertModifyFailure("adding an empty user id should fail", ring, builder.build(),
                    LogType.MSG_MF_UID_ERROR_EMPTY);
        }

        resetBuilder();
        builder.addUserId("rainbow");

        UncachedKeyRing modified = applyModificationWithChecks(builder.build(), ring, onlyA, onlyB);

        Assert.assertTrue("keyring must contain added user id",
                modified.getPublicKey().getUnorderedUserIds().contains("rainbow"));

        Assert.assertEquals("no extra packets in original", 0, onlyA.size());
        Assert.assertEquals("exactly two extra packets in modified", 2, onlyB.size());

        Assert.assertTrue("keyring must contain added user id",
                modified.getPublicKey().getUnorderedUserIds().contains("rainbow"));

        Packet p;

        p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
        Assert.assertTrue("first new packet must be user id", p instanceof UserIDPacket);
        Assert.assertEquals("user id packet must match added user id",
                "rainbow", ((UserIDPacket) p).getID());

        p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(1).buf)).readPacket();
        Assert.assertTrue("second new packet must be signature", p instanceof SignaturePacket);
        Assert.assertEquals("signature type must be positive certification",
                PGPSignature.POSITIVE_CERTIFICATION, ((SignaturePacket) p).getSignatureType());

    }

    @Test
    public void testUserAttributeAdd() throws Exception {

        {
            builder.addUserAttribute(WrappedUserAttribute.fromData(new byte[0]));
            assertModifyFailure("adding an empty user attribute should fail", ring, builder.build(),
                    LogType.MSG_MF_UAT_ERROR_EMPTY);
        }

        resetBuilder();

        Random r = new Random();
        int type = r.nextInt(110)+2; // any type except image attribute, to avoid interpretation of these
        byte[] data = new byte[r.nextInt(2000)];
        new Random().nextBytes(data);

        WrappedUserAttribute uat = WrappedUserAttribute.fromSubpacket(type, data);
        builder.addUserAttribute(uat);

        UncachedKeyRing modified = applyModificationWithChecks(builder.build(), ring, onlyA, onlyB);

        Assert.assertEquals("no extra packets in original", 0, onlyA.size());
        Assert.assertEquals("exactly two extra packets in modified", 2, onlyB.size());

        Assert.assertTrue("keyring must contain added user attribute",
                modified.getPublicKey().getUnorderedUserAttributes().contains(uat));

        Packet p;

        p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
        Assert.assertTrue("first new packet must be user attribute", p instanceof UserAttributePacket);
        {
            UserAttributeSubpacket[] subpackets = ((UserAttributePacket) p).getSubpackets();
            Assert.assertEquals("user attribute packet must contain one subpacket",
                    1, subpackets.length);
            Assert.assertEquals("user attribute subpacket type must be as specified above",
                    type, subpackets[0].getType());
            Assert.assertArrayEquals("user attribute subpacket data must be as specified above",
                    data, subpackets[0].getData());
        }

        p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(1).buf)).readPacket();
        Assert.assertTrue("second new packet must be signature", p instanceof SignaturePacket);
        Assert.assertEquals("signature type must be positive certification",
                PGPSignature.POSITIVE_CERTIFICATION, ((SignaturePacket) p).getSignatureType());

        Thread.sleep(1000);

        // applying the same modification AGAIN should not add more certifications but drop those
        // as duplicates
        applyModificationWithChecks(builder.build(), modified, onlyA, onlyB,
                CryptoInputParcel.createCryptoInputParcel(new Date(), passphrase), true, false);

        Assert.assertEquals("duplicate modification: one extra packet in original", 1, onlyA.size());
        Assert.assertEquals("duplicate modification: one extra packet in modified", 1, onlyB.size());

        p = new BCPGInputStream(new ByteArrayInputStream(onlyA.get(0).buf)).readPacket();
        Assert.assertTrue("lost packet must be signature", p instanceof SignaturePacket);
        p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
        Assert.assertTrue("new packet must be signature", p instanceof SignaturePacket);

    }


    @Test
    public void testUserIdPrimary() throws Exception {

        UncachedKeyRing modified = ring;
        String uid = ring.getPublicKey().getUnorderedUserIds().get(1);

        { // first part, add new user id which is also primary
            builder.addUserId("jack");
            builder.setChangePrimaryUserId("jack");

            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            Assert.assertEquals("primary user id must be the one added",
                    "jack", modified.getPublicKey().getPrimaryUserId());
        }

        { // second part, change primary to a different one
            resetBuilder();
            builder.setChangePrimaryUserId(uid);

            modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB);

            Assert.assertEquals("old keyring must have two outdated certificates", 2, onlyA.size());
            Assert.assertEquals("new keyring must have two new packets", 2, onlyB.size());

            Assert.assertEquals("primary user id must be the one changed to",
                    "pink", modified.getPublicKey().getPrimaryUserId());
        }

        { // third part, change primary to a non-existent one
            resetBuilder();
            //noinspection SpellCheckingInspection
            builder.setChangePrimaryUserId("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
            assertModifyFailure("changing primary user id to a non-existent one should fail",
                    ring, builder.build(), LogType.MSG_MF_ERROR_NOEXIST_PRIMARY);
        }

        // check for revoked primary user id already done in revoke test

    }

    @Test
    public void testPassphraseChange() throws Exception {

        // change passphrase to empty
        builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(new Passphrase()));
        // note that canonicalization here necessarily strips the empty notation packet
        UncachedKeyRing modified = applyModificationWithChecks(builder.build(), ring, onlyA, onlyB, cryptoInput);

        Assert.assertEquals("exactly three packets should have been modified (the secret keys)",
                3, onlyB.size());

        // remember secret key packet with no passphrase for later
        RawPacket sKeyNoPassphrase = onlyB.get(1);
        Assert.assertEquals("extracted packet should be a secret subkey",
                PacketTags.SECRET_SUBKEY, sKeyNoPassphrase.tag);

        // modify keyring, change to non-empty passphrase
        Passphrase otherPassphrase = TestingUtils.genPassphrase(true);
        CryptoInputParcel otherCryptoInput = CryptoInputParcel.createCryptoInputParcel(otherPassphrase);
        builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(otherPassphrase));
        modified = applyModificationWithChecks(builder.build(), modified, onlyA, onlyB,
                CryptoInputParcel.createCryptoInputParcel(new Date(), new Passphrase()));

        Assert.assertEquals("exactly three packets should have been modified (the secret keys)",
                3, onlyB.size());

        { // quick check to make sure no two secret keys have the same IV
            HashSet<ByteBuffer> ivs = new HashSet<>();
            for (int i = 0; i < 3; i++) {
                SecretKeyPacket p = (SecretKeyPacket) new BCPGInputStream(
                        new ByteArrayInputStream(onlyB.get(i).buf)).readPacket();
                ByteBuffer iv = ByteBuffer.wrap(p.getIV());
                Assert.assertFalse(
                        "no two secret keys should have the same s2k iv (slightly non-deterministic!)",
                        ivs.contains(iv)
                );
                ivs.add(iv);
            }
        }

        RawPacket sKeyWithPassphrase = onlyB.get(1);
        Assert.assertEquals("extracted packet should be a secret subkey",
                PacketTags.SECRET_SUBKEY, sKeyNoPassphrase.tag);

        Passphrase otherPassphrase2 = TestingUtils.genPassphrase(true);
        builder.setNewUnlock(ChangeUnlockParcel.createUnLockParcelForNewKey(otherPassphrase2));
        {
            // if we replace a secret key with one without passphrase
            modified = KeyringTestingHelper.removePacket(modified, sKeyNoPassphrase.position);
            modified = KeyringTestingHelper.injectPacket(modified, sKeyNoPassphrase.buf, sKeyNoPassphrase.position);

            // we should still be able to modify it (and change its passphrase) without errors
            PgpKeyOperation op = new PgpKeyOperation(null);
            CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(modified.getEncoded(), 0);
            PgpEditKeyResult result = op.modifySecretKeyRing(secretRing, otherCryptoInput, builder.build());
            Assert.assertTrue("key modification must succeed", result.success());
            Assert.assertFalse("log must not contain a warning",
                    result.getLog().containsWarnings());
            Assert.assertTrue("log must contain an empty passphrase retry notice",
                result.getLog().containsType(LogType.MSG_MF_PASSPHRASE_EMPTY_RETRY));
            modified = result.getRing();
        }

        {
            // if we add one subkey with a different passphrase, that should produce a warning but also work
            modified = KeyringTestingHelper.removePacket(modified, sKeyWithPassphrase.position);
            modified = KeyringTestingHelper.injectPacket(modified, sKeyWithPassphrase.buf, sKeyWithPassphrase.position);

            PgpKeyOperation op = new PgpKeyOperation(null);
            CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(modified.getEncoded(), 0);
            PgpEditKeyResult result = op.modifySecretKeyRing(secretRing,
                    CryptoInputParcel.createCryptoInputParcel(otherPassphrase2), builder.build());
            Assert.assertTrue("key modification must succeed", result.success());
            Assert.assertTrue("log must contain a failed passphrase change warning",
                    result.getLog().containsType(LogType.MSG_MF_PASSPHRASE_FAIL));
        }

    }

    @Test
    public void testRestricted() throws Exception {

        CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), 0);

        builder.addUserId("discord");
        PgpKeyOperation op = new PgpKeyOperation(null);
        PgpEditKeyResult result = op.modifySecretKeyRing(secretRing,
                CryptoInputParcel.createCryptoInputParcel(new Date()), builder.build());
        Assert.assertFalse("non-restricted operations should fail without passphrase", result.success());
    }

    public static UncachedKeyRing applyModificationWithChecks(SaveKeyringParcel parcel,
                                                               UncachedKeyRing ring,
                                                               ArrayList<RawPacket> onlyA,
                                                               ArrayList<RawPacket> onlyB) {
        return applyModificationWithChecks(parcel, ring, onlyA, onlyB, cryptoInput, true, true);
    }

    public static UncachedKeyRing applyModificationWithChecks(SaveKeyringParcel parcel,
                                                               UncachedKeyRing ring,
                                                               ArrayList<RawPacket> onlyA,
                                                               ArrayList<RawPacket> onlyB,
                                                               CryptoInputParcel cryptoInput) {
        return applyModificationWithChecks(parcel, ring, onlyA, onlyB, cryptoInput, true, true);
    }

    // applies a parcel modification while running some integrity checks
    public static UncachedKeyRing applyModificationWithChecks(SaveKeyringParcel parcel,
                                                               UncachedKeyRing ring,
                                                               ArrayList<RawPacket> onlyA,
                                                               ArrayList<RawPacket> onlyB,
                                                               CryptoInputParcel cryptoInput,
                                                               boolean canonicalize,
                                                               boolean constantCanonicalize) {

        try {

            Assert.assertTrue("modified keyring must be secret", ring.isSecret());
            CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), 0);

            PgpKeyOperation op = new PgpKeyOperation(null);
            PgpEditKeyResult result = op.modifySecretKeyRing(secretRing, cryptoInput, parcel);
            Assert.assertTrue("key modification must succeed", result.success());
            UncachedKeyRing rawModified = result.getRing();
            Assert.assertNotNull("key modification must not return null", rawModified);

            if (!canonicalize) {
                Assert.assertTrue("keyring must differ from original", KeyringTestingHelper.diffKeyrings(
                        ring.getEncoded(), rawModified.getEncoded(), onlyA, onlyB));
                return rawModified;
            }

            CanonicalizedKeyRing modified = rawModified.canonicalize(new OperationLog(), 0);
            if (constantCanonicalize) {
                Assert.assertTrue("key must be constant through canonicalization",
                        !KeyringTestingHelper.diffKeyrings(
                                modified.getEncoded(), rawModified.getEncoded(), onlyA, onlyB)
                );
            }
            Assert.assertTrue("keyring must differ from original", KeyringTestingHelper.diffKeyrings(
                    ring.getEncoded(), modified.getEncoded(), onlyA, onlyB));

            return modified.getUncachedKeyRing();

        } catch (IOException e) {
            throw new AssertionFailedError("error during encoding!");
        }
    }

    @Test
    public void testVerifySuccess() throws Exception {

        UncachedKeyRing expectedKeyRing = KeyringBuilder.correctRing();
        UncachedKeyRing inputKeyRing = KeyringBuilder.ringWithExtraIncorrectSignature();

        CanonicalizedKeyRing canonicalized = inputKeyRing.canonicalize(new OperationLog(), 0);
        Assert.assertNotNull("canonicalization must succeed", canonicalized);

        ArrayList onlyA = new ArrayList<>();
        ArrayList onlyB = new ArrayList<>();
        //noinspection unchecked
        Assert.assertTrue("keyrings differ", !KeyringTestingHelper.diffKeyrings(
                expectedKeyRing.getEncoded(), expectedKeyRing.getEncoded(), onlyA, onlyB));

    }

    /**
     * Just testing my own test code. Should really be using a library for this.
     */
    @Test
    public void testConcat() throws Exception {
        byte[] actual = TestDataUtil.concatAll(new byte[]{1}, new byte[]{2, -2}, new byte[]{5}, new byte[]{3});
        byte[] expected = new byte[]{1,2,-2,5,3};
        Assert.assertEquals(java.util.Arrays.toString(expected), java.util.Arrays.toString(actual));
    }

    private void assertFailure(String reason, SaveKeyringParcel parcel, LogType expected) {

        PgpEditKeyResult result = op.createSecretKeyRing(parcel);

        Assert.assertFalse(reason, result.success());
        Assert.assertNull(reason, result.getRing());
        Assert.assertTrue(reason + "(with correct error)",
                result.getLog().containsType(expected));

    }

    private void assertModifyFailure(String reason, UncachedKeyRing ring,
                                     SaveKeyringParcel parcel, CryptoInputParcel cryptoInput, LogType expected)
            throws Exception {

        CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), 0);
        PgpEditKeyResult result = op.modifySecretKeyRing(secretRing, cryptoInput, parcel);

        Assert.assertFalse(reason, result.success());
        Assert.assertNull(reason, result.getRing());
        Assert.assertTrue(reason + "(with correct error)",
                result.getLog().containsType(expected));

    }

    private void assertModifyFailure(String reason, UncachedKeyRing ring, SaveKeyringParcel parcel,
                                     LogType expected)
            throws Exception {

        CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), 0);
        PgpEditKeyResult result = op.modifySecretKeyRing(secretRing, cryptoInput, parcel);

        Assert.assertFalse(reason, result.success());
        Assert.assertNull(reason, result.getRing());
        Assert.assertTrue(reason + "(with correct error)",
                result.getLog().containsType(expected));

    }

    private UncachedKeyRing assertCreateSuccess(String reason, SaveKeyringParcel parcel) {

        PgpEditKeyResult result = op.createSecretKeyRing(parcel);

        Assert.assertTrue(reason, result.success());
        Assert.assertNotNull(reason, result.getRing());

        return result.getRing();

    }

}

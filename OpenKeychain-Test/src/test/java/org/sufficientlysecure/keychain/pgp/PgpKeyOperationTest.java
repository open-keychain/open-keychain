package org.sufficientlysecure.keychain.pgp;

import junit.framework.AssertionFailedError;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.shadows.ShadowLog;
import org.spongycastle.bcpg.BCPGInputStream;
import org.spongycastle.bcpg.Packet;
import org.spongycastle.bcpg.PacketTags;
import org.spongycastle.bcpg.SecretSubkeyPacket;
import org.spongycastle.bcpg.SignaturePacket;
import org.spongycastle.bcpg.UserIDPacket;
import org.spongycastle.bcpg.sig.KeyFlags;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.openpgp.PGPSignature;
import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.sufficientlysecure.keychain.service.OperationResultParcel.OperationLog;
import org.sufficientlysecure.keychain.service.OperationResults.EditKeyResult;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyChange;
import org.sufficientlysecure.keychain.support.KeyringBuilder;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper;
import org.sufficientlysecure.keychain.support.KeyringTestingHelper.RawPacket;
import org.sufficientlysecure.keychain.support.TestDataUtil;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(emulateSdk = 18) // Robolectric doesn't yet support 19
public class PgpKeyOperationTest {

    static UncachedKeyRing staticRing;
    static String passphrase;

    UncachedKeyRing ring;
    PgpKeyOperation op;
    SaveKeyringParcel parcel;
    ArrayList<RawPacket> onlyA = new ArrayList<RawPacket>();
    ArrayList<RawPacket> onlyB = new ArrayList<RawPacket>();

    @BeforeClass public static void setUpOnce() throws Exception {
        Security.insertProviderAt(new BouncyCastleProvider(), 1);

        ShadowLog.stream = System.out;

        {
            String chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ123456789!@#$%^&*()-_=";
            Random r = new Random();
            StringBuilder passbuilder = new StringBuilder();
            // 20% chance for an empty passphrase
            for(int i = 0, j = r.nextInt(10) > 2 ? r.nextInt(20) : 0; i < j; i++) {
                passbuilder.append(chars.charAt(r.nextInt(chars.length())));
            }
            passphrase = passbuilder.toString();
            System.out.println("Passphrase is '" + passphrase + "'");
        }

        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                PublicKeyAlgorithmTags.RSA_GENERAL, 1024, KeyFlags.CERTIFY_OTHER, 0L));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                PublicKeyAlgorithmTags.RSA_GENERAL, 1024, KeyFlags.SIGN_DATA, 0L));
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                PublicKeyAlgorithmTags.RSA_GENERAL, 1024, KeyFlags.ENCRYPT_COMMS, 0L));

        parcel.mAddUserIds.add("twi");
        parcel.mAddUserIds.add("pink");
        parcel.mNewPassphrase = passphrase;
        PgpKeyOperation op = new PgpKeyOperation(null);

        EditKeyResult result = op.createSecretKeyRing(parcel);
        Assert.assertTrue("initial test key creation must succeed", result.success());
        Assert.assertNotNull("initial test key creation must succeed", result.getRing());

        staticRing = result.getRing();

        // we sleep here for a second, to make sure all new certificates have different timestamps
        Thread.sleep(1000);
    }

    @Before public void setUp() throws Exception {
        // show Log.x messages in system.out
        ShadowLog.stream = System.out;
        ring = staticRing;

        // setting up some parameters just to reduce code duplication
        op = new PgpKeyOperation(new ProgressScaler(null, 0, 100, 100));

        // set this up, gonna need it more than once
        parcel = new SaveKeyringParcel();
        parcel.mMasterKeyId = ring.getMasterKeyId();
        parcel.mFingerprint = ring.getFingerprint();

    }

    @Test
    public void createSecretKeyRingTests() {

        {
            parcel.reset();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    PublicKeyAlgorithmTags.RSA_GENERAL, new Random().nextInt(256)+255, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mAddUserIds.add("shy");
            parcel.mNewPassphrase = passphrase;

            assertFailure("creating ring with < 512 bytes keysize should fail", parcel);
        }

        {
            parcel.reset();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT, 1024, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mAddUserIds.add("shy");
            parcel.mNewPassphrase = passphrase;

            assertFailure("creating ring with ElGamal master key should fail", parcel);
        }

        {
            parcel.reset();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    12345, 1024, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mAddUserIds.add("shy");
            parcel.mNewPassphrase = passphrase;

            assertFailure("creating ring with bad algorithm choice should fail", parcel);
        }

        {
            parcel.reset();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    PublicKeyAlgorithmTags.RSA_GENERAL, 1024, KeyFlags.SIGN_DATA, 0L));
            parcel.mAddUserIds.add("shy");
            parcel.mNewPassphrase = passphrase;

            assertFailure("creating ring with non-certifying master key should fail", parcel);
        }

        {
            parcel.reset();
            parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                    PublicKeyAlgorithmTags.RSA_GENERAL, 1024, KeyFlags.CERTIFY_OTHER, 0L));
            parcel.mNewPassphrase = passphrase;

            assertFailure("creating ring without user ids should fail", parcel);
        }

        {
            parcel.reset();
            parcel.mAddUserIds.add("shy");
            parcel.mNewPassphrase = passphrase;

            assertFailure("creating ring without subkeys should fail", parcel);
        }

    }

    @Test
    // this is a special case since the flags are in user id certificates rather than
    // subkey binding certificates
    public void testMasterFlags() throws Exception {
        SaveKeyringParcel parcel = new SaveKeyringParcel();
        parcel.mAddSubKeys.add(new SaveKeyringParcel.SubkeyAdd(
                PublicKeyAlgorithmTags.RSA_GENERAL, 1024, KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA, 0L));
        parcel.mAddUserIds.add("luna");
        ring = assertCreateSuccess("creating ring with master key flags must succeed", parcel);

        Assert.assertEquals("the keyring should contain only the master key",
                1, KeyringTestingHelper.itToList(ring.getPublicKeys()).size());
        Assert.assertEquals("first (master) key must have both flags",
                KeyFlags.CERTIFY_OTHER | KeyFlags.SIGN_DATA, ring.getPublicKey().getKeyUsage());

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

        List<UncachedPublicKey> subkeys = KeyringTestingHelper.itToList(ring.getPublicKeys());
        Assert.assertEquals("number of subkeys must be three", 3, subkeys.size());

        Assert.assertTrue("key ring should have been created in the last 120 seconds",
                ring.getPublicKey().getCreationTime().after(new Date(new Date().getTime()-1000*120)));

        Assert.assertNull("key ring should not expire",
                ring.getPublicKey().getExpiryTime());

        Assert.assertEquals("first (master) key can certify",
                KeyFlags.CERTIFY_OTHER, subkeys.get(0).getKeyUsage());

        Assert.assertEquals("second key can sign",
                KeyFlags.SIGN_DATA, subkeys.get(1).getKeyUsage());
        ArrayList<WrappedSignature> sigs = subkeys.get(1).getSignatures().next().getEmbeddedSignatures();
        Assert.assertEquals("signing key signature should have one embedded signature",
                1, sigs.size());
        Assert.assertEquals("embedded signature should be of primary key binding type",
                PGPSignature.PRIMARYKEY_BINDING, sigs.get(0).getSignatureType());
        Assert.assertEquals("primary key binding signature issuer should be signing subkey",
                subkeys.get(1).getKeyId(), sigs.get(0).getKeyId());

        Assert.assertEquals("third key can encrypt",
                KeyFlags.ENCRYPT_COMMS, subkeys.get(2).getKeyUsage());

    }

    @Test
    public void testBadKeyModification() throws Exception {

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            // off by one
            parcel.mMasterKeyId = ring.getMasterKeyId() -1;
            parcel.mFingerprint = ring.getFingerprint();

            assertModifyFailure("keyring modification with bad master key id should fail",
                    ring, parcel);
        }

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            // off by one
            parcel.mMasterKeyId = null;
            parcel.mFingerprint = ring.getFingerprint();

            assertModifyFailure("keyring modification with null master key id should fail",
                    ring, parcel);
        }

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mMasterKeyId = ring.getMasterKeyId();
            parcel.mFingerprint = ring.getFingerprint();
            // some byte, off by one
            parcel.mFingerprint[5] += 1;

            assertModifyFailure("keyring modification with bad fingerprint should fail",
                    ring, parcel);
        }

        {
            SaveKeyringParcel parcel = new SaveKeyringParcel();
            parcel.mMasterKeyId = ring.getMasterKeyId();
            parcel.mFingerprint = null;

            assertModifyFailure("keyring modification with null fingerprint should fail",
                    ring, parcel);
        }

        {
            String badphrase = "";
            if (badphrase.equals(passphrase)) {
                badphrase = "a";
            }

            assertModifyFailure("keyring modification with bad passphrase should fail",
                    ring, parcel, badphrase);
        }

    }

    @Test
    public void testSubkeyAdd() throws Exception {

        long expiry = new Date().getTime() / 1000 + 159;
        int flags = KeyFlags.SIGN_DATA;
        int bits = 1024 + new Random().nextInt(8);
        parcel.mAddSubKeys.add(new SubkeyAdd(PublicKeyAlgorithmTags.RSA_GENERAL, bits, flags, expiry));

        UncachedKeyRing modified = applyModificationWithChecks(parcel, ring, onlyA, onlyB);

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
                newKey.getExpiryTime());
        Assert.assertEquals("added key must have expected expiry date",
                expiry, newKey.getExpiryTime().getTime()/1000);
        Assert.assertEquals("added key must have expected flags",
                flags, newKey.getKeyUsage());
        Assert.assertEquals("added key must have expected bitsize",
                bits, newKey.getBitStrength());

        { // bad keysize should fail
            parcel.reset();
            parcel.mAddSubKeys.add(new SubkeyAdd(
                    PublicKeyAlgorithmTags.RSA_GENERAL, new Random().nextInt(512), KeyFlags.SIGN_DATA, 0L));
            assertModifyFailure("creating a subkey with keysize < 512 should fail", ring, parcel);

        }

        { // a past expiry should fail
            parcel.reset();
            parcel.mAddSubKeys.add(new SubkeyAdd(PublicKeyAlgorithmTags.RSA_GENERAL, 1024, KeyFlags.SIGN_DATA,
                    new Date().getTime()/1000-10));
            assertModifyFailure("creating subkey with past expiry date should fail", ring, parcel);
        }

    }

    @Test
    public void testSubkeyModify() throws Exception {

        long expiry = new Date().getTime()/1000 + 1024;
        long keyId = KeyringTestingHelper.getSubkeyId(ring, 1);

        UncachedKeyRing modified = ring;
        {
            parcel.mChangeSubKeys.add(new SubkeyChange(keyId, null, expiry));
            modified = applyModificationWithChecks(parcel, modified, onlyA, onlyB);

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
                    modified.getPublicKey(keyId).getExpiryTime());
            Assert.assertEquals("modified key must have expected expiry date",
                    expiry, modified.getPublicKey(keyId).getExpiryTime().getTime()/1000);
            Assert.assertEquals("modified key must have same flags as before",
                    ring.getPublicKey(keyId).getKeyUsage(), modified.getPublicKey(keyId).getKeyUsage());
        }

        {
            int flags = KeyFlags.SIGN_DATA | KeyFlags.ENCRYPT_COMMS;
            parcel.reset();
            parcel.mChangeSubKeys.add(new SubkeyChange(keyId, flags, null));
            modified = applyModificationWithChecks(parcel, modified, onlyA, onlyB);

            Assert.assertEquals("old packet must be signature",
                    PacketTags.SIGNATURE, onlyA.get(0).tag);

            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertTrue("first new packet must be signature", p instanceof SignaturePacket);
            Assert.assertEquals("signature type must be subkey binding certificate",
                    PGPSignature.SUBKEY_BINDING, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            Assert.assertEquals("modified key must have expected flags",
                    flags, modified.getPublicKey(keyId).getKeyUsage());
            Assert.assertNotNull("key must retain its expiry",
                    modified.getPublicKey(keyId).getExpiryTime());
            Assert.assertEquals("key expiry must be unchanged",
                    expiry, modified.getPublicKey(keyId).getExpiryTime().getTime()/1000);
        }

        { // expiry of 0 should be "no expiry"
            parcel.reset();
            parcel.mChangeSubKeys.add(new SubkeyChange(keyId, null, 0L));
            modified = applyModificationWithChecks(parcel, modified, onlyA, onlyB);

            Assert.assertEquals("old packet must be signature",
                    PacketTags.SIGNATURE, onlyA.get(0).tag);

            Packet p = new BCPGInputStream(new ByteArrayInputStream(onlyB.get(0).buf)).readPacket();
            Assert.assertTrue("first new packet must be signature", p instanceof SignaturePacket);
            Assert.assertEquals("signature type must be subkey binding certificate",
                    PGPSignature.SUBKEY_BINDING, ((SignaturePacket) p).getSignatureType());
            Assert.assertEquals("signature must have been created by master key",
                    ring.getMasterKeyId(), ((SignaturePacket) p).getKeyID());

            Assert.assertNull("key must not expire anymore", modified.getPublicKey(keyId).getExpiryTime());
        }

        { // a past expiry should fail
            parcel.reset();
            parcel.mChangeSubKeys.add(new SubkeyChange(keyId, null, new Date().getTime()/1000-10));

            CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), false, 0);
            assertModifyFailure("setting subkey expiry to a past date should fail", secretRing, parcel);
        }

        { // modifying nonexistent keyring should fail
            parcel.reset();
            parcel.mChangeSubKeys.add(new SubkeyChange(123, null, null));

            CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), false, 0);
            assertModifyFailure("modifying non-existent subkey should fail", secretRing, parcel);
        }

    }

    @Test
    public void testSubkeyRevoke() throws Exception {

        long keyId = KeyringTestingHelper.getSubkeyId(ring, 1);
        int flags = ring.getPublicKey(keyId).getKeyUsage();

        UncachedKeyRing modified;

        {

            parcel.reset();
            parcel.mRevokeSubKeys.add(123L);

            CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), false, 0);
            UncachedKeyRing otherModified = op.modifySecretKeyRing(secretRing, parcel, passphrase).getRing();

            Assert.assertNull("revoking a nonexistent subkey should fail", otherModified);

        }

        { // revoked second subkey

            parcel.reset();
            parcel.mRevokeSubKeys.add(keyId);

            modified = applyModificationWithChecks(parcel, ring, onlyA, onlyB);

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
                    modified.getPublicKey(keyId).isRevoked());
        }

        { // re-add second subkey

            parcel.reset();
            parcel.mChangeSubKeys.add(new SubkeyChange(keyId, null, null));

            modified = applyModificationWithChecks(parcel, modified, onlyA, onlyB);

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
                    modified.getPublicKey(keyId).isRevoked());
            Assert.assertEquals("subkey must have the same usage flags as before",
                    flags, modified.getPublicKey(keyId).getKeyUsage());

        }
    }

    @Test
    public void testUserIdRevoke() throws Exception {

        UncachedKeyRing modified;
        String uid = ring.getPublicKey().getUnorderedUserIds().get(1);

        { // revoke second user id

            parcel.mRevokeUserIds.add(uid);

            modified = applyModificationWithChecks(parcel, ring, onlyA, onlyB);

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

            parcel.reset();
            parcel.mChangePrimaryUserId = uid;

            assertModifyFailure("setting primary user id to a revoked user id should fail", modified, parcel);

        }

        { // re-add second user id

            parcel.reset();
            parcel.mAddUserIds.add(uid);

            applyModificationWithChecks(parcel, modified, onlyA, onlyB);

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
            parcel.reset();
            parcel.mRevokeUserIds.add("nonexistent");

            assertModifyFailure("revocation of nonexistent user id should fail", modified, parcel);
        }

    }

    @Test
    public void testUserIdAdd() throws Exception {

        {
            parcel.mAddUserIds.add("");
            assertModifyFailure("adding an empty user id should fail", ring, parcel);
        }

        parcel.reset();
        parcel.mAddUserIds.add("rainbow");

        UncachedKeyRing modified = applyModificationWithChecks(parcel, ring, onlyA, onlyB);

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
    public void testUserIdPrimary() throws Exception {

        UncachedKeyRing modified = ring;
        String uid = ring.getPublicKey().getUnorderedUserIds().get(1);

        { // first part, add new user id which is also primary
            parcel.mAddUserIds.add("jack");
            parcel.mChangePrimaryUserId = "jack";

            modified = applyModificationWithChecks(parcel, modified, onlyA, onlyB);

            Assert.assertEquals("primary user id must be the one added",
                    "jack", modified.getPublicKey().getPrimaryUserId());
        }

        { // second part, change primary to a different one
            parcel.reset();
            parcel.mChangePrimaryUserId = uid;

            modified = applyModificationWithChecks(parcel, modified, onlyA, onlyB);

            Assert.assertEquals("old keyring must have two outdated certificates", 2, onlyA.size());
            Assert.assertEquals("new keyring must have two new packets", 2, onlyB.size());

            Assert.assertEquals("primary user id must be the one changed to",
                    "pink", modified.getPublicKey().getPrimaryUserId());
        }

        { // third part, change primary to a non-existent one
            parcel.reset();
            //noinspection SpellCheckingInspection
            parcel.mChangePrimaryUserId = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
            if (parcel.mChangePrimaryUserId.equals(passphrase)) {
                parcel.mChangePrimaryUserId += "A";
            }

            assertModifyFailure("changing primary user id to a non-existent one should fail",
                    ring, parcel);
        }

        // check for revoked primary user id already done in revoke test

    }


    private static UncachedKeyRing applyModificationWithChecks(SaveKeyringParcel parcel,
                                                               UncachedKeyRing ring,
                                                               ArrayList<RawPacket> onlyA,
                                                               ArrayList<RawPacket> onlyB) {
        return applyModificationWithChecks(parcel, ring, onlyA, onlyB, true, true);
    }

    // applies a parcel modification while running some integrity checks
    private static UncachedKeyRing applyModificationWithChecks(SaveKeyringParcel parcel,
                                                               UncachedKeyRing ring,
                                                               ArrayList<RawPacket> onlyA,
                                                               ArrayList<RawPacket> onlyB,
                                                               boolean canonicalize,
                                                               boolean constantCanonicalize) {

        try {

            Assert.assertTrue("modified keyring must be secret", ring.isSecret());
            CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), false, 0);

            PgpKeyOperation op = new PgpKeyOperation(null);
            EditKeyResult result = op.modifySecretKeyRing(secretRing, parcel, passphrase);
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

        ArrayList onlyA = new ArrayList<RawPacket>();
        ArrayList onlyB = new ArrayList<RawPacket>();
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

    private void assertFailure(String reason, SaveKeyringParcel parcel) {

        EditKeyResult result = op.createSecretKeyRing(parcel);

        Assert.assertFalse(reason, result.success());
        Assert.assertNull(reason, result.getRing());

    }

    private void assertModifyFailure(String reason, UncachedKeyRing ring,
                                     SaveKeyringParcel parcel, String passphrase) throws Exception {

        CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), false, 0);
        EditKeyResult result = op.modifySecretKeyRing(secretRing, parcel, passphrase);

        Assert.assertFalse(reason, result.success());
        Assert.assertNull(reason, result.getRing());

    }

    private void assertModifyFailure(String reason, UncachedKeyRing ring, SaveKeyringParcel parcel)
            throws Exception {

        CanonicalizedSecretKeyRing secretRing = new CanonicalizedSecretKeyRing(ring.getEncoded(), false, 0);
        EditKeyResult result = op.modifySecretKeyRing(secretRing, parcel, passphrase);

        Assert.assertFalse(reason, result.success());
        Assert.assertNull(reason, result.getRing());

    }

    private UncachedKeyRing assertCreateSuccess(String reason, SaveKeyringParcel parcel) {

        EditKeyResult result = op.createSecretKeyRing(parcel);

        Assert.assertTrue(reason, result.success());
        Assert.assertNotNull(reason, result.getRing());

        return result.getRing();

    }

}

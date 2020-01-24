package org.sufficientlysecure.keychain.remote;


import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.shadows.ShadowBinder;
import org.robolectric.shadows.ShadowLog;
import org.robolectric.shadows.ShadowPackageManager;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.daos.AutocryptPeerDao;
import org.sufficientlysecure.keychain.daos.KeyWritableRepository;
import org.sufficientlysecure.keychain.model.AutocryptPeer.GossipOrigin;
import org.sufficientlysecure.keychain.operations.CertifyOperation;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.KeyRepositorySaveTest;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract.AutocryptStatus;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;


@SuppressWarnings("WeakerAccess")
@RunWith(KeychainTestRunner.class)
public class AutocryptStatusProviderTest {
    static final String PACKAGE_NAME = "test.package";
    static final byte[] PACKAGE_SIGNATURE = new byte[] { 1, 2, 3 };
    static final String MAIL_ADDRESS_1 = "twi@openkeychain.org";
    static final String USER_ID_1 = "twi <twi@openkeychain.org>";
    static final long KEY_ID_SECRET = 0x5D4DA4423C39122FL;
    static final long KEY_ID_PUBLIC = 0x9A282CE2AB44A382L;
    public static final String AUTOCRYPT_PEER = "tid";
    public static final int PACKAGE_UID = 42;


    KeyWritableRepository databaseInteractor =
            KeyWritableRepository.create(RuntimeEnvironment.application);
    AutocryptStatusProvider autocryptStatusProvider = new AutocryptStatusProvider(RuntimeEnvironment.application);
    ApiPermissionHelper apiPermissionHelper;
    AutocryptPeerDao autocryptPeerDao;


    @Before
    public void setUp() throws Exception {
        ShadowLog.stream = System.out;

        ShadowPackageManager packageManager = shadowOf(RuntimeEnvironment.application.getPackageManager());
        packageManager.setPackagesForUid(PACKAGE_UID, PACKAGE_NAME);
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.signatures = new Signature[] { new Signature(PACKAGE_SIGNATURE) };
        packageInfo.packageName = PACKAGE_NAME;
        packageManager.addPackage(packageInfo);

        ShadowBinder.setCallingUid(PACKAGE_UID);

        apiPermissionHelper = new ApiPermissionHelper(RuntimeEnvironment.application);
        autocryptPeerDao = AutocryptPeerDao.getInstance(RuntimeEnvironment.application);
    }

    @Test
    public void testAutocryptStatus_autocryptPeer_withUnconfirmedKey() throws Exception {
        insertSecretKeyringFrom("/test-keys/testring.sec");
        insertPublicKeyringFrom("/test-keys/testring.pub");

        autocryptPeerDao.insertOrUpdateLastSeen("tid", new Date());
        autocryptPeerDao.updateKey(AUTOCRYPT_PEER, new Date(), KEY_ID_PUBLIC, false);

        Cursor cursor = autocryptStatusProvider.query(
                AutocryptStatus.CONTENT_URI, new String[] {
                        AutocryptStatus.ADDRESS, AutocryptStatus.UID_KEY_STATUS, AutocryptStatus.UID_ADDRESS,
                        AutocryptStatus.AUTOCRYPT_PEER_STATE,
                        AutocryptStatus.AUTOCRYPT_KEY_STATUS, AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID
                },
                null, new String [] { AUTOCRYPT_PEER }, null
            );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("tid", cursor.getString(0));
        assertTrue(cursor.isNull(1));
        assertEquals(null, cursor.getString(2));
        assertEquals(AutocryptStatus.AUTOCRYPT_PEER_AVAILABLE, cursor.getInt(3));
        assertEquals(KeychainExternalContract.KEY_STATUS_UNVERIFIED, cursor.getInt(4));
        assertEquals(KEY_ID_PUBLIC, cursor.getLong(5));
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void testAutocryptStatus_autocryptPeer_withMutualKey() throws Exception {
        insertSecretKeyringFrom("/test-keys/testring.sec");
        insertPublicKeyringFrom("/test-keys/testring.pub");

        autocryptPeerDao.insertOrUpdateLastSeen("tid", new Date());
        autocryptPeerDao.updateKey(AUTOCRYPT_PEER, new Date(), KEY_ID_PUBLIC, true);

        Cursor cursor = autocryptStatusProvider.query(
                AutocryptStatus.CONTENT_URI, new String[] {
                        AutocryptStatus.ADDRESS, AutocryptStatus.UID_KEY_STATUS, AutocryptStatus.UID_ADDRESS,
                        AutocryptStatus.AUTOCRYPT_PEER_STATE,
                        AutocryptStatus.AUTOCRYPT_KEY_STATUS, AutocryptStatus.AUTOCRYPT_MASTER_KEY_ID
                },
                null, new String [] { AUTOCRYPT_PEER }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("tid", cursor.getString(0));
        assertTrue(cursor.isNull(1));
        assertEquals(null, cursor.getString(2));
        assertEquals(AutocryptStatus.AUTOCRYPT_PEER_MUTUAL, cursor.getInt(3));
        assertEquals(KeychainExternalContract.KEY_STATUS_UNVERIFIED, cursor.getInt(4));
        assertEquals(KEY_ID_PUBLIC, cursor.getLong(5));
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void testAutocryptStatus_available_withConfirmedKey() throws Exception {
        insertSecretKeyringFrom("/test-keys/testring.sec");
        insertPublicKeyringFrom("/test-keys/testring.pub");

        autocryptPeerDao.insertOrUpdateLastSeen("tid", new Date());
        autocryptPeerDao.updateKey(AUTOCRYPT_PEER, new Date(), KEY_ID_PUBLIC, false);
        certifyKey(KEY_ID_SECRET, KEY_ID_PUBLIC, USER_ID_1);

        Cursor cursor = autocryptStatusProvider.query(
                AutocryptStatus.CONTENT_URI, new String[] {
                        AutocryptStatus.ADDRESS, AutocryptStatus.UID_KEY_STATUS, AutocryptStatus.UID_ADDRESS,
                        AutocryptStatus.AUTOCRYPT_PEER_STATE, AutocryptStatus.AUTOCRYPT_KEY_STATUS },
                null, new String [] { AUTOCRYPT_PEER }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("tid", cursor.getString(0));
        assertEquals(KeychainExternalContract.KEY_STATUS_UNAVAILABLE, cursor.getInt(1));
        assertEquals(null, cursor.getString(2));
        assertEquals(AutocryptStatus.AUTOCRYPT_PEER_AVAILABLE, cursor.getInt(3));
        assertEquals(KeychainExternalContract.KEY_STATUS_VERIFIED, cursor.getInt(4));
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void testAutocryptStatus_noData() throws Exception {
        Cursor cursor = autocryptStatusProvider.query(
                AutocryptStatus.CONTENT_URI, new String[] {
                        AutocryptStatus.ADDRESS, AutocryptStatus.UID_KEY_STATUS, AutocryptStatus.UID_ADDRESS,
                        AutocryptStatus.AUTOCRYPT_PEER_STATE, AutocryptStatus.AUTOCRYPT_KEY_STATUS },
                null, new String [] { AUTOCRYPT_PEER }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("tid", cursor.getString(0));
        assertEquals(KeychainExternalContract.KEY_STATUS_UNAVAILABLE, cursor.getInt(1));
        assertEquals(null, cursor.getString(2));
        assertEquals(AutocryptStatus.AUTOCRYPT_PEER_DISABLED, cursor.getInt(3));
        assertTrue(cursor.isNull(4));
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void testAutocryptStatus_afterDelete() throws Exception {
        insertSecretKeyringFrom("/test-keys/testring.sec");
        insertPublicKeyringFrom("/test-keys/testring.pub");

        autocryptPeerDao.insertOrUpdateLastSeen("tid", new Date());
        autocryptPeerDao.updateKeyGossip("tid", new Date(), KEY_ID_PUBLIC, GossipOrigin.GOSSIP_HEADER);
        autocryptPeerDao.deleteByIdentifier("tid");

        Cursor cursor = autocryptStatusProvider.query(
                AutocryptStatus.CONTENT_URI, new String[] {
                        AutocryptStatus.ADDRESS, AutocryptStatus.UID_KEY_STATUS, AutocryptStatus.UID_ADDRESS,
                        AutocryptStatus.AUTOCRYPT_PEER_STATE, AutocryptStatus.AUTOCRYPT_KEY_STATUS },
                null, new String [] { AUTOCRYPT_PEER }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("tid", cursor.getString(0));
        assertEquals(KeychainExternalContract.KEY_STATUS_UNAVAILABLE, cursor.getInt(1));
        assertEquals(null, cursor.getString(2));
        assertEquals(AutocryptStatus.AUTOCRYPT_PEER_DISABLED, cursor.getInt(3));
        assertTrue(cursor.isNull(4));
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void testAutocryptStatus_stateGossip() throws Exception {
        insertSecretKeyringFrom("/test-keys/testring.sec");
        insertPublicKeyringFrom("/test-keys/testring.pub");

        autocryptPeerDao.insertOrUpdateLastSeen("tid", new Date());
        autocryptPeerDao.updateKeyGossip(AUTOCRYPT_PEER, new Date(), KEY_ID_PUBLIC, GossipOrigin.GOSSIP_HEADER);
        certifyKey(KEY_ID_SECRET, KEY_ID_PUBLIC, USER_ID_1);

        Cursor cursor = autocryptStatusProvider.query(
                AutocryptStatus.CONTENT_URI, new String[] {
                        AutocryptStatus.ADDRESS, AutocryptStatus.UID_KEY_STATUS, AutocryptStatus.UID_ADDRESS,
                        AutocryptStatus.AUTOCRYPT_PEER_STATE, AutocryptStatus.AUTOCRYPT_KEY_STATUS },
                null, new String [] { AUTOCRYPT_PEER }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("tid", cursor.getString(0));
        assertEquals(KeychainExternalContract.KEY_STATUS_UNAVAILABLE, cursor.getInt(1));
        assertEquals(null, cursor.getString(2));
        assertEquals(AutocryptStatus.AUTOCRYPT_PEER_GOSSIP, cursor.getInt(3));
        assertEquals(KeychainExternalContract.KEY_STATUS_VERIFIED, cursor.getInt(4));
        assertFalse(cursor.moveToNext());
    }

/*
    @Test
    public void testAutocryptStatus_stateSelected() throws Exception {
        insertSecretKeyringFrom("/test-keys/testring.sec");
        insertPublicKeyringFrom("/test-keys/testring.pub");

        autocryptPeerDao.updateToSelectedState("tid", KEY_ID_PUBLIC);
        certifyKey(KEY_ID_SECRET, KEY_ID_PUBLIC, USER_ID_1);

        Cursor cursor = contentResolver.query(
                AutocryptStatus.CONTENT_URI, new String[] {
                        AutocryptStatus.ADDRESS, AutocryptStatus.UID_KEY_STATUS, AutocryptStatus.UID_ADDRESS,
                        AutocryptStatus.AUTOCRYPT_PEER_STATE, AutocryptStatus.AUTOCRYPT_KEY_STATUS },
                null, new String [] { AUTOCRYPT_PEER }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals("tid", cursor.getString(0));
        assertEquals(KeychainExternalContract.KEY_STATUS_UNAVAILABLE, cursor.getInt(1));
        assertEquals(null, cursor.getString(2));
        assertEquals(AutocryptStatus.AUTOCRYPT_PEER_SELECTED, cursor.getInt(3));
        assertEquals(KeychainExternalContract.KEY_STATUS_VERIFIED, cursor.getInt(4));
        assertFalse(cursor.moveToNext());
    }
*/

    @Test
    public void testAutocryptStatus_withConfirmedKey() throws Exception {
        insertSecretKeyringFrom("/test-keys/testring.sec");
        insertPublicKeyringFrom("/test-keys/testring.pub");

        certifyKey(KEY_ID_SECRET, KEY_ID_PUBLIC, USER_ID_1);

        Cursor cursor = autocryptStatusProvider.query(
                AutocryptStatus.CONTENT_URI, new String[] {
                        AutocryptStatus.ADDRESS, AutocryptStatus.UID_KEY_STATUS, AutocryptStatus.UID_ADDRESS,
                        AutocryptStatus.AUTOCRYPT_PEER_STATE },
                null, new String [] { MAIL_ADDRESS_1 }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(MAIL_ADDRESS_1, cursor.getString(0));
        assertEquals(KeychainExternalContract.KEY_STATUS_VERIFIED, cursor.getInt(1));
        assertEquals(USER_ID_1, cursor.getString(2));
        assertEquals(AutocryptStatus.AUTOCRYPT_PEER_DISABLED, cursor.getInt(3));
        assertFalse(cursor.moveToNext());
    }

    private void certifyKey(long secretMasterKeyId, long publicMasterKeyId, String userId) {
        CertifyActionsParcel.Builder certifyActionsParcel = CertifyActionsParcel.builder(secretMasterKeyId);
        certifyActionsParcel.addAction(
                CertifyAction.createForUserIds(publicMasterKeyId, Collections.singletonList(userId)));
        CertifyOperation op = new CertifyOperation(
                RuntimeEnvironment.application, databaseInteractor, new ProgressScaler(), null);
        CertifyResult certifyResult = op.execute(certifyActionsParcel.build(), CryptoInputParcel.createCryptoInputParcel());

        assertTrue(certifyResult.success());
    }

    private void insertPublicKeyringFrom(String filename) throws Exception {
        UncachedKeyRing ring = readRingFromResource(filename);
        SaveKeyringResult saveKeyringResult = databaseInteractor.savePublicKeyRing(ring);
        assertTrue(saveKeyringResult.success());
    }

    private void insertSecretKeyringFrom(String filename) throws Exception {
        UncachedKeyRing ring = readRingFromResource(filename);
        SaveKeyringResult saveKeyringResult = databaseInteractor.saveSecretKeyRing(ring);
        assertTrue(saveKeyringResult.success());
    }

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(KeyRepositorySaveTest.class.getResourceAsStream(name)).next();
    }
}
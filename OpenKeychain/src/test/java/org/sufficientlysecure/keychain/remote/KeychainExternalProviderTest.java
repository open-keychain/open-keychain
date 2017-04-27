package org.sufficientlysecure.keychain.remote;


import java.security.AccessControlException;
import java.util.Collections;

import android.content.ContentResolver;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.database.Cursor;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.res.builder.RobolectricPackageManager;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.operations.CertifyOperation;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.SaveKeyringResult;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.ApiDataAccessObject;
import org.sufficientlysecure.keychain.provider.KeyWritableRepository;
import org.sufficientlysecure.keychain.provider.KeychainExternalContract.EmailStatus;
import org.sufficientlysecure.keychain.provider.KeyRepositorySaveTest;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import static org.junit.Assert.*;


@SuppressWarnings("WeakerAccess")
@RunWith(KeychainTestRunner.class)
public class KeychainExternalProviderTest {
    static final String PACKAGE_NAME = "test.package";
    static final byte[] PACKAGE_SIGNATURE = new byte[] { 1, 2, 3 };
    static final String MAIL_ADDRESS_1 = "twi@openkeychain.org";
    static final String MAIL_ADDRESS_2 = "pink@openkeychain.org";
    static final String MAIL_ADDRESS_SEC_1 = "twi-sec@openkeychain.org";
    static final String USER_ID_1 = "twi <twi@openkeychain.org>";
    static final String USER_ID_SEC_1 = "twi <twi-sec@openkeychain.org>";
    static final long KEY_ID_SECRET = 0x5D4DA4423C39122FL;
    static final long KEY_ID_PUBLIC = 0x9A282CE2AB44A382L;


    KeyWritableRepository databaseInteractor =
            KeyWritableRepository.createDatabaseReadWriteInteractor(RuntimeEnvironment.application);
    ContentResolver contentResolver = RuntimeEnvironment.application.getContentResolver();
    ApiPermissionHelper apiPermissionHelper;
    ApiDataAccessObject apiDao;


    @Before
    public void setUp() throws Exception {
        RobolectricPackageManager rpm = (RobolectricPackageManager) RuntimeEnvironment.getPackageManager();
        rpm.setPackagesForUid(0, PACKAGE_NAME);
        PackageInfo packageInfo = new PackageInfo();
        packageInfo.signatures = new Signature[] { new Signature(PACKAGE_SIGNATURE) };
        packageInfo.packageName = PACKAGE_NAME;
        rpm.addPackage(packageInfo);

        apiDao = new ApiDataAccessObject(RuntimeEnvironment.application);
        apiPermissionHelper = new ApiPermissionHelper(RuntimeEnvironment.application, apiDao);

        apiDao.insertApiApp(new AppSettings(PACKAGE_NAME, PACKAGE_SIGNATURE));
    }

    @Test(expected = AccessControlException.class)
    public void testPermission__withMissingPackage() throws Exception {
        apiDao.deleteApiApp(PACKAGE_NAME);

        contentResolver.query(
                EmailStatus.CONTENT_URI,
                new String[] { EmailStatus.EMAIL_ADDRESS, EmailStatus.EMAIL_ADDRESS, EmailStatus.USER_ID },
                null, new String [] { }, null
        );
    }

    @Test(expected = AccessControlException.class)
    public void testPermission__withExplicitPackage() throws Exception {
        contentResolver.query(
                EmailStatus.CONTENT_URI.buildUpon().appendPath("fake_pkg").build(),
                new String[] { EmailStatus.EMAIL_ADDRESS, EmailStatus.EMAIL_ADDRESS, EmailStatus.USER_ID },
                null, new String [] { }, null
        );
    }

    @Test(expected = AccessControlException.class)
    public void testPermission__withWrongPackageCert() throws Exception {
        apiDao.deleteApiApp(PACKAGE_NAME);
        apiDao.insertApiApp(new AppSettings(PACKAGE_NAME, new byte[] { 1, 2, 4 }));

        contentResolver.query(
                EmailStatus.CONTENT_URI,
                new String[] { EmailStatus.EMAIL_ADDRESS, EmailStatus.EMAIL_ADDRESS, EmailStatus.USER_ID },
                null, new String [] { }, null
        );
    }

    @Test
    public void testQuery__withNonExistentAddress() throws Exception {
        Cursor cursor = contentResolver.query(
                EmailStatus.CONTENT_URI, new String[] {
                        EmailStatus.EMAIL_ADDRESS, EmailStatus.USER_ID_STATUS, EmailStatus.USER_ID },
                null, new String [] { MAIL_ADDRESS_1 }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(MAIL_ADDRESS_1, cursor.getString(0));
        assertEquals(0, cursor.getInt(1));
        assertTrue(cursor.isNull(2));
    }

    @Test
    public void testQuery() throws Exception {
        insertPublicKeyringFrom("/test-keys/testring.pub");

        Cursor cursor = contentResolver.query(
                EmailStatus.CONTENT_URI, new String[] {
                        EmailStatus.EMAIL_ADDRESS, EmailStatus.USER_ID_STATUS, EmailStatus.USER_ID },
                null, new String [] { MAIL_ADDRESS_1 }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(MAIL_ADDRESS_1, cursor.getString(0));
        assertEquals(1, cursor.getInt(1));
        assertEquals("twi <twi@openkeychain.org>", cursor.getString(2));
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void testQuery__multiple() throws Exception {
        insertPublicKeyringFrom("/test-keys/testring.pub");

        Cursor cursor = contentResolver.query(
                EmailStatus.CONTENT_URI, new String[] {
                        EmailStatus.EMAIL_ADDRESS, EmailStatus.USER_ID_STATUS, EmailStatus.USER_ID },
                null, new String [] { MAIL_ADDRESS_1, MAIL_ADDRESS_2 }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToNext());
        assertEquals(MAIL_ADDRESS_2, cursor.getString(0));
        assertEquals(0, cursor.getInt(1));
        assertTrue(cursor.isNull(2));
        assertTrue(cursor.moveToNext());
        assertEquals(MAIL_ADDRESS_1, cursor.getString(0));
        assertEquals(1, cursor.getInt(1));
        assertEquals("twi <twi@openkeychain.org>", cursor.getString(2));
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void testQuery__withSecretKey() throws Exception {
        insertSecretKeyringFrom("/test-keys/testring.sec");

        Cursor cursor = contentResolver.query(
                EmailStatus.CONTENT_URI, new String[] {
                        EmailStatus.EMAIL_ADDRESS, EmailStatus.USER_ID_STATUS, EmailStatus.USER_ID },
                null, new String [] { MAIL_ADDRESS_SEC_1 }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(MAIL_ADDRESS_SEC_1, cursor.getString(0));
        assertEquals(USER_ID_SEC_1, cursor.getString(2));
        assertEquals(2, cursor.getInt(1));
        assertFalse(cursor.moveToNext());
    }

    @Test
    public void testQuery__withConfirmedKey() throws Exception {
        insertSecretKeyringFrom("/test-keys/testring.sec");
        insertPublicKeyringFrom("/test-keys/testring.pub");

        certifyKey(KEY_ID_SECRET, KEY_ID_PUBLIC, USER_ID_1);

        Cursor cursor = contentResolver.query(
                EmailStatus.CONTENT_URI, new String[] {
                        EmailStatus.EMAIL_ADDRESS, EmailStatus.USER_ID_STATUS, EmailStatus.USER_ID },
                null, new String [] { MAIL_ADDRESS_1 }, null
        );

        assertNotNull(cursor);
        assertTrue(cursor.moveToFirst());
        assertEquals(MAIL_ADDRESS_1, cursor.getString(0));
        assertEquals(USER_ID_1, cursor.getString(2));
        assertEquals(2, cursor.getInt(1));
        assertFalse(cursor.moveToNext());
    }

    private void certifyKey(long secretMasterKeyId, long publicMasterKeyId, String userId) {
        CertifyActionsParcel certifyActionsParcel = new CertifyActionsParcel(secretMasterKeyId);
        certifyActionsParcel.add(new CertifyAction(publicMasterKeyId, Collections.singletonList(userId), null));
        CertifyOperation op = new CertifyOperation(
                RuntimeEnvironment.application, databaseInteractor, new ProgressScaler(), null);
        CertifyResult certifyResult = op.execute(certifyActionsParcel, new CryptoInputParcel());

        assertTrue(certifyResult.success());
    }

    private void insertPublicKeyringFrom(String filename) throws Exception {
        UncachedKeyRing ring = readRingFromResource(filename);
        SaveKeyringResult saveKeyringResult = databaseInteractor.savePublicKeyRing(ring);
        assertTrue(saveKeyringResult.success());
    }

    private void insertSecretKeyringFrom(String filename) throws Exception {
        UncachedKeyRing ring = readRingFromResource(filename);
        SaveKeyringResult saveKeyringResult = databaseInteractor.saveSecretKeyRing(ring, new ProgressScaler());
        assertTrue(saveKeyringResult.success());
    }

    UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(KeyRepositorySaveTest.class.getResourceAsStream(name)).next();
    }
}
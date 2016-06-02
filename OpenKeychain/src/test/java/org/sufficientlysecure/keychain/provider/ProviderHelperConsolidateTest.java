/*
 * Copyright (C) 2016 Alex Fong Jie Wen <alexfongg@gmail.com>
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

package org.sufficientlysecure.keychain.provider;

import android.content.ContentResolver;
import android.database.Cursor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import org.sufficientlysecure.keychain.WorkaroundBuildConfig;
import org.sufficientlysecure.keychain.pgp.UncachedKeyRing;
import org.sufficientlysecure.keychain.provider.KeychainContract.Certs;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.provider.KeychainContract.Keys;
import org.sufficientlysecure.keychain.provider.KeychainContract.UserPackets;
import org.sufficientlysecure.keychain.util.ProgressScaler;

import java.util.ArrayList;
import java.util.Arrays;

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = WorkaroundBuildConfig.class, sdk = 21, manifest = "src/main/AndroidManifest.xml")
public class ProviderHelperConsolidateTest {

    ProviderHelper mProviderHelper = new ProviderHelper(RuntimeEnvironment.application);

    @BeforeClass
    public static void setUpOnce() throws Exception {
        ShadowLog.stream = System.out;
    }

    @Before
    public void setUp() {
        // clear database
        mProviderHelper.getContentResolver().delete(
                KeyRings.buildUnifiedKeyRingsUri(), null, null);
    }

    @Test
    public void testConsolidateBasicConfig() throws Exception {
        // insert secret, this should work
        UncachedKeyRing secKey = readRingFromResource("/test-keys/basic-gpg-secret.asc");
        mProviderHelper.saveSecretKeyRing(secKey);
        Long masterKeyId = secKey.getMasterKeyId();

        ContentResolver resolver = mProviderHelper.getContentResolver();
        Cursor keyCursor,
                userPacketCursor,
                certsCursor;

        // ensure query results are sorted
        String keysSortOrder = Keys.MASTER_KEY_ID + " DESC";
        String userPacketSortOrder = KeychainDatabase.Tables.USER_PACKETS + "." + keysSortOrder;
        String certsSortOrder = KeychainDatabase.Tables.CERTS + "." + keysSortOrder;

        // get data before consolidate
        keyCursor = resolver.query(Keys.buildKeysUri(masterKeyId), null, null, null, keysSortOrder);
        userPacketCursor = resolver.query(UserPackets.buildUserIdsUri(masterKeyId), null, null, null, userPacketSortOrder);
        certsCursor = resolver.query(Certs.buildCertsUri(masterKeyId), null, null, null, certsSortOrder);
        ArrayList<CursorRow> keyDataBeforeConsolidate = extractData(keyCursor);
        ArrayList<CursorRow> userDataBeforeConsolidate = extractData(userPacketCursor);
        ArrayList<CursorRow> certsDataBeforeConsolidate = extractData(certsCursor);
        closeCursor(keyCursor);
        closeCursor(userPacketCursor);
        closeCursor(certsCursor);

        mProviderHelper.consolidateDatabaseStep1(new ProgressScaler());

        // get data after consolidate
        keyCursor = resolver.query(Keys.buildKeysUri(masterKeyId), null, null, null , keysSortOrder);
        userPacketCursor = resolver.query(UserPackets.buildUserIdsUri(), null, null, null, userPacketSortOrder);
        certsCursor = resolver.query(Certs.buildCertsUri(masterKeyId), null, null, null, certsSortOrder);
        ArrayList<CursorRow> keyDataAfterConsolidate = extractData(keyCursor);
        ArrayList<CursorRow> userDataAfterConsolidate = extractData(userPacketCursor);
        ArrayList<CursorRow> certsDataAfterConsolidate = extractData(certsCursor);
        closeCursor(keyCursor);
        closeCursor(userPacketCursor);
        closeCursor(certsCursor);

        // compare data
        boolean result = cursorRowsAreEqual(keyDataBeforeConsolidate, keyDataAfterConsolidate);
        Assert.assertEquals("Subkey data is not equal.", true, result);
        result = cursorRowsAreEqual(userDataBeforeConsolidate, userDataAfterConsolidate);
        Assert.assertEquals("User packet data is not equal.", true, result);
        result = cursorRowsAreEqual(certsDataBeforeConsolidate, certsDataAfterConsolidate);
        Assert.assertEquals("Cert data is not equal.", true, result);
    }


    private boolean cursorRowsAreEqual(ArrayList<CursorRow> list1, ArrayList<CursorRow> list2) {
        for (int i = 0; i < list1.size(); i++) {
            CursorRow row1 = list1.get(i);
            CursorRow row2 = list2.get(i);
            if(!row1.equals(row2)) {
                return false;
            }
        }

        return true;
    }

    private ArrayList<CursorRow> extractData(Cursor cursor) {
        ArrayList<CursorRow> list = new ArrayList<>();

        if(cursor == null || cursor.isClosed()) {
            return list;
        } else {
            cursor.moveToFirst();
            while(!cursor.isAfterLast()) {
                CursorRow row = extractRow(cursor);
                list.add(row);
                cursor.moveToNext();
            }
        }

        return list;
    }

    private CursorRow extractRow(Cursor cursor) {
        CursorRow row = new CursorRow();
        int numCols = cursor.getColumnCount();

        if(!cursor.isClosed() && !cursor.isAfterLast()) {
            for (int index = 0; index < numCols; index++) {
                int colType = cursor.getType(index);
                switch(colType) {
                    case(Cursor.FIELD_TYPE_NULL): {
                        row.booleans.add(cursor.isNull(index));
                        break;
                    }
                    case(Cursor.FIELD_TYPE_BLOB): {
                        row.blobs.add(cursor.getBlob(index));
                        break;
                    }
                    case(Cursor.FIELD_TYPE_FLOAT): {
                        row.floats.add(cursor.getFloat(index));
                        break;
                    }
                    case(Cursor.FIELD_TYPE_INTEGER): {
                        row.ints.add(cursor.getInt(index));
                        break;
                    }
                    case(Cursor.FIELD_TYPE_STRING): {
                        row.strings.add(cursor.getString(index));
                        break;
                    }
                }
            }
        }

        return row;
    }

    private UncachedKeyRing readRingFromResource(String name) throws Exception {
        return UncachedKeyRing.fromStream(this.getClass()
                .getResourceAsStream(name)).next();
    }

    private void closeCursor(Cursor cursor) {
        if(cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    /**
     * Beware of simple hash code implementation
     */
    private static class CursorRow {
        public final ArrayList<byte[]> blobs;
        public final ArrayList<Float> floats;
        public final ArrayList<Integer> ints;
        public final ArrayList<String> strings;
        public final ArrayList<Boolean> booleans;

        public CursorRow() {
            blobs = new ArrayList<>();
            floats = new ArrayList<>();
            ints = new ArrayList<>();
            strings = new ArrayList<>();
            booleans = new ArrayList<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            CursorRow other = (CursorRow) o;

            // check blobs
            for (int i = 0; i < blobs.size(); i++) {
                byte[] blob = blobs.get(i);
                byte[] otherBlob = other.blobs.get(i);

                if(otherBlob == null || !Arrays.equals(blob, otherBlob)) {
                    return false;
                }
            }

            // check rest
            return (floats.equals(other.floats)
                    && ints.equals(other.ints)
                    && strings.equals(other.strings)
                    && booleans.equals(other.booleans));
        }

        @Override
        public int hashCode() {
            return 17;
        }
    }

}

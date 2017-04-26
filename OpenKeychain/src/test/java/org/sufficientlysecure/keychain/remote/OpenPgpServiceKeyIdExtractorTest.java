package org.sufficientlysecure.keychain.remote;


import java.util.ArrayList;
import java.util.Arrays;

import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.MatrixCursor;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.remote.OpenPgpServiceKeyIdExtractor.KeyIdResult;
import org.sufficientlysecure.keychain.remote.OpenPgpServiceKeyIdExtractor.KeyIdResultStatus;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
@RunWith(KeychainTestRunner.class)
public class OpenPgpServiceKeyIdExtractorTest {

    private static final long[] KEY_IDS = new long[] { 123L, 234L };
    private static final String[] USER_IDS = new String[] { "user1@example.org", "User 2 <user2@example.org>" };
    private OpenPgpServiceKeyIdExtractor openPgpServiceKeyIdExtractor;
    private ContentResolver contentResolver;
    private ApiPendingIntentFactory apiPendingIntentFactory;

    @Before
    public void setUp() throws Exception {
        contentResolver = mock(ContentResolver.class);
        apiPendingIntentFactory = mock(ApiPendingIntentFactory.class);

        openPgpServiceKeyIdExtractor = OpenPgpServiceKeyIdExtractor.getInstance(contentResolver,
                apiPendingIntentFactory);
    }

    @Test
    public void returnKeyIdsFromIntent__withKeyIdsExtra() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_KEY_IDS, KEY_IDS);

        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false,
                BuildConfig.APPLICATION_ID);

        assertEquals(KeyIdResultStatus.NO_KEYS_ERROR, keyIdResult.getStatus());
        assertFalse(keyIdResult.hasKeySelectionPendingIntent());
        assertArrayEqualsSorted(KEY_IDS, keyIdResult.getKeyIds());
    }

    @Test
    public void returnKeyIdsFromIntent__withKeyIdsSelectedExtra() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_KEY_IDS_SELECTED, KEY_IDS);
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS); // should be ignored

        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false,
                BuildConfig.APPLICATION_ID);

        assertEquals(KeyIdResultStatus.OK, keyIdResult.getStatus());
        assertFalse(keyIdResult.hasKeySelectionPendingIntent());
        assertArrayEqualsSorted(KEY_IDS, keyIdResult.getKeyIds());
    }

    @Test
    public void returnKeyIdsFromIntent__withUserIds__withEmptyQueryResult() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS);

        setupContentResolverResult();

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false,
                BuildConfig.APPLICATION_ID);


        assertEquals(KeyIdResultStatus.NO_KEYS, keyIdResult.getStatus());
        assertTrue(keyIdResult.hasKeySelectionPendingIntent());
    }

    @Test
    public void returnKeyIdsFromIntent__withNoData() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, new String[] { });

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false,
                BuildConfig.APPLICATION_ID);


        assertEquals(KeyIdResultStatus.NO_KEYS, keyIdResult.getStatus());
        assertTrue(keyIdResult.hasKeySelectionPendingIntent());
        assertSame(pendingIntent, keyIdResult.getKeySelectionPendingIntent());
    }

    @Test
    public void returnKeyIdsFromIntent__withEmptyUserId() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, new String[0]);

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);

        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false,
                BuildConfig.APPLICATION_ID);


        assertEquals(KeyIdResultStatus.NO_KEYS, keyIdResult.getStatus());
        assertTrue(keyIdResult.hasKeySelectionPendingIntent());
        assertSame(pendingIntent, keyIdResult.getKeySelectionPendingIntent());
    }

    @Test
    public void returnKeyIdsFromIntent__withNoData__askIfNoData() throws Exception {
        Intent intent = new Intent();

        setupContentResolverResult();

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, true,
                BuildConfig.APPLICATION_ID);


        assertEquals(KeyIdResultStatus.NO_KEYS, keyIdResult.getStatus());
        assertTrue(keyIdResult.hasKeySelectionPendingIntent());
        assertSame(pendingIntent, keyIdResult.getKeySelectionPendingIntent());
    }

    @Test
    public void returnKeyIdsFromIntent__withUserIds() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS);

        setupContentResolverResult(USER_IDS, new Long[] { 123L, 234L }, new int[] { 0, 0 });


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false,
                BuildConfig.APPLICATION_ID);


        assertEquals(KeyIdResultStatus.OK, keyIdResult.getStatus());
        assertFalse(keyIdResult.hasKeySelectionPendingIntent());
        assertArrayEqualsSorted(KEY_IDS, keyIdResult.getKeyIds());
    }

    @Test
    public void returnKeyIdsFromIntent__withUserIds__withDuplicate() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS);

        setupContentResolverResult(new String[] {
                USER_IDS[0], USER_IDS[0], USER_IDS[1]
        }, new Long[] { 123L, 345L, 234L }, new int[] { 0, 0, 0 });

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false,
                BuildConfig.APPLICATION_ID);


        assertEquals(KeyIdResultStatus.DUPLICATE, keyIdResult.getStatus());
        assertTrue(keyIdResult.hasKeySelectionPendingIntent());
    }

    @Test
    public void returnKeyIdsFromIntent__withUserIds__withMissing() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS);

        setupContentResolverResult(USER_IDS, new Long[] { null, 234L }, new int[] { 0, 0 });

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false,
                BuildConfig.APPLICATION_ID);


        assertEquals(KeyIdResultStatus.MISSING, keyIdResult.getStatus());
        assertTrue(keyIdResult.hasKeySelectionPendingIntent());
    }

    private void setupContentResolverResult() {
        MatrixCursor resultCursor = new MatrixCursor(OpenPgpServiceKeyIdExtractor.PROJECTION_KEY_SEARCH);
        when(contentResolver.query(
                any(Uri.class), any(String[].class), any(String.class), any(String[].class), any(String.class)))
                .thenReturn(resultCursor);
    }

    private void setupContentResolverResult(String[] userIds, Long[] resultKeyIds, int[] verified) {
        MatrixCursor resultCursor = new MatrixCursor(OpenPgpServiceKeyIdExtractor.PROJECTION_KEY_SEARCH);
        for (int i = 0; i < userIds.length; i++) {
            resultCursor.addRow(new Object[] { userIds[i], resultKeyIds[i], verified[i] });
        }

        when(contentResolver.query(
                any(Uri.class), any(String[].class), any(String.class), any(String[].class), any(String.class)))
                .thenReturn(resultCursor);
    }

    private void setupPendingIntentFactoryResult(PendingIntent pendingIntent) {
        when(apiPendingIntentFactory.createSelectPublicKeyPendingIntent(
                any(Intent.class), any(long[].class), any(ArrayList.class), any(ArrayList.class), any(Boolean.class)))
                .thenReturn(pendingIntent);
    }


    private static void assertArrayEqualsSorted(long[] a, long[] b) {
        long[] tmpA = Arrays.copyOf(a, a.length);
        long[] tmpB = Arrays.copyOf(b, b.length);
        Arrays.sort(tmpA);
        Arrays.sort(tmpB);

        assertArrayEquals(tmpA, tmpB);
    }
}
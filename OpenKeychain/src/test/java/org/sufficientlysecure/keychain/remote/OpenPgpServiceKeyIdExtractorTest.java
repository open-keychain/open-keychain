package org.sufficientlysecure.keychain.remote;


import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.KeychainTestRunner;
import org.sufficientlysecure.keychain.remote.OpenPgpServiceKeyIdExtractor.KeyIdResult;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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

        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false);

        assertFalse(keyIdResult.hasResultIntent());
        assertArrayEqualsSorted(KEY_IDS, keyIdResult.getKeyIds());
    }

    @Test
    public void returnKeyIdsFromIntent__withKeyIdsSelectedExtra() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_KEY_IDS_SELECTED, KEY_IDS);
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS); // should be ignored

        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false);

        assertFalse(keyIdResult.hasResultIntent());
        assertArrayEqualsSorted(KEY_IDS, keyIdResult.getKeyIds());
    }

    @Test
    public void returnKeyIdsFromIntent__withUserIds__withEmptyQueryResult() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS);

        setupContentResolverResult();

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false);


        assertTrue(keyIdResult.hasResultIntent());
        Intent resultIntent = keyIdResult.getResultIntent();
        assertEquals(OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED,
                resultIntent.getIntExtra(OpenPgpApi.RESULT_CODE, Integer.MAX_VALUE));
        assertEquals(pendingIntent, resultIntent.getParcelableExtra(OpenPgpApi.RESULT_INTENT));
    }

    @Test
    public void returnKeyIdsFromIntent__withNoData() throws Exception {
        Intent intent = new Intent();


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false);


        assertTrue(keyIdResult.hasResultIntent());
        Intent resultIntent = keyIdResult.getResultIntent();
        assertEquals(OpenPgpApi.RESULT_CODE_ERROR,
                resultIntent.getIntExtra(OpenPgpApi.RESULT_CODE, Integer.MAX_VALUE));
        assertEquals(OpenPgpError.NO_USER_IDS,
                resultIntent.<OpenPgpError>getParcelableExtra(OpenPgpApi.RESULT_ERROR).getErrorId());
    }

    @Test
    public void returnKeyIdsFromIntent__withEmptyUserId() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, new String[0]);

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);

        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false);


        assertTrue(keyIdResult.hasResultIntent());
        Intent resultIntent = keyIdResult.getResultIntent();
        assertEquals(OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED,
                resultIntent.getIntExtra(OpenPgpApi.RESULT_CODE, Integer.MAX_VALUE));
        assertEquals(pendingIntent, resultIntent.getParcelableExtra(OpenPgpApi.RESULT_INTENT));
    }

    @Test
    public void returnKeyIdsFromIntent__withNoData__askIfNoData() throws Exception {
        Intent intent = new Intent();

        setupContentResolverResult();

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, true);


        assertTrue(keyIdResult.hasResultIntent());
        Intent resultIntent = keyIdResult.getResultIntent();
        assertEquals(OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED,
                resultIntent.getIntExtra(OpenPgpApi.RESULT_CODE, Integer.MAX_VALUE));
        assertEquals(pendingIntent, resultIntent.getParcelableExtra(OpenPgpApi.RESULT_INTENT));
    }

    @Test
    public void returnKeyIdsFromIntent__withUserIds__withEmptyQueryResult__inOpportunisticMode() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS);
        intent.putExtra(OpenPgpApi.EXTRA_OPPORTUNISTIC_ENCRYPTION, true);

        setupContentResolverResult();


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false);


        assertTrue(keyIdResult.hasResultIntent());
        Intent resultIntent = keyIdResult.getResultIntent();
        assertEquals(OpenPgpApi.RESULT_CODE_ERROR, resultIntent.getIntExtra(OpenPgpApi.RESULT_CODE, Integer.MAX_VALUE));
        assertEquals(OpenPgpError.OPPORTUNISTIC_MISSING_KEYS,
                resultIntent.<OpenPgpError>getParcelableExtra(OpenPgpApi.RESULT_ERROR).getErrorId());
    }

    @Test
    public void returnKeyIdsFromIntent__withUserIds() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS);

        setupContentResolverResult(new long[][] {
                new long[] { 123L },
                new long[] { 234L }
        });


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false);


        assertFalse(keyIdResult.hasResultIntent());
        assertArrayEqualsSorted(KEY_IDS, keyIdResult.getKeyIds());
    }

    @Test
    public void returnKeyIdsFromIntent__withUserIds__withDuplicate() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS);

        setupContentResolverResult(new long[][] {
                new long[] { 123L, 345L },
                new long[] { 234L }
        });

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false);


        assertTrue(keyIdResult.hasResultIntent());
        Intent resultIntent = keyIdResult.getResultIntent();
        assertEquals(OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED,
                resultIntent.getIntExtra(OpenPgpApi.RESULT_CODE, Integer.MAX_VALUE));
        assertEquals(pendingIntent, resultIntent.getParcelableExtra(OpenPgpApi.RESULT_INTENT));
    }

    @Test
    public void returnKeyIdsFromIntent__withUserIds__withMissing() throws Exception {
        Intent intent = new Intent();
        intent.putExtra(OpenPgpApi.EXTRA_USER_IDS, USER_IDS);

        setupContentResolverResult(new long[][] {
                new long[] { },
                new long[] { 234L }
        });

        PendingIntent pendingIntent = mock(PendingIntent.class);
        setupPendingIntentFactoryResult(pendingIntent);


        KeyIdResult keyIdResult = openPgpServiceKeyIdExtractor.returnKeyIdsFromIntent(intent, false);


        assertTrue(keyIdResult.hasResultIntent());
        Intent resultIntent = keyIdResult.getResultIntent();
        assertEquals(OpenPgpApi.RESULT_CODE_USER_INTERACTION_REQUIRED,
                resultIntent.getIntExtra(OpenPgpApi.RESULT_CODE, Integer.MAX_VALUE));
        assertEquals(pendingIntent, resultIntent.getParcelableExtra(OpenPgpApi.RESULT_INTENT));
    }

    private void setupContentResolverResult() {
        MatrixCursor resultCursor = new MatrixCursor(OpenPgpServiceKeyIdExtractor.KEY_SEARCH_PROJECTION);
        when(contentResolver.query(
                any(Uri.class), any(String[].class), any(String.class), any(String[].class), any(String.class)))
                .thenReturn(resultCursor);
    }

    private void setupContentResolverResult(long[][] resultKeyIds) {
        Cursor[] resultCursors = new MatrixCursor[resultKeyIds.length];
        for (int i = 0; i < resultKeyIds.length; i++) {
            MatrixCursor resultCursor = new MatrixCursor(OpenPgpServiceKeyIdExtractor.KEY_SEARCH_PROJECTION);
            for (long keyId : resultKeyIds[i]) {
                resultCursor.addRow(new Object[] { keyId, keyId, 0L, 0L });
            }

            resultCursors[i] = resultCursor;
        }

        when(contentResolver.query(
                any(Uri.class), any(String[].class), any(String.class), any(String[].class), any(String.class)))
                .thenReturn(resultCursors[0], Arrays.copyOfRange(resultCursors, 1, resultCursors.length));
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
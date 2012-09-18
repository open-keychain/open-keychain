package org.thialfihar.android.apg.provider;

import java.io.IOException;

import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.thialfihar.android.apg.helper.PGPMain.ApgGeneralException;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeyRings;
import org.thialfihar.android.apg.provider.ApgContract.PublicKeys;
import org.thialfihar.android.apg.provider.ApgContract.SecretKeys;

import android.content.ContentValues;
import android.content.Context;

public class ProviderHelper {
    // public static void insertHostsSource(Context context, String url) {
    // ContentValues values = new ContentValues();
    // values.put(HostsSources.URL, url);
    // values.put(HostsSources.ENABLED, true); // default is enabled
    // values.put(HostsSources.LAST_MODIFIED_LOCAL, 0); // last_modified_local starts at 0
    // values.put(HostsSources.LAST_MODIFIED_ONLINE, 0); // last_modified_onlinestarts at 0
    // context.getContentResolver().insert(HostsSources.CONTENT_URI, values);
    // }

    // public int saveKeyRing(Context context, PGPPublicKeyRing keyRing) throws IOException,
    // ApgGeneralException {
    // // mDb.beginTransaction();
    // ContentValues values = new ContentValues();
    // PGPPublicKey masterKey = keyRing.getPublicKey();
    // long masterKeyId = masterKey.getKeyID();
    //
    // values.put(PublicKeyRings.MASTER_KEY_ID, masterKeyId);
    // // values.put(KeyRings.TYPE, Id.database.type_public);
    // values.put(PublicKeyRings.KEY_RING_DATA, keyRing.getEncoded());
    //
    // context.getContentResolver().insert(PublicKeyRings.CONTENT_URI, values);
    //
    // long rowId = insertOrUpdateKeyRing(values);
    // int returnValue = mStatus;
    //
    // if (rowId == -1) {
    // throw new ApgGeneralException("saving public key ring " + masterKeyId + " failed");
    // }
    //
    // Vector<Integer> seenIds = new Vector<Integer>();
    // int rank = 0;
    // for (PGPPublicKey key : new IterableIterator<PGPPublicKey>(keyRing.getPublicKeys())) {
    // seenIds.add(saveKey(rowId, key, rank));
    // ++rank;
    // }
    //
    // String seenIdsStr = "";
    // for (Integer id : seenIds) {
    // if (seenIdsStr.length() > 0) {
    // seenIdsStr += ",";
    // }
    // seenIdsStr += id;
    // }
    // mDb.delete(Keys.TABLE_NAME, Keys.KEY_RING_ID + " = ? AND " + Keys._ID + " NOT IN ("
    // + seenIdsStr + ")", new String[] { "" + rowId });
    //
    // mDb.setTransactionSuccessful();
    // mDb.endTransaction();
    // return returnValue;
    // }

    /**
     * Deletes public and secret keys
     * 
     * @param context
     * @param rowId
     */
    public static void deleteKey(Context context, long rowId) {
        context.getContentResolver().delete(PublicKeys.buildPublicKeysUri(Long.toString(rowId)),
                null, null);
        context.getContentResolver().delete(SecretKeys.buildSecretKeysUri(Long.toString(rowId)),
                null, null);
    }
}

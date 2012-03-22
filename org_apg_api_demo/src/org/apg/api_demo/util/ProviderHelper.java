package org.apg.api_demo.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

public class ProviderHelper {

    /**
     * Get secret key ids based on a given email.
     * 
     * @param context
     * @param email
     *            The email in question.
     * @return key ids
     */
    public long[] getSecretKeyIdsFromEmail(Context context, String email) {
        long ids[] = null;
        try {
            Uri contentUri = Uri.withAppendedPath(Constants.CONTENT_URI_SECRET_KEY_RING_BY_EMAILS,
                    email);
            Cursor c = context.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                ids = new long[c.getCount()];
                while (c.moveToNext()) {
                    ids[c.getPosition()] = c.getLong(0);
                }
            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissionsInfo(context);
        }

        return ids;
    }

    /**
     * Get public key ids based on a given email.
     * 
     * @param context
     * @param email
     *            The email in question.
     * @return key ids
     */
    public long[] getPublicKeyIdsFromEmail(Context context, String email) {
        long ids[] = null;
        try {
            Uri contentUri = Uri.withAppendedPath(Constants.CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS,
                    email);
            Cursor c = context.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                ids = new long[c.getCount()];
                while (c.moveToNext()) {
                    ids[c.getPosition()] = c.getLong(0);
                }
            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissionsInfo(context);
        }

        return ids;
    }

    /**
     * Find out if a given email has a secret key.
     * 
     * @param context
     * @param email
     *            The email in question.
     * @return true if there is a secret key for this email.
     */
    public boolean hasSecretKeyForEmail(Context context, String email) {
        try {
            Uri contentUri = Uri.withAppendedPath(Constants.CONTENT_URI_SECRET_KEY_RING_BY_EMAILS,
                    email);
            Cursor c = context.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.close();
                return true;
            }
            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissionsInfo(context);
        }
        return false;
    }

    /**
     * Find out if a given email has a public key.
     * 
     * @param context
     * @param email
     *            The email in question.
     * @return true if there is a public key for this email.
     */
    public boolean hasPublicKeyForEmail(Context context, String email) {
        try {
            Uri contentUri = Uri.withAppendedPath(Constants.CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS,
                    email);
            Cursor c = context.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.close();
                return true;
            }
            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissionsInfo(context);
        }
        return false;
    }

    /**
     * Get the user id based on the key id.
     * 
     * @param context
     * @param keyId
     * @return user id
     */
    public String getUserId(Context context, long keyId) {
        String userId = null;
        try {
            Uri contentUri = ContentUris.withAppendedId(
                    Constants.CONTENT_URI_SECRET_KEY_RING_BY_KEY_ID, keyId);
            Cursor c = context.getContentResolver().query(contentUri, new String[] { "user_id" },
                    null, null, null);
            if (c != null && c.moveToFirst()) {
                userId = c.getString(0);
            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissionsInfo(context);
        }

        if (userId == null) {
            userId = "unknown";
        }
        return userId;
    }

    private void insufficientPermissionsInfo(Context context) {
        Toast.makeText(context, "Permission to access APG Provider is missing!", Toast.LENGTH_LONG)
                .show();
    }
}

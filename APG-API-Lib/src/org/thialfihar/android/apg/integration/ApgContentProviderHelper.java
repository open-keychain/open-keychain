/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2011 K-9 Mail Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg.integration;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

public class ApgContentProviderHelper {
    public static final String AUTHORITY = Constants.APG_PACKAGE_NAME;

    public static final Uri CONTENT_URI_PUBLIC_KEY_RING_BY_KEY_ID = Uri.parse("content://"
            + AUTHORITY + "/key_rings/public/key_id/");
    public static final Uri CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS = Uri.parse("content://"
            + AUTHORITY + "/key_rings/public/emails/");

    public static final Uri CONTENT_URI_SECRET_KEY_RING_BY_KEY_ID = Uri.parse("content://"
            + AUTHORITY + "/key_rings/secret/key_id/");
    public static final Uri CONTENT_URI_SECRET_KEY_RING_BY_EMAILS = Uri.parse("content://"
            + AUTHORITY + "/key_rings/secret/emails/");

    private Context activity;

    public ApgContentProviderHelper(Activity activity) {
        this.activity = activity;
    }

    /**
     * Get secret key ids based on a given email.
     * 
     * @param context
     * @param email
     *            The email in question.
     * @return key ids
     */
    public long[] getSecretKeyIdsFromEmail(String email) {
        long ids[] = null;
        try {
            Uri contentUri = Uri.withAppendedPath(CONTENT_URI_SECRET_KEY_RING_BY_EMAILS, email);
            Cursor c = activity.getContentResolver().query(contentUri,
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
            insufficientPermissions();
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
    public long[] getPublicKeyIdsFromEmail(String email) {
        long ids[] = null;
        try {
            Uri contentUri = Uri.withAppendedPath(CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS, email);
            Cursor c = activity.getContentResolver().query(contentUri,
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
            insufficientPermissions();
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
    public boolean hasSecretKeyForEmail(String email) {
        try {
            Uri contentUri = Uri.withAppendedPath(CONTENT_URI_SECRET_KEY_RING_BY_EMAILS, email);
            Cursor c = activity.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.close();
                return true;
            }
            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissions();
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
    public boolean hasPublicKeyForEmail(String email) {
        try {
            Uri contentUri = Uri.withAppendedPath(CONTENT_URI_PUBLIC_KEY_RING_BY_EMAILS, email);
            Cursor c = activity.getContentResolver().query(contentUri,
                    new String[] { "master_key_id" }, null, null, null);
            if (c != null && c.getCount() > 0) {
                c.close();
                return true;
            }
            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissions();
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
    public String getUserId(long keyId) {
        String userId = null;
        try {
            Uri contentUri = ContentUris.withAppendedId(CONTENT_URI_SECRET_KEY_RING_BY_KEY_ID,
                    keyId);
            Cursor c = activity.getContentResolver().query(contentUri, new String[] { "user_id" },
                    null, null, null);
            if (c != null && c.moveToFirst()) {
                userId = c.getString(0);
            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            insufficientPermissions();
        }

        if (userId == null) {
            userId = "unknown";
        }
        return userId;
    }

    private void insufficientPermissions() {
        Toast.makeText(activity, "Permission to access APG Provider is missing!", Toast.LENGTH_LONG)
                .show();
    }
}

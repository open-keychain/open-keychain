/*
 * Copyright (C) 2014-2017 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.intents;

public class OpenKeychainIntents {
    private static final String PACKAGE_NAME = "org.sufficientlysecure.keychain";

    // prefix packagename for exported Intents
    // as described in http://developer.android.com/guide/components/intents-filters.html
    private static final String INTENT_PREFIX = PACKAGE_NAME + ".action.";
    private static final String EXTRA_PREFIX = PACKAGE_NAME + ".";

    public static final String ENCRYPT_TEXT = INTENT_PREFIX + "ENCRYPT_TEXT";
    public static final String ENCRYPT_EXTRA_TEXT = EXTRA_PREFIX + "EXTRA_TEXT"; // String

    public static final String ENCRYPT_DATA = INTENT_PREFIX + "ENCRYPT_DATA";
    public static final String ENCRYPT_EXTRA_ASCII_ARMOR = EXTRA_PREFIX + "EXTRA_ASCII_ARMOR"; // boolean

    public static final String DECRYPT_DATA = INTENT_PREFIX + "DECRYPT_DATA";

    public static final String IMPORT_KEY = INTENT_PREFIX + "IMPORT_KEY";
    public static final String IMPORT_EXTRA_KEY_EXTRA_KEY_BYTES = EXTRA_PREFIX + "EXTRA_KEY_BYTES"; // byte[]

    public static final String IMPORT_KEY_FROM_KEYSERVER = INTENT_PREFIX + "IMPORT_KEY_FROM_KEYSERVER";
    public static final String IMPORT_KEY_FROM_KEYSERVER_EXTRA_QUERY = EXTRA_PREFIX + "EXTRA_QUERY"; // String
    public static final String IMPORT_KEY_FROM_KEYSERVER_EXTRA_FINGERPRINT = EXTRA_PREFIX + "EXTRA_FINGERPRINT"; // String

    public static final String IMPORT_KEY_FROM_QR_CODE = INTENT_PREFIX + "IMPORT_KEY_FROM_QR_CODE";

}

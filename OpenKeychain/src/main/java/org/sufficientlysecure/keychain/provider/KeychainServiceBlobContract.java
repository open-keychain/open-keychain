/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.net.Uri;
import android.provider.BaseColumns;
import org.sufficientlysecure.keychain.Constants;

public class KeychainServiceBlobContract {

    interface BlobsColumns {
        String KEY = "key";
    }

    public static final String CONTENT_AUTHORITY = Constants.PACKAGE_NAME + ".blobs";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static class Blobs implements BlobsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI;
    }

    private KeychainServiceBlobContract() {
    }
}

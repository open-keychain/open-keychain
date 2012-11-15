/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.thialfihar.android.apg.provider;

import org.thialfihar.android.apg.Constants;

import android.net.Uri;
import android.provider.BaseColumns;

public class ApgServiceBlobContract {

    interface BlobsColumns {
        String KEY = "key";
    }

    public static final String CONTENT_AUTHORITY = Constants.PACKAGE_NAME + ".blobs";

    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    public static class Blobs implements BlobsColumns, BaseColumns {
        public static final Uri CONTENT_URI = BASE_CONTENT_URI;
    }

    private ApgServiceBlobContract() {
    }
}

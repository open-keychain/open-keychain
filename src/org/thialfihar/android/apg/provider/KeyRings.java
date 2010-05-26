/*
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
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

import android.net.Uri;
import android.provider.BaseColumns;

public class KeyRings implements BaseColumns {
    public static final String TABLE_NAME = "key_rings";

    public static final String _ID_type = "INTEGER PRIMARY KEY";
    public static final String MASTER_KEY_ID = "c_master_key_id";
    public static final String MASTER_KEY_ID_type = "INT64";
    public static final String TYPE = "c_type";
    public static final String TYPE_type = "INTEGER";
    public static final String WHO_ID = "c_who_id";
    public static final String WHO_ID_type = "INTEGER";
    public static final String KEY_RING_DATA = "c_key_ring_data";
    public static final String KEY_RING_DATA_type = "BLOB";

    public static final Uri CONTENT_URI =
            Uri.parse("content://" + DataProvider.AUTHORITY + "/key_ring");
    public static final Uri CONTENT_URI_BY_KEY_ID =
            Uri.parse("content://" + DataProvider.AUTHORITY + "/key_ring/key_id");
    public static final String CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.thialfihar.apg.key_ring";
    public static final String CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.thialfihar.apg.key_ring";
    public static final String DEFAULT_SORT_ORDER = _ID + " DESC";
}

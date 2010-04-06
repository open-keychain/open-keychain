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

class Accounts1 implements BaseColumns {
    public static final String TABLE_NAME = "accounts";

    public static final String _ID_type = "INTEGER PRIMARY KEY";
    public static final String NAME = "c_name";
    public static final String NAME_type = "TEXT";

    public static final Uri CONTENT_URI =
            Uri.parse("content://" + DataProvider.AUTHORITY + "/accounts");
    public static final String CONTENT_TYPE =
            "vnd.android.cursor.dir/vnd.thialfihar.apg.account";
    public static final String CONTENT_ITEM_TYPE =
            "vnd.android.cursor.item/vnd.thialfihar.apg.account";
    public static final String DEFAULT_SORT_ORDER = _ID + " DESC";
}
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

import android.provider.BaseColumns;

public class Keys implements BaseColumns {
    public static final String TABLE_NAME = "keys";

    public static final String _ID_type = "INTEGER PRIMARY KEY";
    public static final String KEY_ID = "c_key_id";
    public static final String KEY_ID_type = "INT64";
    public static final String TYPE = "c_type";
    public static final String TYPE_type = "INTEGER";
    public static final String IS_MASTER_KEY = "c_is_master_key";
    public static final String IS_MASTER_KEY_type = "INTEGER";
    public static final String ALGORITHM = "c_algorithm";
    public static final String ALGORITHM_type = "INTEGER";
    public static final String KEY_SIZE = "c_key_size";
    public static final String KEY_SIZE_type = "INTEGER";
    public static final String CAN_SIGN = "c_can_sign";
    public static final String CAN_SIGN_type = "INTEGER";
    public static final String CAN_ENCRYPT = "c_can_encrypt";
    public static final String CAN_ENCRYPT_type = "INTEGER";
    public static final String IS_REVOKED = "c_is_revoked";
    public static final String IS_REVOKED_type = "INTEGER";
    public static final String CREATION = "c_creation";
    public static final String CREATION_type = "INTEGER";
    public static final String EXPIRY = "c_expiry";
    public static final String EXPIRY_type = "INTEGER";
    public static final String KEY_RING_ID = "c_key_ring_id";
    public static final String KEY_RING_ID_type = "INTEGER";
    public static final String KEY_DATA = "c_key_data";
    public static final String KEY_DATA_type = "BLOB";
    public static final String RANK = "c_key_data";
    public static final String RANK_type = "INTEGER";
}

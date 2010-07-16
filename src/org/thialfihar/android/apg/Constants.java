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

package org.thialfihar.android.apg;

import android.os.Environment;

public final class Constants {
    public static final class path {
        public static final String app_dir = Environment.getExternalStorageDirectory() + "/APG";
    }

    public static final class pref {
        public static final String has_seen_change_log = "seenChangeLogDialog";
        public static final String default_encryption_algorithm = "defaultEncryptionAlgorithm";
        public static final String default_hash_algorithm = "defaultHashAlgorithm";
        public static final String default_ascii_armour = "defaultAsciiArmour";
        public static final String default_message_compression = "defaultMessageCompression";
        public static final String default_file_compression = "defaultFileCompression";
        public static final String pass_phrase_cache_ttl = "passPhraseCacheTtl";
    }
}

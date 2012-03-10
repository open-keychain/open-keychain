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

package org.apg;

import android.os.Environment;

public final class Constants {

    public static final String TAG = "APG";

    public static final class path {
        public static final String APP_DIR = Environment.getExternalStorageDirectory() + "/APG";
    }

    public static final class pref {
        public static final String HAS_SEEN_HELP = "seenHelp";
        public static final String HAS_SEEN_CHANGE_LOG = "seenChangeLogDialog";
        public static final String DEFAULT_ENCRYPTION_ALGORITHM = "defaultEncryptionAlgorithm";
        public static final String DEFAULT_HASH_ALGORITHM = "defaultHashAlgorithm";
        public static final String DEFAULT_ASCII_ARMOUR = "defaultAsciiArmour";
        public static final String DEFAULT_MESSAGE_COMPRESSION = "defaultMessageCompression";
        public static final String DEFAULT_FILE_COMPRESSION = "defaultFileCompression";
        public static final String PASS_PHRASE_CACHE_TTL = "passPhraseCacheTtl";
        public static final String LANGUAGE = "language";
        public static final String FORCE_V3_SIGNATURES = "forceV3Signatures";
        public static final String KEY_SERVERS = "keyServers";
    }

    public static final class defaults {
        public static final String KEY_SERVERS = "pool.sks-keyservers.net, subkeys.pgp.net, pgp.mit.edu";
    }

    public static final class extras {
        public static final String PROGRESS = "progress";
        public static final String PROGRESS_MAX = "max";
        public static final String STATUS = "status";
        public static final String MESSAGE = "message";
        public static final String KEY_ID = "keyId";
    }
}

/*
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain;

import org.spongycastle.bcpg.CompressionAlgorithmTags;

/**
 * TODO:
 * <p/>
 * - refactor ids, some are not needed and can be done with xml
 */
public final class Id {


    public static final class type {
        public static final int public_key = 0x21070001;
        public static final int secret_key = 0x21070002;
        public static final int user_id = 0x21070003;
        public static final int key = 0x21070004;
    }

    public static final class choice {
        public static final class algorithm {
            public static final int dsa = 0x21070001;
            public static final int elgamal = 0x21070002;
            public static final int rsa = 0x21070003;
        }

        public static final class compression {
            public static final int none = 0x21070001;
            public static final int zlib = CompressionAlgorithmTags.ZLIB;
            public static final int bzip2 = CompressionAlgorithmTags.BZIP2;
            public static final int zip = CompressionAlgorithmTags.ZIP;
        }
    }

    public static final class return_value {
        public static final int ok = 0;
        public static final int error = -1;
        public static final int no_master_key = -2;
        public static final int updated = 1;
        public static final int bad = -3;
    }

    public static final class key {
        public static final int none = 0;
        public static final int symmetric = -1;
    }

}

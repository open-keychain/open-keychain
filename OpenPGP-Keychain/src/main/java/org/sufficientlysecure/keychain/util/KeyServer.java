/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 Thialfihar <thi@thialfihar.org>
 * Copyright (C) 2011 Senecaso
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

package org.sufficientlysecure.keychain.util;

import java.util.List;

import org.sufficientlysecure.keychain.ui.adapter.ImportKeysListEntry;

public abstract class KeyServer {
    static public class QueryException extends Exception {
        private static final long serialVersionUID = 2703768928624654512L;

        public QueryException(String message) {
            super(message);
        }
    }

    static public class TooManyResponses extends Exception {
        private static final long serialVersionUID = 2703768928624654513L;
    }

    static public class InsufficientQuery extends Exception {
        private static final long serialVersionUID = 2703768928624654514L;
    }

    static public class AddKeyException extends Exception {
        private static final long serialVersionUID = -507574859137295530L;
    }

    abstract List<ImportKeysListEntry> search(String query) throws QueryException, TooManyResponses,
            InsufficientQuery;

    abstract String get(long keyId) throws QueryException;

    abstract void add(String armouredText) throws AddKeyException;
}

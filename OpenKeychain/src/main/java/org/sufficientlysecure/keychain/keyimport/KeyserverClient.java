/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.keyimport;

import org.sufficientlysecure.keychain.util.ParcelableProxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface KeyserverClient {

    class CloudSearchFailureException extends Exception {
        private static final long serialVersionUID = 2703768928624654515L;

        public CloudSearchFailureException(String message) {
            super(message);
        }

        public CloudSearchFailureException() {
            super();
        }
    }

    class QueryFailedException extends CloudSearchFailureException {
        private static final long serialVersionUID = 2703768928624654512L;

        public QueryFailedException(String message) {
            super(message);
        }
    }

    class QueryNotFoundException extends QueryFailedException {
        private static final long serialVersionUID = 2693768928624654513L;

        public QueryNotFoundException(String message) {
            super(message);
        }
    }

    class QueryNeedsRepairException extends CloudSearchFailureException {
        private static final long serialVersionUID = 2693768928624654512L;
    }

    class TooManyResponsesException extends QueryNeedsRepairException {
        private static final long serialVersionUID = 2703768928624654513L;
    }

    class QueryTooShortException extends QueryNeedsRepairException {
        private static final long serialVersionUID = 2703768928624654514L;
    }

    /**
     * query too short _or_ too many responses
     */
    class QueryTooShortOrTooManyResponsesException extends QueryNeedsRepairException {
        private static final long serialVersionUID = 2703768928624654518L;
    }

    class QueryNoEnabledSourceException extends QueryNeedsRepairException {
        private static final long serialVersionUID = 2703768928624654519L;
    }

    class AddKeyException extends Exception {
        private static final long serialVersionUID = -507574859137295530L;
    }

    List<ImportKeysListEntry> search(String query, ParcelableProxy proxy)
            throws QueryFailedException, QueryNeedsRepairException;

    String get(String keyIdHex, ParcelableProxy proxy) throws QueryFailedException;

    void add(String armoredKey, ParcelableProxy proxy) throws AddKeyException;
}

/*
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

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

    static public class KeyInfo implements Serializable, Parcelable {
        private static final long serialVersionUID = -7797972113284992662L;
        public ArrayList<String> userIds;
        public String revoked;
        public Date date;
        public String fingerPrint;
        public long keyId;
        public int size;
        public String algorithm;

        public KeyInfo() {
            userIds = new ArrayList<String>();
        }

        public KeyInfo(Parcel in) {
            this();

            in.readStringList(this.userIds);
            this.revoked = in.readString();
            this.date = (Date) in.readSerializable();
            this.fingerPrint = in.readString();
            this.keyId = in.readLong();
            this.size = in.readInt();
            this.algorithm = in.readString();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeStringList(userIds);
            dest.writeString(revoked);
            dest.writeSerializable(date);
            dest.writeString(fingerPrint);
            dest.writeLong(keyId);
            dest.writeInt(size);
            dest.writeString(algorithm);
        }
    }

    abstract List<KeyInfo> search(String query) throws QueryException, TooManyResponses,
            InsufficientQuery;

    abstract String get(long keyId) throws QueryException;

    abstract void add(String armouredText) throws AddKeyException;
}

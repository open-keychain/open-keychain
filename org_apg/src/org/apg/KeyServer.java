package org.apg;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Vector;

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

    static public class KeyInfo implements Serializable {
        private static final long serialVersionUID = -7797972113284992662L;
        public Vector<String> userIds;
        public String revoked;
        public Date date;
        public String fingerPrint;
        public long keyId;
        public int size;
        public String algorithm;
    }

    abstract List<KeyInfo> search(String query) throws QueryException, TooManyResponses,
            InsufficientQuery;

    abstract String get(long keyId) throws QueryException;

    abstract void add(String armouredText) throws AddKeyException;
}

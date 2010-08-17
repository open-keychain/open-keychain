package org.thialfihar.android.apg;

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
    static public class KeyInfo implements Serializable {
        private static final long serialVersionUID = -7797972113284992662L;
        Vector<String> userIds;
        String revoked;
        Date date;
        String fingerPrint;
        long keyId;
        int size;
        String algorithm;
    }
    abstract List<KeyInfo> search(String query) throws QueryException, TooManyResponses, InsufficientQuery;
    abstract String get(long keyId) throws QueryException;
}

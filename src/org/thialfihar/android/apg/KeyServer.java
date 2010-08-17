package org.thialfihar.android.apg;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.List;
import java.util.Vector;

public abstract class KeyServer {
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
    abstract List<KeyInfo> search(String query) throws MalformedURLException, IOException;
    abstract String get(long keyId) throws MalformedURLException, IOException;
}

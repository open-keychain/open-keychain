package org.bouncycastle.bcpg;

import java.io.OutputStream;
import java.util.Hashtable;

public class ArmoredWithoutVersionOutputStream extends ArmoredOutputStream {

    public ArmoredWithoutVersionOutputStream(OutputStream out) {
        super(out);
        setHeader(ArmoredOutputStream.VERSION_HDR, null);
    }

    public ArmoredWithoutVersionOutputStream(OutputStream out, Hashtable headers) {
        super(out, headers);
        setHeader(ArmoredOutputStream.VERSION_HDR, null);
    }
}

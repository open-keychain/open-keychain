package org.bouncycastle.openpgp.jcajce;


import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.openpgp.PGPMarker;

/** This class wraps the regular PGPObjectFactory, changing its behavior to
 * ignore all PGPMarker packets it encounters while reading. These packets
 * carry no semantics of their own, and should be ignored according to
 * RFC 4880.
 * 
 * @see https://tools.ietf.org/html/rfc4880#section-5.8
 * @see org.bouncycastle.openpgp.PGPMarker
 * 
 */
public class JcaSkipMarkerPGPObjectFactory extends JcaPGPObjectFactory {

    public JcaSkipMarkerPGPObjectFactory(InputStream in) {
        super(in);
    }

    @Override
    public Object nextObject() throws IOException {
        Object o = super.nextObject();
        while (o instanceof PGPMarker) {
            o = super.nextObject();
        }
        return o;
    }
}

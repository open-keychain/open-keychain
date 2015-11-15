package org.spongycastle.openpgp.jcajce;


import java.io.IOException;
import java.io.InputStream;

import org.spongycastle.openpgp.PGPMarker;


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

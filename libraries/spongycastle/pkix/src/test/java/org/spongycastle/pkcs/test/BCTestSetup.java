// Copyright (c) 2005 The Legion Of The Bouncy Castle (http://www.bouncycastle.org)
package org.spongycastle.pkcs.test;

import java.security.Security;

import junit.extensions.TestSetup;
import junit.framework.Test;

class BCTestSetup
    extends TestSetup
{
    public BCTestSetup(Test test)
    {
        super(test);
    }

    protected void setUp()
    {
        Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider());
    }

    protected void tearDown()
    {
        Security.removeProvider("SC");
    }
}

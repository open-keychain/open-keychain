package org.spongycastle.crypto.tls.test;

import java.io.IOException;

import org.spongycastle.crypto.tls.DefaultTlsClient;
import org.spongycastle.crypto.tls.TlsAuthentication;

public class TestTlsClient
    extends DefaultTlsClient
{
    private final TlsAuthentication authentication;

    TestTlsClient(TlsAuthentication authentication)
    {
        this.authentication = authentication;
    }

    public TlsAuthentication getAuthentication()
        throws IOException
    {
        return authentication;
    }
}

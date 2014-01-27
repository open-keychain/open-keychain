package org.spongycastle.crypto.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import org.spongycastle.crypto.test.cavp.CAVPReader;
import org.spongycastle.crypto.test.cavp.KDFCounterTests;
import org.spongycastle.util.test.SimpleTest;

public class KDFCounterGeneratorTest
    extends SimpleTest
{

    private static void testCounter()
    {

        CAVPReader cavpReader = new CAVPReader(new KDFCounterTests());

        final InputStream stream = CAVPReader.class.getResourceAsStream("KDFCTR_gen.rsp");
        final Reader reader = new InputStreamReader(stream, Charset.forName("UTF-8"));
        cavpReader.setInput("KDFCounter", reader);

        try
        {
            cavpReader.readAll();
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Something is rotten in the state of Denmark", e);
        }
    }

    public String getName()
    {
        return this.getClass().getSimpleName();
    }

    public void performTest()
        throws Exception
    {
        testCounter();
    }

    public static void main(String[] args)
    {
        runTest(new KDFCounterGeneratorTest());
    }
}

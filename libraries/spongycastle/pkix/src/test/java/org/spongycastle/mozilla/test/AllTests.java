package org.spongycastle.mozilla.test;

import java.security.Security;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.util.test.SimpleTestResult;

import junit.framework.*;

public class AllTests
    extends TestCase
{
    public void testMozilla()
    {   
        Security.addProvider(new BouncyCastleProvider());
        
        org.spongycastle.util.test.Test[] tests = new org.spongycastle.util.test.Test[] { new SPKACTest() };
        
        for (int i = 0; i != tests.length; i++)
        {
            SimpleTestResult  result = (SimpleTestResult)tests[i].perform();
            
            if (!result.isSuccessful())
            {
                fail(result.toString());
            }
        }
    }
    
    public static void main (String[] args)
    {
        junit.textui.TestRunner.run(suite());
    }
    
    public static Test suite()
    {
        TestSuite suite = new TestSuite("Mozilla Tests");
        
        suite.addTestSuite(AllTests.class);
        
        return suite;
    }
}

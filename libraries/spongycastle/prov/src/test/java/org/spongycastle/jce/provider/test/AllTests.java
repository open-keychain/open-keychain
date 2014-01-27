package org.spongycastle.jce.provider.test;

import java.security.Security;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.spongycastle.jce.provider.test.rsa3.RSA3CertTest;
import org.spongycastle.util.test.SimpleTestResult;

public class AllTests
    extends TestCase
{
    public void testJCE()
    {   
        org.spongycastle.util.test.Test[] tests = RegressionTest.tests;
        
        for (int i = 0; i != tests.length; i++)
        {
            SimpleTestResult  result = (SimpleTestResult)tests[i].perform();
            
            if (!result.isSuccessful())
            {
                if (result.getException() != null)
                {
                    result.getException().printStackTrace();
                }
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
        TestSuite suite = new TestSuite("JCE Tests");
        
        if (Security.getProvider("SC") == null)
        {
            Security.addProvider(new BouncyCastleProvider());  
        }
        
        suite.addTestSuite(RSA3CertTest.class);
        suite.addTestSuite(AllTests.class);
        
        return suite;
    }
}

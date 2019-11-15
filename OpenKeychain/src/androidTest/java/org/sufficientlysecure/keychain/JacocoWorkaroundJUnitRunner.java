package org.sufficientlysecure.keychain;


import java.lang.reflect.Method;

import android.os.Bundle;
import androidx.test.runner.AndroidJUnitRunner;


public class JacocoWorkaroundJUnitRunner extends AndroidJUnitRunner {
    static {
            System.setProperty("jacoco-agent.destfile", "/data/data/"
                    + BuildConfig.APPLICATION_ID + "/coverage.ec");
    }

    @Override
    public void finish(int resultCode, Bundle results) {
        try {
            Class rt = Class.forName("org.jacoco.agent.rt.RT");
            Method getAgent = rt.getMethod("getAgent");
            Method dump = getAgent.getReturnType().getMethod("dump", boolean.class);
            Object agent = getAgent.invoke(null);
            dump.invoke(agent, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.finish(resultCode, results);
    }
}
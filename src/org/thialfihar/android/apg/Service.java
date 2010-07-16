package org.thialfihar.android.apg;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;

public class Service extends android.app.Service {
    private final IBinder mBinder = new LocalBinder();

    public static final String EXTRA_TTL = "ttl";

    private int mPassPhraseCacheTtl = 15;
    private Handler mCacheHandler = new Handler();
    private Runnable mCacheTask = new Runnable() {
        public void run() {
            // check every ttl/2 seconds, which shouldn't be heavy on the device (even if ttl = 15),
            // and makes sure the longest a pass phrase survives in the cache is 1.5 * ttl
            int delay = mPassPhraseCacheTtl * 1000 / 2;
            // also make sure the delay is not longer than one minute
            if (delay > 60000) {
                delay = 60000;
            }

            delay = Apg.cleanUpCache(mPassPhraseCacheTtl, delay);
            // don't check too often, even if we were close
            if (delay < 5000) {
                delay = 5000;
            }

            mCacheHandler.postDelayed(this, delay);
        }
    };

    static private boolean mIsRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();

        mIsRunning = true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mIsRunning = false;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        if (intent != null) {
            mPassPhraseCacheTtl = intent.getIntExtra(EXTRA_TTL, 15);
        }
        if (mPassPhraseCacheTtl < 15) {
            mPassPhraseCacheTtl = 15;
        }
        mCacheHandler.removeCallbacks(mCacheTask);
        mCacheHandler.postDelayed(mCacheTask, 1000);
    }

    static public boolean isRunning() {
        return mIsRunning;
    }

    public class LocalBinder extends Binder {
        Service getService() {
            return Service.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
}

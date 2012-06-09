package org.thialfihar.android.apg;

import org.thialfihar.android.apg.passphrase.PassphraseCacheService;

import android.app.Application;
import android.content.Intent;

public class ApgApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        /** Start passphrase cache service */
        PassphraseCacheService.startCacheService(this);
    }

}

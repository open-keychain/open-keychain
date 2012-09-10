/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.thialfihar.android.apg;

import java.security.Security;

import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.thialfihar.android.apg.helper.PGPMain;
import org.thialfihar.android.apg.service.PassphraseCacheService;

import android.app.Application;

public class ApgApplication extends Application {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        /* Start passphrase cache service */
        PassphraseCacheService.startCacheService(this);

        // TODO: Do it better than this!
        // this initializes the database to be used in PGPMain
        PGPMain.initialize(this);
    }

}

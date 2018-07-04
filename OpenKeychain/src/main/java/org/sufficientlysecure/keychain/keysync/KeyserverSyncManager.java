/*
 * Copyright (C) 2017 Schürmann & Breitmoser GbR
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.sufficientlysecure.keychain.keysync;


import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;

import androidx.work.Constraints.Builder;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public class KeyserverSyncManager {
    private static final long SYNC_INTERVAL = 3;
    private static final TimeUnit SYNC_INTERVAL_UNIT = TimeUnit.DAYS;

    private static final String PERIODIC_WORK_TAG = "keyserverSync";

    public static void updateKeyserverSyncSchedule(Context context, boolean forceReschedule) {
        Preferences prefs = Preferences.getPreferences(context);
        if (!forceReschedule && prefs.isKeyserverSyncScheduled() != prefs.isKeyserverSyncEnabled()) {
            return;
        }
        WorkManager workManager = WorkManager.getInstance();
        if (workManager == null) {
            Timber.e("WorkManager unavailable!");
            return;
        }
        workManager.cancelAllWorkByTag(PERIODIC_WORK_TAG);

        if (!prefs.isKeyserverSyncEnabled()) {
            return;
        }

        /* Periodic syncs can't be unique, so we just use this to launch a uniquely queued worker */

        Builder constraints = new Builder()
                .setRequiredNetworkType(prefs.getWifiOnlySync() ? NetworkType.UNMETERED : NetworkType.CONNECTED)
                .setRequiresBatteryNotLow(true);
        if (VERSION.SDK_INT >= VERSION_CODES.M) {
            constraints.setRequiresDeviceIdle(true);
        }

        PeriodicWorkRequest workRequest =
                new PeriodicWorkRequest.Builder(KeyserverSyncWorker.class, SYNC_INTERVAL, SYNC_INTERVAL_UNIT)
                        .setConstraints(constraints.build())
                        .addTag(PERIODIC_WORK_TAG)
                        .build();
        workManager.enqueue(workRequest);

        prefs.setKeyserverSyncScheduled(true);
    }

    public static void debugRunSyncNow() {
        WorkManager workManager = WorkManager.getInstance();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(KeyserverSyncWorker.class).build();
        workManager.enqueue(workRequest);
    }
}

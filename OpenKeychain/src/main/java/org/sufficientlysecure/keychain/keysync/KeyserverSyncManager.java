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


import java.util.List;
import java.util.concurrent.TimeUnit;

import android.arch.lifecycle.LiveData;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;

import androidx.work.Constraints.Builder;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;
import androidx.work.Worker;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public class KeyserverSyncManager {
    private static final long SYNC_INTERVAL = 3;
    private static final TimeUnit SYNC_INTERVAL_UNIT = TimeUnit.DAYS;

    private static final String PERIODIC_WORK_TAG = "keyserverSync";
    private static final String UNIQUE_WORK_NAME = "keySync";

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
                new PeriodicWorkRequest.Builder(KeyserverSyncLauncherWorker.class, SYNC_INTERVAL, SYNC_INTERVAL_UNIT)
                        .setConstraints(constraints.build())
                        .addTag(PERIODIC_WORK_TAG)
                        .build();
        workManager.enqueue(workRequest);

        prefs.setKeyserverSyncScheduled(true);
    }

    public static class KeyserverSyncLauncherWorker extends Worker {
        @NonNull
        @Override
        public WorkerResult doWork() {
            runSyncNow(false, false);
            return WorkerResult.SUCCESS;
        }
    }

    public static void runSyncNow(boolean isForeground, boolean isForceUpdate) {
        WorkManager workManager = WorkManager.getInstance();
        if (workManager == null) {
            Timber.e("WorkManager unavailable!");
            return;
        }

        Data workData = new Data.Builder()
                .putBoolean(KeyserverSyncWorker.DATA_IS_FOREGROUND, isForeground)
                .putBoolean(KeyserverSyncWorker.DATA_IS_FORCE, isForceUpdate)
                .build();

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(KeyserverSyncWorker.class)
                .setInputData(workData)
                .build();
        workManager.beginUniqueWork(UNIQUE_WORK_NAME,
                isForeground ? ExistingWorkPolicy.REPLACE : ExistingWorkPolicy.KEEP, workRequest).enqueue();
     }

     public static LiveData<List<WorkStatus>> getSyncWorkerLiveData() {
         WorkManager workManager = WorkManager.getInstance();
         return workManager.getStatusesForUniqueWork(UNIQUE_WORK_NAME);
     }
}

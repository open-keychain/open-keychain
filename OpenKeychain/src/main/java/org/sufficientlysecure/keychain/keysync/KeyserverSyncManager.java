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


import java.util.UUID;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.support.annotation.WorkerThread;

import androidx.work.Constraints.Builder;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.State;
import androidx.work.SynchronousWorkManager;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;
import org.sufficientlysecure.keychain.util.Preferences;
import timber.log.Timber;


public class KeyserverSyncManager {
    private static final long SYNC_INTERVAL = 3;
    private static final TimeUnit SYNC_INTERVAL_UNIT = TimeUnit.DAYS;

    private static final String PERIODIC_WORK_TAG = "keyserverSync";

    public static void updateKeyserverSyncScheduleAsync(Context context, boolean forceReschedule) {
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                updateKeyserverSyncSchedule(context, forceReschedule);
                return null;
            }
        }.execute();
    }

    @WorkerThread
    private static void updateKeyserverSyncSchedule(Context context, boolean forceReschedule) {
        Preferences prefs = Preferences.getPreferences(context);
        WorkManager workManager = WorkManager.getInstance();
        if (workManager == null) {
            Timber.e("WorkManager unavailable!");
            return;
        }
        SynchronousWorkManager synchronousWorkManager = workManager.synchronous();
        if (synchronousWorkManager == null) {
            Timber.e("WorkManager unavailable!");
            return;
        }

        UUID workUuid = prefs.getKeyserverSyncWorkUuid();
        WorkStatus status = workUuid != null ? synchronousWorkManager.getStatusByIdSync(workUuid) : null;
        boolean workIsScheduled = status != null && status.getState() != State.CANCELLED;
        if (workIsScheduled == prefs.isKeyserverSyncEnabled()) {
            if (!forceReschedule) {
                Timber.d("Key sync already scheduled, no changes necessary");
                return;
            }
            Timber.d("Key sync already scheduled, but forcing reschedule");
        }

        Timber.d("Cancelling sync tasks…");
        synchronousWorkManager.cancelAllWorkByTagSync(PERIODIC_WORK_TAG);

        if (!prefs.isKeyserverSyncEnabled()) {
            Timber.d("Key sync disabled");
            return;
        }

        Timber.d("Scheduling periodic key sync");

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
        synchronousWorkManager.enqueueSync(workRequest);

        Timber.d("Work id: %s", workRequest.getId());
        prefs.setKeyserverSyncScheduled(workRequest.getId());
    }

    public static void debugRunSyncNow() {
        WorkManager workManager = WorkManager.getInstance();
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(KeyserverSyncWorker.class).build();
        workManager.enqueue(workRequest);
    }
}

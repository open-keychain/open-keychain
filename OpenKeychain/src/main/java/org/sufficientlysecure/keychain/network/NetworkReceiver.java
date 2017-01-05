/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.network;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.service.KeyserverSyncAdapterService;
import org.sufficientlysecure.keychain.util.Log;

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();
        boolean isTypeWifi = (networkInfo != null) &&
                (networkInfo.getType() == ConnectivityManager.TYPE_WIFI);
        boolean isConnected = (networkInfo != null) && networkInfo.isConnected();

        if (isTypeWifi && isConnected) {

            // broadcaster receiver disabled
            setWifiReceiverComponent(false, context);
            Intent serviceIntent = new Intent(context, KeyserverSyncAdapterService.class);
            serviceIntent.setAction(KeyserverSyncAdapterService.ACTION_SYNC_NOW);
            context.startService(serviceIntent);
        }
    }

    public void setWifiReceiverComponent(Boolean isEnabled, Context context) {

        PackageManager pm = context.getPackageManager();
        ComponentName compName = new ComponentName(context,
                NetworkReceiver.class);

        if (isEnabled) {
            pm.setComponentEnabledSetting(compName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            Log.d(Constants.TAG, "Wifi Receiver is enabled!");
        } else {
            pm.setComponentEnabledSetting(compName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            Log.d(Constants.TAG, "Wifi Receiver is disabled!");
        }
    }
}

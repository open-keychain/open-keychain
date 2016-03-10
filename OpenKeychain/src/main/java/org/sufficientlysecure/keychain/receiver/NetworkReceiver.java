package org.sufficientlysecure.keychain.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import org.sufficientlysecure.keychain.service.KeyserverSyncAdapterService;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

public class NetworkReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {

            // broadcaster receiver disabled
            setWifiReceiverComponent(false, context);
            Intent serviceIntent = new Intent(context, KeyserverSyncAdapterService.class);
            serviceIntent.setAction(KeyserverSyncAdapterService.ACTION_UPDATE_ALL);
        }
    }

    public void setWifiReceiverComponent(Boolean isEnabled, Context context){

        PackageManager pm = context.getPackageManager();
        ComponentName compName = new ComponentName(context,
                NetworkReceiver.class);

        if(isEnabled) {
            pm.setComponentEnabledSetting(compName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
            Log.d(Constants.TAG, "Wifi Receiver is enabled!");
        }
        else {
            pm.setComponentEnabledSetting(compName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            Log.d(Constants.TAG, "Wifi Receiver is disabled!");
        }
    }
}

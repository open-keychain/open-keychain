package org.sufficientlysecure.keychain.receiver;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.sufficientlysecure.keychain.service.KeyserverSyncAdapterService;
import org.sufficientlysecure.keychain.util.Preferences;

public class NetworkReceiver extends BroadcastReceiver {

    Context context;
    public NetworkReceiver(){}

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        KeyserverSyncAdapterService KSAS = new KeyserverSyncAdapterService(context);
        ConnectivityManager conn = (ConnectivityManager)
                context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = conn.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {

            // broadcaster receiver disabled
            setWifiReceiverComponent(false, context);
        }
    }

    public void setWifiReceiverComponent(Boolean isEnabled, Context context){

        PackageManager pm = context.getPackageManager();
        ComponentName compName = new ComponentName(context,
                NetworkReceiver.class);

        if(isEnabled) {
            Preferences.getPreferences(context).setAllowSync(false);
            pm.setComponentEnabledSetting(compName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        }
        else {
            pm.setComponentEnabledSetting(compName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            if(!Preferences.getPreferences(context).getAllowSync())
            KeyserverSyncAdapterService.enableKeyserverSync(context);
        }
    }
}
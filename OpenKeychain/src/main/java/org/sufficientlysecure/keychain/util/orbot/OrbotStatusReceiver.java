package org.sufficientlysecure.keychain.util.orbot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import org.sufficientlysecure.keychain.util.Log;

/**
 * Created by vanitas on 11.05.16.
 * BroadcastReceiver that receives Orbots status
 */
public class OrbotStatusReceiver extends BroadcastReceiver {

    private static final String TAG = "OrbStatRec";

    //TODO: These two Strings are missing in older versions of NetCipher.
    //TODO: Once they are present in OrbotHelper (not ProxyHelper) point to OrbotHelpers Strings instead.
    public final static String EXTRA_PROXY_PORT_HTTP = "org.torproject.android.intent.extra.HTTP_PROXY_PORT";
    public final static String EXTRA_PROXY_PORT_SOCKS = "org.torproject.android.intent.extra.SOCKS_PROXY_PORT";

    //Variables representing Orbots status
    private boolean torRunning;
    private int proxy_port_http;
    private int proxy_port_socks;

    private static OrbotStatusReceiver instance;

    public OrbotStatusReceiver() {
        instance = this;
    }

    public static OrbotStatusReceiver getInstance() {
        if(instance == null) instance = new OrbotStatusReceiver();
        return instance;
    }


    @Override
    public void onReceive(Context context, Intent intent) {
        if (OrbotHelper.ACTION_STATUS.equals(intent.getAction())) {
            Log.i(TAG, context.getPackageName() + " received intent : " + intent.getAction() + " " + intent.getPackage());
            String status = intent.getStringExtra(OrbotHelper.EXTRA_STATUS) + " (" + intent.getStringExtra(OrbotHelper.EXTRA_PACKAGE_NAME) + ")";
            this.torRunning = (intent.getStringExtra(OrbotHelper.EXTRA_STATUS).equals(OrbotHelper.STATUS_ON));

            Log.d(TAG, "Orbot status: "+status);
            if(torRunning){
                Bundle extras = intent.getExtras();

                if (extras.containsKey(EXTRA_PROXY_PORT_HTTP)) {
                    this.proxy_port_http = extras.getInt(EXTRA_PROXY_PORT_HTTP, -1);
                    Log.i(TAG, "Http proxy set to "+proxy_port_http);
                }

                if (extras.containsKey(EXTRA_PROXY_PORT_SOCKS)) {
                    this.proxy_port_socks = extras.getInt(EXTRA_PROXY_PORT_SOCKS, -1);
                    Log.i(TAG, "Socks proxy set to "+proxy_port_socks);
                }
            }
        }
    }

    public boolean isTorRunning(Context context) {
        OrbotHelper.requestStartTor(context);
        return this.torRunning;
    }

    public int getProxyPortHttp(Context context) {
        OrbotHelper.requestStartTor(context);
        return this.proxy_port_http;
    }

    public int getProxyPortSocks(Context context) {
        OrbotHelper.requestStartTor(context);
        return this.proxy_port_socks;
    }
}

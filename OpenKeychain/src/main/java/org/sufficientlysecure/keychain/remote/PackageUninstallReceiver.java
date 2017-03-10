package org.sufficientlysecure.keychain.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.sufficientlysecure.keychain.provider.KeychainContract;

public class PackageUninstallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_PACKAGE_FULLY_REMOVED.equals(intent.getAction())) {
            Uri uri = intent.getData();
            if (uri == null) {
                return;
            }
            String packageName = uri.getEncodedSchemeSpecificPart();
            Uri appUri = KeychainContract.ApiApps.buildByPackageNameUri(packageName);
            context.getContentResolver().delete(appUri, null, null);
        }
    }
}

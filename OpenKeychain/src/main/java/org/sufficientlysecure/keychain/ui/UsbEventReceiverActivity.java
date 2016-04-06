package org.sufficientlysecure.keychain.ui;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

public class UsbEventReceiverActivity extends Activity {
    public static final String ACTION_USB_PERMISSION =
            "org.sufficientlysecure.keychain.ui.USB_PERMISSION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

        Intent intent = getIntent();
        if (intent != null) {
            if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                Log.d(Constants.TAG, "Requesting permission for " + usbDevice.getDeviceName());
                usbManager.requestPermission(usbDevice,
                        PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0));
            }
        }

        // Close the activity
        finish();
    }
}
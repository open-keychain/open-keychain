package org.sufficientlysecure.keychain.smartcard;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.ui.UsbEventReceiverActivity;
import org.sufficientlysecure.keychain.util.Log;

public class UsbConnectionManager {
    private Activity mActivity;

    private OnDiscoveredUsbDeviceListener mListener;
    /**
     * Receives broadcast when a supported USB device get permission.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (UsbEventReceiverActivity.ACTION_USB_PERMISSION.equals(action)) {
                UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                        false);
                if (permission) {
                    Log.d(Constants.TAG, "Got permission for " + usbDevice.getDeviceName());
                    mListener.usbDeviceDiscovered(usbDevice);
                }
            }
        }
    };

    public UsbConnectionManager(final Activity activity, final OnDiscoveredUsbDeviceListener listener) {
        this.mActivity = activity;
        this.mListener = listener;
    }

    public void onStart() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UsbEventReceiverActivity.ACTION_USB_PERMISSION);

        mActivity.registerReceiver(mUsbReceiver, intentFilter);
    }

    public void onStop() {
        mActivity.unregisterReceiver(mUsbReceiver);
    }

    public void rescanDevices() {
        final SmartcardDevice smartcardDevice = SmartcardDevice.getInstance();
        if (smartcardDevice.isConnected()
                && (smartcardDevice.getTransport() instanceof UsbTransport)) {
            mListener.usbDeviceDiscovered(((UsbTransport) smartcardDevice.getTransport()).getUsbDevice());
        }
    }
}

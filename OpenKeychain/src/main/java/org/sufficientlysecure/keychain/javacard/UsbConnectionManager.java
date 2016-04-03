package org.sufficientlysecure.keychain.javacard;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

public class UsbConnectionManager {
    private static final String LOG_TAG = UsbConnectionManager.class.getName();
    private static final String ACTION_USB_PERMISSION = Constants.PACKAGE_NAME + ".USB_PERMITSSION";

    private Activity mActivity;
    private OnDiscoveredUsbDeviceListener mListener;
    private final Semaphore mRunning = new Semaphore(1);
    private final Set<UsbDevice> mProcessedDevices = Collections.newSetFromMap(new ConcurrentHashMap<UsbDevice, Boolean>());

    /**
     * Receives broadcast when a supported USB device is attached, detached or
     * when a permission to communicate to the device has been granted.
     */
    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            String deviceName = usbDevice.getDeviceName();

            if (ACTION_USB_PERMISSION.equals(action)) {
                boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
                        false);
                Log.d(LOG_TAG, "ACTION_USB_PERMISSION: " + permission + " Device: " + deviceName);

                interceptIntent(intent);

                context.unregisterReceiver(mUsbReceiver);
            }
        }
    };

    private final Thread mWatchThread = new Thread() {
        @Override
        public void run() {
            final UsbManager usbManager = (UsbManager) mActivity.getSystemService(Context.USB_SERVICE);

            while (true) {
                mRunning.acquireUninterruptibly();
                mRunning.release();

                //

                final UsbDevice device = getDevice(usbManager);
                if (device != null && !mProcessedDevices.contains(device)) {
                    mProcessedDevices.add(device);

                    final Intent intent = new Intent(ACTION_USB_PERMISSION);

                    IntentFilter filter = new IntentFilter();
                    filter.addAction(ACTION_USB_PERMISSION);
                    mActivity.registerReceiver(mUsbReceiver, filter);

                    usbManager.requestPermission(device, PendingIntent.getBroadcast(mActivity, 0, intent, 0));
                }

                try {
                    sleep(1000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    };

    public UsbConnectionManager(final Activity activity, final OnDiscoveredUsbDeviceListener listener) {
        this.mActivity = activity;
        this.mListener = listener;
        mRunning.acquireUninterruptibly();
        mWatchThread.start();
    }

    public void startListeningForDevices() {
        mRunning.release();
    }

    public void stopListeningForDevices() {
        mRunning.acquireUninterruptibly();
    }

    public void interceptIntent(final Intent intent) {
        if (intent == null || intent.getAction() == null) return;
        switch (intent.getAction()) {
            case UsbManager.ACTION_USB_DEVICE_ATTACHED: {
                final UsbManager usbManager = (UsbManager) mActivity.getSystemService(Context.USB_SERVICE);
                final UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                Intent usbI = new Intent(mActivity, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                usbI.setAction(ACTION_USB_PERMISSION);
                usbI.putExtra(UsbManager.EXTRA_DEVICE, device);
                PendingIntent pi = PendingIntent.getActivity(mActivity, 0, usbI, PendingIntent.FLAG_CANCEL_CURRENT);
                usbManager.requestPermission(device, pi);
                break;
            }
            case ACTION_USB_PERMISSION: {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                if (device != null)
                    mListener.usbDeviceDiscovered(device);
                break;
            }
            default:
                break;
        }
    }

    private static UsbDevice getDevice(UsbManager manager) {
        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        for (UsbDevice device : deviceList.values()) {
            Log.d(LOG_TAG, device.getDeviceName() + " " + device.getDeviceId());
            if (device.getVendorId() == 0x1050 && device.getProductId() == 0x0112) {
                Log.d(LOG_TAG, device.getDeviceName() + " OK");
                return device;
            }
        }
        return null;
    }
}

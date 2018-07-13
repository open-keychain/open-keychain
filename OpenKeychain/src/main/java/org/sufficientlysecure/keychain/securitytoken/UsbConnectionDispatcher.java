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

package org.sufficientlysecure.keychain.securitytoken;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.securitytoken.SecurityTokenInfo.TokenType;
import org.sufficientlysecure.keychain.securitytoken.usb.UsbTransport;
import timber.log.Timber;


public class UsbConnectionDispatcher {
    private static final String ACTION_USB_PERMISSION = "org.sufficientlysecure.keychain.ui.USB_PERMISSION";

    private Context context;
    private OnDiscoveredUsbDeviceListener onDiscoveredUsbDeviceListener;
    private UsbManager usbManager;

    public UsbConnectionDispatcher(Context context, OnDiscoveredUsbDeviceListener listener) {
        this.context = context.getApplicationContext();
        this.onDiscoveredUsbDeviceListener = listener;
        this.usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
    }

    private final BroadcastReceiver usbBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) {
                return;
            }

            switch (action) {
                case ACTION_USB_PERMISSION: {
                    UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    boolean permission = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false);
                    if (permission) {
                        Timber.d("Got permission for " + usbDevice.getDeviceName());
                        sendUsbTransportDiscovered(usbDevice);
                    }
                    break;
                }
            }
        }
    };

    public void onStart() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_USB_PERMISSION);

        context.registerReceiver(usbBroadcastReceiver, intentFilter);
    }

    public void onStop() {
        context.unregisterReceiver(usbBroadcastReceiver);
    }

    /**
     * Rescans devices and triggers {@link OnDiscoveredUsbDeviceListener}
     */
    public void rescanDevices() {
        // Note: we don't check devices VID/PID because
        // we check for permission instead.
        // We should have permission only for matching devices
        for (UsbDevice usbDevice : usbManager.getDeviceList().values()) {
            Timber.d("Device: %s", usbDevice.toString());
            if (usbManager.hasPermission(usbDevice)) {
                Timber.d("Got permission!");
                sendUsbTransportDiscovered(usbDevice);
                break;
            }
            TokenType tokenType = UsbTransport.getTokenTypeFromUsbDeviceInfo(
                    usbDevice.getVendorId(), usbDevice.getProductId(), null);
            if (tokenType != null) {
                Timber.d("Token type: %s", tokenType);
                requestPermissionForUsbDevice(context, usbDevice);
                break;
            }
            Timber.d("Unknown device type, doing nothing…");
        }
    }

    private void sendUsbTransportDiscovered(UsbDevice usbDevice) {
        if (onDiscoveredUsbDeviceListener == null) {
            return;
        }

        UsbTransport usbTransport = UsbTransport.createUsbTransport(context, usbDevice);
        onDiscoveredUsbDeviceListener.usbTransportDiscovered(usbTransport);
    }

    public interface OnDiscoveredUsbDeviceListener {
        void usbTransportDiscovered(UsbTransport usbTransport);
    }

    public static void requestPermissionForUsbDevice(Context context, UsbDevice usbDevice) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        if (usbManager == null) {
            return;
        }

        Intent answerBroadcastIntent = new Intent(ACTION_USB_PERMISSION);
        answerBroadcastIntent.setPackage(BuildConfig.APPLICATION_ID);
        PendingIntent answerPendingIntent = PendingIntent.getBroadcast(context, 0, answerBroadcastIntent, 0);

        Timber.d("Requesting permission for " + usbDevice.getDeviceName());
        usbManager.requestPermission(usbDevice, answerPendingIntent);
    }
}

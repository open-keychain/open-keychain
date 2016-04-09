/*
 * Copyright (C) 2016 Nikita Mikhailov <nikita.s.mikhailov@gmail.com>
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

package org.sufficientlysecure.keychain.util;

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

public class UsbConnectionDispatcher {
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

    public UsbConnectionDispatcher(final Activity activity, final OnDiscoveredUsbDeviceListener listener) {
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

    public interface OnDiscoveredUsbDeviceListener {
        void usbDeviceDiscovered(UsbDevice usbDevice);
    }
}

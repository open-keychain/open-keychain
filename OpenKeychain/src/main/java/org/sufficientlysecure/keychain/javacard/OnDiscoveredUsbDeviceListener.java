package org.sufficientlysecure.keychain.javacard;

import android.hardware.usb.UsbDevice;

public interface OnDiscoveredUsbDeviceListener {
    void usbDeviceDiscovered(UsbDevice usbDevice);
}
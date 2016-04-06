package org.sufficientlysecure.keychain.smartcard;

import android.hardware.usb.UsbDevice;

public interface OnDiscoveredUsbDeviceListener {
    void usbDeviceDiscovered(UsbDevice usbDevice);
}
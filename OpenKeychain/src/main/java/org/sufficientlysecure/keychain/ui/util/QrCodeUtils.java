/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2011 Andreas Schildbach
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

package org.sufficientlysecure.keychain.ui.util;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.KeychainApplication;
import org.sufficientlysecure.keychain.util.Log;

import java.util.Hashtable;
import java.util.Locale;

/**
 * Copied from Bitcoin Wallet
 */
public class QrCodeUtils {

    public static Bitmap getQRCodeBitmap(final Uri uri) {
        return getQRCodeBitmap(uri.toString(), 0);
    }

    public static Bitmap getQRCodeBitmap(final Uri uri, final int size) {
        // for URIs we want alphanumeric encoding to save space, thus make everything upper case!
        // zxing will then select Mode.ALPHANUMERIC internally
        return getQRCodeBitmap(uri.toString().toUpperCase(Locale.ENGLISH), size);
    }

    /**
     * Generate Bitmap with QR Code based on input.
     * @return QR Code as Bitmap
     */
    private static Bitmap getQRCodeBitmap(final String input, final int size) {

        try {

            // the qrCodeCache is handled in KeychainApplication so we can
            // properly react to onTrimMemory calls
            Bitmap bitmap = KeychainApplication.qrCodeCache.get(input);
            if (bitmap == null) {

                Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
                hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
                BitMatrix result = new QRCodeWriter().encode(input, BarcodeFormat.QR_CODE, size,
                        size, hints);

                int width = result.getWidth();
                int height = result.getHeight();
                int[] pixels = new int[width * height];

                for (int y = 0; y < height; y++) {
                    final int offset = y * width;
                    for (int x = 0; x < width; x++) {
                        pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
                    }
                }

                bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

                KeychainApplication.qrCodeCache.put(input, bitmap);
            }

            return bitmap;
        } catch (WriterException e) {
            Log.e(Constants.TAG, "QrCodeUtils", e);
            return null;
        }

    }

}

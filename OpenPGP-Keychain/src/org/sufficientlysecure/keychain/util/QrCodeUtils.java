/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.util;

import java.util.Hashtable;

import org.sufficientlysecure.keychain.Constants;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QrCodeUtils {
    public final static QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

    /**
     * Generate Bitmap with QR Code based on input.
     * 
     * @param input
     * @param size
     * @return QR Code as Bitmap
     */
    public static Bitmap getQRCodeBitmap(final String input, final int size) {
        try {
            final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            final BitMatrix result = QR_CODE_WRITER.encode(input, BarcodeFormat.QR_CODE, size,
                    size, hints);

            final int width = result.getWidth();
            final int height = result.getHeight();
            final int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                final int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
                }
            }

            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (final WriterException e) {
            Log.e(Constants.TAG, "QrCodeUtils", e);
            return null;
        }
    }

    /**
     * Displays QrCode in Dialog
     */
    // public static void showQrCode(Activity activity, Bitmap qrCodeBitmap) {
    // final Dialog dialog = new Dialog(activity);
    // dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
    // dialog.setContentView(R.layout.qr_code_dialog);
    // final ImageView imageView = (ImageView) dialog.findViewById(R.id.qr_dialog_view);
    // imageView.setImageBitmap(qrCodeBitmap);
    // dialog.setCanceledOnTouchOutside(true);
    // dialog.show();
    // imageView.setOnClickListener(new OnClickListener() {
    // public void onClick(final View v) {
    // dialog.dismiss();
    // }
    // });
    // }

    /**
     * Starts Scanning of Barcode with Barcode Scanner App, if Barcode Scanner is not installed
     * requests install of it, done by using IntentIntegrator from Barcode Scanner
     * 
     * @param activity
     */
    // public static void scanQrCode(Activity activity) {
    // IntentIntegrator.initiateScan(activity, R.string.no_barcode_scanner_title,
    // R.string.no_barcode_scanner, R.string.button_yes, R.string.button_no);
    // }
    //
    // /**
    // * Return scanned QR Code as String, must be used in Activities onActivityResult(), done by
    // * using IntentIntegrator from Barcode Scanner
    // *
    // * @param requestCode
    // * @param resultCode
    // * @param intent
    // * @return QR Code content as String
    // */
    // public static String returnQrCodeOnActivityResult(int requestCode, int resultCode, Intent
    // intent) {
    // IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode,
    // intent);
    //
    // if (scanResult != null) {
    // return scanResult.getContents();
    // } else {
    // return null;
    // }
    // }
}

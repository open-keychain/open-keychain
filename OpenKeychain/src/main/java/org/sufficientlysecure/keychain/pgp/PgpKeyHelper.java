/*
 * Copyright (C) 2012-2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010-2014 Thialfihar <thi@thialfihar.org>
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

package org.sufficientlysecure.keychain.pgp;

import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.nist.NISTNamedCurves;
import org.spongycastle.asn1.teletrust.TeleTrusTNamedCurves;
import org.spongycastle.bcpg.PublicKeyAlgorithmTags;
import org.spongycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Algorithm;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.Curve;
import org.sufficientlysecure.keychain.util.Log;

import java.security.DigestException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class PgpKeyHelper {

    public static String getAlgorithmInfo(int algorithm, Integer keySize, String oid) {
        return getAlgorithmInfo(null, algorithm, keySize, oid);
    }

    /**
     * Based on <a href="http://tools.ietf.org/html/rfc2440#section-9.1">OpenPGP Message Format</a>
     */
    public static String getAlgorithmInfo(Context context, int algorithm, Integer keySize, String oid) {
        String algorithmStr;

        switch (algorithm) {
            case PublicKeyAlgorithmTags.RSA_ENCRYPT:
            case PublicKeyAlgorithmTags.RSA_GENERAL:
            case PublicKeyAlgorithmTags.RSA_SIGN: {
                algorithmStr = "RSA";
                break;
            }
            case PublicKeyAlgorithmTags.DSA: {
                algorithmStr = "DSA";
                break;
            }

            case PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT:
            case PublicKeyAlgorithmTags.ELGAMAL_GENERAL: {
                algorithmStr = "ElGamal";
                break;
            }

            case PublicKeyAlgorithmTags.ECDSA: {
                if (oid == null) {
                    return "ECDSA";
                }
                String oidName = PgpKeyHelper.getCurveInfo(context, oid);
                return "ECDSA (" + oidName + ")";
            }
            case PublicKeyAlgorithmTags.ECDH: {
                if (oid == null) {
                    return "ECDH";
                }
                String oidName = PgpKeyHelper.getCurveInfo(context, oid);
                return "ECDH (" + oidName + ")";
            }

            default: {
                if (context != null) {
                    algorithmStr = context.getResources().getString(R.string.unknown_algorithm);
                } else {
                    algorithmStr = "unknown";
                }
                break;
            }
        }
        if (keySize != null && keySize > 0)
            return algorithmStr + ", " + keySize + " bit";
        else
            return algorithmStr;
    }

    public static String getAlgorithmInfo(Algorithm algorithm, Integer keySize, Curve curve) {
        return getAlgorithmInfo(null, algorithm, keySize, curve);
    }

    /**
     * Based on <a href="http://tools.ietf.org/html/rfc2440#section-9.1">OpenPGP Message Format</a>
     */
    public static String getAlgorithmInfo(Context context, Algorithm algorithm, Integer keySize, Curve curve) {
        String algorithmStr;

        switch (algorithm) {
            case RSA: {
                algorithmStr = "RSA";
                break;
            }
            case DSA: {
                algorithmStr = "DSA";
                break;
            }

            case ELGAMAL: {
                algorithmStr = "ElGamal";
                break;
            }

            case ECDSA: {
                algorithmStr = "ECDSA";
                if (curve != null) {
                    algorithmStr += " (" + getCurveInfo(context, curve) + ")";
                }
                return algorithmStr;
            }
            case ECDH: {
                algorithmStr = "ECDH";
                if (curve != null) {
                    algorithmStr += " (" + getCurveInfo(context, curve) + ")";
                }
                return algorithmStr;
            }

            default: {
                if (context != null) {
                    algorithmStr = context.getResources().getString(R.string.unknown_algorithm);
                } else {
                    algorithmStr = "unknown";
                }
                break;
            }
        }
        if (keySize != null && keySize > 0)
            return algorithmStr + ", " + keySize + " bit";
        else
            return algorithmStr;
    }

    /**
     * Return name of a curve. These are names, no need for translation
     */
    public static String getCurveInfo(Context context, Curve curve) {
        switch (curve) {
            case NIST_P256:
                return "NIST P-256";
            case NIST_P384:
                return "NIST P-384";
            case NIST_P521:
                return "NIST P-521";

            /* see SaveKeyringParcel
            case BRAINPOOL_P256:
                return "Brainpool P-256";
            case BRAINPOOL_P384:
                return "Brainpool P-384";
            case BRAINPOOL_P512:
                return "Brainpool P-512";
            */
        }
        if (context != null) {
            return context.getResources().getString(R.string.unknown_algorithm);
        } else {
            return "unknown";
        }
    }

    public static String getCurveInfo(Context context, String oidStr) {
        ASN1ObjectIdentifier oid = new ASN1ObjectIdentifier(oidStr);

        String name;
        name = NISTNamedCurves.getName(oid);
        if (name != null) {
            return name;
        }
        name = TeleTrusTNamedCurves.getName(oid);
        if (name != null) {
            return name;
        }
        if (context != null) {
            return context.getResources().getString(R.string.unknown_algorithm);
        } else {
            return "unknown";
        }
    }

    /**
     * Converts fingerprint to hex
     * <p/>
     * Fingerprint is shown using lowercase characters. Studies have shown that humans can
     * better differentiate between numbers and letters when letters are lowercase.
     *
     * @param fingerprint
     * @return
     */
    public static String convertFingerprintToHex(byte[] fingerprint) {
        return Hex.toHexString(fingerprint).toLowerCase(Locale.ENGLISH);
    }

    /**
     * Convert key id from long to 64 bit hex string
     * <p/>
     * V4: "The Key ID is the low-order 64 bits of the fingerprint"
     * <p/>
     * see http://tools.ietf.org/html/rfc4880#section-12.2
     *
     * @param keyId
     * @return
     */
    public static String convertKeyIdToHex(long keyId) {
        long upper = keyId >> 32;
        if (upper == 0) {
            // this is a short key id
            return convertKeyIdToHexShort(keyId);
        }
        return "0x" + convertKeyIdToHex32bit(keyId >> 32) + convertKeyIdToHex32bit(keyId);
    }

    public static String convertKeyIdToHexShort(long keyId) {
        return "0x" + convertKeyIdToHex32bit(keyId);
    }

    private static String convertKeyIdToHex32bit(long keyId) {
        String hexString = Long.toHexString(keyId & 0xffffffffL).toLowerCase(Locale.ENGLISH);
        while (hexString.length() < 8) {
            hexString = "0" + hexString;
        }
        return hexString;
    }


    public static SpannableStringBuilder colorizeFingerprint(String fingerprint) {
        // split by 4 characters
        fingerprint = fingerprint.replaceAll("(.{4})(?!$)", "$1 ");

        // add line breaks to have a consistent "image" that can be recognized
        char[] chars = fingerprint.toCharArray();
        chars[24] = '\n';
        fingerprint = String.valueOf(chars);

        SpannableStringBuilder sb = new SpannableStringBuilder(fingerprint);
        try {
            // for each 4 characters of the fingerprint + 1 space
            for (int i = 0; i < fingerprint.length(); i += 5) {
                int spanEnd = Math.min(i + 4, fingerprint.length());
                String fourChars = fingerprint.substring(i, spanEnd);

                int raw = Integer.parseInt(fourChars, 16);
                byte[] bytes = {(byte) ((raw >> 8) & 0xff - 128), (byte) (raw & 0xff - 128)};
                int[] color = getRgbForData(bytes);
                int r = color[0];
                int g = color[1];
                int b = color[2];

                // we cannot change black by multiplication, so adjust it to an almost-black grey,
                // which will then be brightened to the minimal brightness level
                if (r == 0 && g == 0 && b == 0) {
                    r = 1;
                    g = 1;
                    b = 1;
                }

                // Convert rgb to brightness
                double brightness = 0.2126 * r + 0.7152 * g + 0.0722 * b;

                // If a color is too dark to be seen on black,
                // then brighten it up to a minimal brightness.
                if (brightness < 80) {
                    double factor = 80.0 / brightness;
                    r = Math.min(255, (int) (r * factor));
                    g = Math.min(255, (int) (g * factor));
                    b = Math.min(255, (int) (b * factor));

                    // If it is too light, then darken it to a respective maximal brightness.
                } else if (brightness > 180) {
                    double factor = 180.0 / brightness;
                    r = (int) (r * factor);
                    g = (int) (g * factor);
                    b = (int) (b * factor);
                }

                // Create a foreground color with the 3 digest integers as RGB
                // and then converting that int to hex to use as a color
                sb.setSpan(new ForegroundColorSpan(Color.rgb(r, g, b)),
                        i, spanEnd, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
        } catch (Exception e) {
            Log.e(Constants.TAG, "Colorization failed", e);
            // if anything goes wrong, then just display the fingerprint without colour,
            // instead of partially correct colour or wrong colours
            return new SpannableStringBuilder(fingerprint);
        }

        return sb;
    }

    /**
     * Converts the given bytes to a unique RGB color using SHA1 algorithm
     *
     * @param bytes
     * @return an integer array containing 3 numeric color representations (Red, Green, Black)
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.DigestException
     */
    private static int[] getRgbForData(byte[] bytes) throws NoSuchAlgorithmException, DigestException {
        MessageDigest md = MessageDigest.getInstance("SHA1");

        md.update(bytes);
        byte[] digest = md.digest();

        return new int[]{((int) digest[0] + 256) % 256,
                ((int) digest[1] + 256) % 256,
                ((int) digest[2] + 256) % 256};
    }

}

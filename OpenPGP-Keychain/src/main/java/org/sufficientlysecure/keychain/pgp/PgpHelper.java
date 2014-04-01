/*
 * Copyright (C) 2012-2013 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2010 Thialfihar <thi@thialfihar.org>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sufficientlysecure.keychain.pgp;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;

import org.spongycastle.openpgp.PGPEncryptedDataList;
import org.spongycastle.openpgp.PGPObjectFactory;
import org.spongycastle.openpgp.PGPPublicKeyEncryptedData;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKey;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.spongycastle.openpgp.PGPUtil;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.exception.NoAsymmetricEncryptionException;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.ProviderHelper;
import org.sufficientlysecure.keychain.util.Log;
import org.sufficientlysecure.keychain.util.ProgressDialogUpdater;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.Iterator;
import java.util.regex.Pattern;

public class PgpHelper {

    public static final Pattern PGP_MESSAGE = Pattern.compile(
            ".*?(-----BEGIN PGP MESSAGE-----.*?-----END PGP MESSAGE-----).*", Pattern.DOTALL);

    public static final Pattern PGP_CLEARTEXT_SIGNATURE = Pattern
            .compile(
                    ".*?(-----BEGIN PGP SIGNED MESSAGE-----.*?-----BEGIN PGP SIGNATURE-----.*?-----END PGP SIGNATURE-----).*",
                    Pattern.DOTALL);

    public static final Pattern PGP_PUBLIC_KEY = Pattern.compile(
            ".*?(-----BEGIN PGP PUBLIC KEY BLOCK-----.*?-----END PGP PUBLIC KEY BLOCK-----).*",
            Pattern.DOTALL);

    public static String getVersion(Context context) {
        String version = null;
        try {
            PackageInfo pi = context.getPackageManager().getPackageInfo(Constants.PACKAGE_NAME, 0);
            version = pi.versionName;
            return version;
        } catch (NameNotFoundException e) {
            Log.e(Constants.TAG, "Version could not be retrieved!", e);
            return "0.0.0";
        }
    }

    public static String getFullVersion(Context context) {
        return "OpenPGP Keychain v" + getVersion(context);
    }

    public static long getDecryptionKeyId(Context context, InputStream inputStream)
            throws PgpGeneralException, NoAsymmetricEncryptionException, IOException {
        InputStream in = PGPUtil.getDecoderStream(inputStream);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        PGPEncryptedDataList enc;
        Object o = pgpF.nextObject();

        // the first object might be a PGP marker packet.
        if (o instanceof PGPEncryptedDataList) {
            enc = (PGPEncryptedDataList) o;
        } else {
            enc = (PGPEncryptedDataList) pgpF.nextObject();
        }

        if (enc == null) {
            throw new PgpGeneralException(context.getString(R.string.error_invalid_data));
        }

        // TODO: currently we always only look at the first known key
        // find the secret key
        PGPSecretKey secretKey = null;
        Iterator<?> it = enc.getEncryptedDataObjects();
        boolean gotAsymmetricEncryption = false;
        while (it.hasNext()) {
            Object obj = it.next();
            if (obj instanceof PGPPublicKeyEncryptedData) {
                gotAsymmetricEncryption = true;
                PGPPublicKeyEncryptedData pbe = (PGPPublicKeyEncryptedData) obj;
                secretKey = ProviderHelper.getPGPSecretKeyByKeyId(context, pbe.getKeyID());
                if (secretKey != null) {
                    break;
                }
            }
        }

        if (!gotAsymmetricEncryption) {
            throw new NoAsymmetricEncryptionException();
        }

        if (secretKey == null) {
            return Id.key.none;
        }

        return secretKey.getKeyID();
    }

    public static int getStreamContent(Context context, InputStream inStream) throws IOException {
        InputStream in = PGPUtil.getDecoderStream(inStream);
        PGPObjectFactory pgpF = new PGPObjectFactory(in);
        Object object = pgpF.nextObject();
        while (object != null) {
            if (object instanceof PGPPublicKeyRing || object instanceof PGPSecretKeyRing) {
                return Id.content.keys;
            } else if (object instanceof PGPEncryptedDataList) {
                return Id.content.encrypted_data;
            }
            object = pgpF.nextObject();
        }

        return Id.content.unknown;
    }

    /**
     * Generate a random filename
     *
     * @param length
     * @return
     */
    public static String generateRandomFilename(int length) {
        SecureRandom random = new SecureRandom();

        byte bytes[] = new byte[length];
        random.nextBytes(bytes);
        String result = "";
        for (int i = 0; i < length; ++i) {
            int v = (bytes[i] + 256) % 64;
            if (v < 10) {
                result += (char) ('0' + v);
            } else if (v < 36) {
                result += (char) ('A' + v - 10);
            } else if (v < 62) {
                result += (char) ('a' + v - 36);
            } else if (v == 62) {
                result += '_';
            } else if (v == 63) {
                result += '.';
            }
        }
        return result;
    }

    /**
     * Go once through stream to get length of stream. The length is later used to display progress
     * when encrypting/decrypting
     *
     * @param in
     * @return
     * @throws IOException
     */
    public static long getLengthOfStream(InputStream in) throws IOException {
        long size = 0;
        long n = 0;
        byte dummy[] = new byte[0x10000];
        while ((n = in.read(dummy)) > 0) {
            size += n;
        }
        return size;
    }

    /**
     * Deletes file securely by overwriting it with random data before deleting it.
     * <p/>
     * TODO: Does this really help on flash storage?
     *
     * @param context
     * @param progress
     * @param file
     * @throws IOException
     */
    public static void deleteFileSecurely(Context context, ProgressDialogUpdater progress, File file)
            throws IOException {
        long length = file.length();
        SecureRandom random = new SecureRandom();
        RandomAccessFile raf = new RandomAccessFile(file, "rws");
        raf.seek(0);
        raf.getFilePointer();
        byte[] data = new byte[1 << 16];
        int pos = 0;
        String msg = context.getString(R.string.progress_deleting_securely, file.getName());
        while (pos < length) {
            if (progress != null) {
                progress.setProgress(msg, (int) (100 * pos / length), 100);
            }
            random.nextBytes(data);
            raf.write(data);
            pos += data.length;
        }
        raf.close();
        file.delete();
    }
}

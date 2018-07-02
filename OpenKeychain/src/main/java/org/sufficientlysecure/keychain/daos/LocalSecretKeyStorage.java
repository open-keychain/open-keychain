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

package org.sufficientlysecure.keychain.daos;


import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;

import okhttp3.internal.Util;


public class LocalSecretKeyStorage {
    private static final String FORMAT_STR_SECRET_KEY = "0x%016x.sec";
    private static final String SECRET_KEYS_DIR_NAME = "secret_keys";


    private final File localSecretKeysDir;


    public static LocalSecretKeyStorage getInstance(Context context) {
        File localSecretKeysDir = new File(context.getFilesDir(), SECRET_KEYS_DIR_NAME);
        return new LocalSecretKeyStorage(localSecretKeysDir);
    }

    private LocalSecretKeyStorage(File localSecretKeysDir) {
        this.localSecretKeysDir = localSecretKeysDir;
    }

    private File getSecretKeyFile(long masterKeyId) throws IOException {
        if (!localSecretKeysDir.exists()) {
            localSecretKeysDir.mkdir();
        }
        if (!localSecretKeysDir.isDirectory()) {
            throw new IOException("Failed creating public key directory!");
        }

        String keyFilename = String.format(FORMAT_STR_SECRET_KEY, masterKeyId);
        return new File(localSecretKeysDir, keyFilename);
    }

    public void writeSecretKey(long masterKeyId, byte[] encoded) throws IOException {
        File publicKeyFile = getSecretKeyFile(masterKeyId);

        FileOutputStream fileOutputStream = new FileOutputStream(publicKeyFile);
        try {
            fileOutputStream.write(encoded);
        } finally {
            Util.closeQuietly(fileOutputStream);
        }
    }

    byte[] readSecretKey(long masterKeyId) throws IOException {
        File publicKeyFile = getSecretKeyFile(masterKeyId);

        try {
            FileInputStream fileInputStream = new FileInputStream(publicKeyFile);
            return readIntoByteArray(fileInputStream);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    private static byte[] readIntoByteArray(FileInputStream fileInputStream) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        byte[] buf = new byte[128];
        int bytesRead;
        while ((bytesRead = fileInputStream.read(buf)) != -1) {
            baos.write(buf, 0, bytesRead);
        }

        return baos.toByteArray();
    }

    void deleteSecretKey(long masterKeyId) throws IOException {
        File publicKeyFile = getSecretKeyFile(masterKeyId);
        if (publicKeyFile.exists()) {
            boolean deleteSuccess = publicKeyFile.delete();
            if (!deleteSuccess) {
                throw new IOException("File exists, but could not be deleted!");
            }
        }
    }
}

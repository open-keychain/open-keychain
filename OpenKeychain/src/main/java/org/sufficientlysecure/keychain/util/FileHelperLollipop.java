/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;

import org.sufficientlysecure.keychain.Constants;

/**
 * FileHelper methods which use Lollipop-exclusive API.
 * Some of the methods and static fields used here cause VerifyErrors because
 * they do not exist in pre-lollipop API, so they must be kept in a
 * lollipop-only class. All methods here should only be called by FileHelper,
 * and consequently have package visibility.
 */
@TargetApi(VERSION_CODES.LOLLIPOP)
class FileHelperLollipop {

    /**
     * A replacement for ContentResolver.openInputStream() that does not allow the usage of
     * "file" Uris that point to private files owned by the application only.
     *
     * This is not allowed:
     * am start -a android.intent.action.SEND -t text/plain -n
     * "org.sufficientlysecure.keychain.debug/org.sufficientlysecure.keychain.ui.EncryptFilesActivity" --eu
     * android.intent.extra.STREAM
     * file:///data/data/org.sufficientlysecure.keychain.debug/databases/openkeychain.db
     *
     * @throws FileNotFoundException
     */
    static InputStream openInputStreamSafe(ContentResolver resolver, Uri uri)
            throws FileNotFoundException {

        String scheme = uri.getScheme();
        if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            ParcelFileDescriptor pfd = ParcelFileDescriptor.open(
                    new File(uri.getPath()), ParcelFileDescriptor.parseMode("r"));

            try {
                final StructStat st = Os.fstat(pfd.getFileDescriptor());
                if (st.st_uid == android.os.Process.myUid()) {
                    Log.e(Constants.TAG, "File is owned by the application itself, aborting!");
                    throw new FileNotFoundException("Unable to create stream");
                }
            } catch (ErrnoException e) {
                Log.e(Constants.TAG, "fstat() failed: " + e);
                throw new FileNotFoundException("fstat() failed");
            }

            AssetFileDescriptor fd = new AssetFileDescriptor(pfd, 0, -1);
            try {
                return fd.createInputStream();
            } catch (IOException e) {
                throw new FileNotFoundException("Unable to create stream");
            }
        } else {
            return resolver.openInputStream(uri);
        }

    }
}

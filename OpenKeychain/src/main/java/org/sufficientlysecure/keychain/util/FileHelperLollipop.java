package org.sufficientlysecure.keychain.util;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.ParcelFileDescriptor;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;

import org.sufficientlysecure.keychain.Constants;

import static android.system.OsConstants.S_IROTH;


/** FileHelper methods which use Lollipop-exclusive API.
 * Some of the methods and static fields used here cause VerifyErrors because
 * they do not exist in pre-lollipop API, so they must be kept in a
 * lollipop-only class. All methods here should only be called by FileHelper,
 * and consequently have package visibility.
 */
@TargetApi(VERSION_CODES.LOLLIPOP)
class FileHelperLollipop {
    /**
     * Tests whether a file is readable by others
     */
    private static boolean S_IROTH(int mode) {
        return (mode & S_IROTH) == S_IROTH;
    }

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
                if (!S_IROTH(st.st_mode)) {
                    Log.e(Constants.TAG, "File is not readable by others, aborting!");
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

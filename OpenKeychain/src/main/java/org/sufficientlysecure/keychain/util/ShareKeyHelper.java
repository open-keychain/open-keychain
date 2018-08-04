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

package org.sufficientlysecure.keychain.util;


import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.pgp.CanonicalizedPublicKey;
import org.sufficientlysecure.keychain.pgp.SshPublicKey;
import org.sufficientlysecure.keychain.pgp.exception.PgpGeneralException;
import org.sufficientlysecure.keychain.provider.TemporaryFileProvider;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import timber.log.Timber;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;

public class ShareKeyHelper {

    private static String getKeyContent(UnifiedKeyInfo unifiedKeyInfo, KeyRepository keyRepository)
            throws KeyRepository.NotFoundException, IOException {

        return keyRepository.getPublicKeyRingAsArmoredString(unifiedKeyInfo.master_key_id());
    }

    private static String getSshKeyContent(UnifiedKeyInfo unifiedKeyInfo, KeyRepository keyRepository)
            throws KeyRepository.NotFoundException, PgpGeneralException, NoSuchAlgorithmException {

        long authSubKeyId = unifiedKeyInfo.has_auth_key_int();
        CanonicalizedPublicKey publicKey = keyRepository.getCanonicalizedPublicKeyRing(unifiedKeyInfo.master_key_id())
                .getPublicKey(authSubKeyId);
        SshPublicKey sshPublicKey = new SshPublicKey(publicKey);

        return sshPublicKey.getEncodedKey();
    }

    private static void shareKeyIntent(Activity activity, UnifiedKeyInfo unifiedKeyInfo, String content) throws IOException {
        // let user choose application
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType(Constants.MIME_TYPE_KEYS);

        // NOTE: Don't use Intent.EXTRA_TEXT to send the key
        // better send it via a Uri!
        // example: Bluetooth Share will convert text/plain sent via Intent.EXTRA_TEXT to HTML
        try {
            TemporaryFileProvider shareFileProv = new TemporaryFileProvider();

            String filename;
            if (unifiedKeyInfo.name() != null) {
                filename = unifiedKeyInfo.name();
            } else {
                filename = KeyFormattingUtils.convertFingerprintToHex(unifiedKeyInfo.fingerprint());
            }
            Uri contentUri = TemporaryFileProvider.createFile(activity, filename + Constants.FILE_EXTENSION_ASC);

            BufferedWriter contentWriter = new BufferedWriter(new OutputStreamWriter(
                    new ParcelFileDescriptor.AutoCloseOutputStream(
                            shareFileProv.openFile(contentUri, "w"))));
            contentWriter.write(content);
            contentWriter.close();

            sendIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
        } catch (FileNotFoundException e) {
            Timber.e(e, "Error creating temporary key share file!");
            // no need for a snackbar because one sharing option doesn't work
            // Notify.create(getActivity(), R.string.error_temp_file, Notify.Style.ERROR).show();
        }

        String title = activity.getString(R.string.title_share_key);
        Intent shareChooser = Intent.createChooser(sendIntent, title);

        activity.startActivity(shareChooser);
    }

    private static void shareKeyToClipBoard(Activity activity, String content) {
        ClipboardManager clipMan = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipMan == null) {
            Notify.create(activity, R.string.error_clipboard_copy, Notify.Style.ERROR).show();
            return;
        }

        ClipData clip = ClipData.newPlainText(Constants.CLIPBOARD_LABEL, content);
        clipMan.setPrimaryClip(clip);

        Notify.create(activity, R.string.key_copied_to_clipboard, Notify.Style.OK).show();
    }

    private static void shareKey(Activity activity, UnifiedKeyInfo unifiedKeyInfo, boolean toClipboard) {
        if (activity == null || unifiedKeyInfo == null) {
            return;
        }

        try {
            String content = getKeyContent(unifiedKeyInfo, KeyRepository.create(activity));
            if (toClipboard) {
                shareKeyToClipBoard(activity, content);
            } else {
                shareKeyIntent(activity, unifiedKeyInfo, content);
            }
        } catch (IOException e) {
            Timber.e(e, "error processing key!");
            Notify.create(activity, R.string.error_key_processing, Notify.Style.ERROR).show();
        } catch (KeyRepository.NotFoundException e) {
            Timber.e(e, "key not found!");
            Notify.create(activity, R.string.error_key_not_found, Notify.Style.ERROR).show();
        }
    }

    private static void shareSshKey(Activity activity, UnifiedKeyInfo unifiedKeyInfo, boolean toClipboard) {
        if (activity == null || unifiedKeyInfo == null) {
            return;
        }
        if (!unifiedKeyInfo.has_auth_key()) {
            Notify.create(activity, R.string.authentication_subkey_not_found, Notify.Style.ERROR).show();
            return;
        }

        try {
            String content = getSshKeyContent(unifiedKeyInfo, KeyRepository.create(activity));
            if (toClipboard) {
                shareKeyToClipBoard(activity, content);
            } else {
                shareKeyIntent(activity, unifiedKeyInfo, content);
            }
        } catch (PgpGeneralException | IOException | NoSuchAlgorithmException e) {
            Timber.e(e, "error processing key!");
            Notify.create(activity, R.string.error_key_processing, Notify.Style.ERROR).show();
        } catch (KeyRepository.NotFoundException e) {
            Timber.e(e, "key not found!");
            Notify.create(activity, R.string.error_key_not_found, Notify.Style.ERROR).show();
        }
    }

    public static void shareKeyToClipboard(Activity activity, UnifiedKeyInfo unifiedKeyInfo) {
        shareKey(activity, unifiedKeyInfo, true);
    }
    public static void shareKey(Activity activity, UnifiedKeyInfo unifiedKeyInfo) {
        shareKey(activity, unifiedKeyInfo, false);
    }
    public static void shareSshKey(Activity activity, UnifiedKeyInfo unifiedKeyInfo) {
        shareSshKey(activity, unifiedKeyInfo, false);
    }
    public static void shareSshKeyToClipboard(Activity activity, UnifiedKeyInfo unifiedKeyInfo) {
        shareSshKey(activity, unifiedKeyInfo, true);
    }

}

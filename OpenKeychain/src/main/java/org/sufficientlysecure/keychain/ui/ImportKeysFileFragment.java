/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.ui.ImportKeysListFragment.BytesLoaderState;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ImportKeysFileFragment extends Fragment {
    private ImportKeysActivity mImportActivity;
    private View mBrowse;
    private View mClipboardButton;

    private Uri mCurrentUri;

    private static final int REQUEST_CODE_FILE = 0x00007003;
    private static final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 12;

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysFileFragment newInstance() {
        ImportKeysFileFragment frag = new ImportKeysFileFragment();

        Bundle args = new Bundle();

        frag.setArguments(args);
        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.import_keys_file_fragment, container, false);

        mBrowse = view.findViewById(R.id.import_keys_file_browse);

        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // open .asc or .gpg files
                // setting it to text/plain prevents Cyanogenmod's file manager from selecting asc
                // or gpg types!
                FileHelper.openDocument(ImportKeysFileFragment.this,
                        Uri.fromFile(Constants.Path.APP_DIR), "*/*", false, REQUEST_CODE_FILE);
            }
        });

        mClipboardButton = view.findViewById(R.id.import_clipboard_button);
        mClipboardButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                CharSequence clipboardText = ClipboardReflection.getClipboardText(getActivity());
                String sendText = "";
                if (clipboardText != null) {
                    sendText = clipboardText.toString();
                    sendText = PgpHelper.getPgpKeyContent(sendText);
                    if (sendText == null) {
                        Notify.create(mImportActivity, R.string.error_bad_data, Style.ERROR).show();
                        return;
                    }
                    mImportActivity.loadCallback(new BytesLoaderState(sendText.getBytes(), null));
                }
            }
        });

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mImportActivity = (ImportKeysActivity) activity;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_FILE: {
                if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                    mCurrentUri = data.getData();

                    if (checkAndRequestReadPermission(mCurrentUri)) {
                        startImportingKeys();
                    }
                }
                break;
            }

            default:
                super.onActivityResult(requestCode, resultCode, data);

                break;
        }
    }

    private void startImportingKeys() {
        boolean isEncrypted;
        try {
            isEncrypted = FileHelper.isEncryptedFile(mImportActivity, mCurrentUri);
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error opening file", e);

            Notify.create(mImportActivity, R.string.error_bad_data, Style.ERROR).show();
            return;
        }

        if (isEncrypted) {
            Intent intent = new Intent(mImportActivity, DecryptActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(mCurrentUri);
            startActivity(intent);
        } else {
            mImportActivity.loadCallback(new BytesLoaderState(null, mCurrentUri));
        }
    }

    /**
     * Request READ_EXTERNAL_STORAGE permission on Android >= 6.0 to read content from "file" Uris.
     * <p/>
     * This method returns true on Android < 6, or if permission is already granted. It
     * requests the permission and returns false otherwise.
     * <p/>
     * see https://commonsware.com/blog/2015/10/07/runtime-permissions-files-action-send.html
     */
    private boolean checkAndRequestReadPermission(final Uri uri) {
        if (!ContentResolver.SCHEME_FILE.equals(uri.getScheme())) {
            return true;
        }

        // Additional check due to https://commonsware.com/blog/2015/11/09/you-cannot-hold-nonexistent-permissions.html
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        requestPermissions(
                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);

        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (requestCode != REQUEST_PERMISSION_READ_EXTERNAL_STORAGE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }

        boolean permissionWasGranted = grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED;

        if (permissionWasGranted) {
            startImportingKeys();
        } else {
            Toast.makeText(getActivity(), R.string.error_denied_storage_permission, Toast.LENGTH_LONG).show();
            getActivity().setResult(Activity.RESULT_CANCELED);
            getActivity().finish();
        }
    }

}

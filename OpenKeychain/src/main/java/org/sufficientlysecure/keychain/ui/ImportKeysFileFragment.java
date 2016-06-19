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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.keyimport.processing.BytesLoaderState;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysListener;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.util.PermissionsUtil;
import org.sufficientlysecure.keychain.util.FileHelper;
import org.sufficientlysecure.keychain.util.Log;

import java.io.IOException;

public class ImportKeysFileFragment extends Fragment {

    private Activity mActivity;
    private ImportKeysListener mCallback;

    private View mBrowse;
    private View mClipboardButton;

    private Uri mCurrentUri;

    private static final int REQUEST_CODE_FILE = 0x00007003;

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
                        Notify.create(mActivity, R.string.error_bad_data, Style.ERROR).show();
                        return;
                    }
                    mCallback.loadKeys(new BytesLoaderState(sendText.getBytes(), null));
                }
            }
        });

        setHasOptionsMenu(true);

        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mActivity = activity;

        try {
            mCallback = (ImportKeysListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement ImportKeysListener");
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.import_keys_file_fragment, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.import_all_keys:
                mCallback.importKeys();
                return true;
        }

        return false;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_FILE: {
                if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                    mCurrentUri = data.getData();

                    if (PermissionsUtil.checkAndRequestReadPermission(mActivity, mCurrentUri)) {
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
            isEncrypted = FileHelper.isEncryptedFile(mActivity, mCurrentUri);
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error opening file", e);

            Notify.create(mActivity, R.string.error_bad_data, Style.ERROR).show();
            return;
        }

        if (isEncrypted) {
            Intent intent = new Intent(mActivity, DecryptActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(mCurrentUri);
            startActivity(intent);
        } else {
            mCallback.loadKeys(new BytesLoaderState(null, mCurrentUri));
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        if (PermissionsUtil.checkReadPermissionResult(mActivity, requestCode, grantResults)) {
            startImportingKeys();
        } else {
            mActivity.setResult(Activity.RESULT_CANCELED);
            mActivity.finish();
        }
    }

}

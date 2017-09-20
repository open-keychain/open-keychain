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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
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

    @Override
    public View onCreateView(LayoutInflater i, ViewGroup c, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return null;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (ImportKeysListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement ImportKeysListener");
        }

        mActivity = (Activity) context;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.import_keys_file_fragment, menu);

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menu_import_keys_file_open:
                // open .asc or .gpg files
                // setting it to text/plain prevents Cyanogenmod's file manager from selecting asc
                // or gpg types!
                FileHelper.openDocument(ImportKeysFileFragment.this,
                        Uri.fromFile(Constants.Path.APP_DIR), "*/*", false, REQUEST_CODE_FILE);
                return true;
            case R.id.menu_import_keys_file_paste:
                importFromClipboard();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void importFromClipboard() {
        CharSequence clipboardText = ClipboardReflection.getClipboardText(getActivity());
        if (TextUtils.isEmpty(clipboardText)) {
            Notify.create(mActivity, R.string.error_clipboard_empty, Style.ERROR).show();
            return;
        }

        String keyText = PgpHelper.getPgpPublicKeyContent(clipboardText);
        if (keyText == null) {
            Notify.create(mActivity, R.string.error_clipboard_bad, Style.ERROR).show();
            return;
        }

        mCallback.loadKeys(new BytesLoaderState(keyText.getBytes(), null));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CODE_FILE: {
                if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                    mCurrentUri = data.getData();

                    if (PermissionsUtil.checkAndRequestReadPermission(this, mCurrentUri)) {
                        startImportingKeys();
                    }
                }
                break;
            }
            default:
                super.onActivityResult(requestCode, resultCode, data);
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

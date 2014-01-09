/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.Id;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.helper.FileHelper;
import org.sufficientlysecure.keychain.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.beardedhen.androidbootstrap.BootstrapButton;

public class ImportKeysFileFragment extends Fragment {
    public static final String ARG_PATH = "path";

    private ImportKeysActivity mImportActivity;
    private EditText mFilename;
    private BootstrapButton mBrowse;

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysFileFragment newInstance(String path) {
        ImportKeysFileFragment frag = new ImportKeysFileFragment();

        Bundle args = new Bundle();
        args.putString(ARG_PATH, path);

        frag.setArguments(args);
        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.import_keys_file_fragment, container, false);

        mFilename = (EditText) view.findViewById(R.id.import_keys_file_input);
        mBrowse = (BootstrapButton) view.findViewById(R.id.import_keys_file_browse);

        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // open .asc or .gpg files
                // setting it to text/plain prevents Cynaogenmod's file manager from selecting asc
                // or gpg types!
                FileHelper.openFile(ImportKeysFileFragment.this, mFilename.getText().toString(),
                        "*/*", Id.request.filename);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImportActivity = (ImportKeysActivity) getActivity();

        // set default path
        String path = Constants.path.APP_DIR + "/";
        if (getArguments() != null && getArguments().containsKey(ARG_PATH)) {
            path = getArguments().getString(ARG_PATH);
        }
        mFilename.setText(path);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case Id.request.filename: {
            if (resultCode == Activity.RESULT_OK && data != null) {
                String path = null;
                try {
                    path = data.getData().getPath();
                    Log.d(Constants.TAG, "path=" + path);

                    // set filename to edittext
                    mFilename.setText(path);
                } catch (NullPointerException e) {
                    Log.e(Constants.TAG, "Nullpointer while retrieving path!", e);
                }

                // load data
                mImportActivity.loadCallback(null, path);
            }

            break;
        }

        default:
            super.onActivityResult(requestCode, resultCode, data);

            break;
        }
    }

}

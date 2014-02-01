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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.beardedhen.androidbootstrap.BootstrapButton;

public class ImportKeysFileFragment extends Fragment {
    private ImportKeysActivity mImportActivity;
    private BootstrapButton mBrowse;

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

        mBrowse = (BootstrapButton) view.findViewById(R.id.import_keys_file_browse);

        mBrowse.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // open .asc or .gpg files
                // setting it to text/plain prevents Cynaogenmod's file manager from selecting asc
                // or gpg types!
                FileHelper.openFile(ImportKeysFileFragment.this, Constants.path.APP_DIR + "/",
                        "*/*", Id.request.filename);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImportActivity = (ImportKeysActivity) getActivity();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode & 0xFFFF) {
            case Id.request.filename: {
                if (resultCode == Activity.RESULT_OK && data != null) {

                    // load data
                    mImportActivity.loadCallback(null, data.getData(), null, null);
                }

                break;
            }

            default:
                super.onActivityResult(requestCode, resultCode, data);

                break;
        }
    }

}

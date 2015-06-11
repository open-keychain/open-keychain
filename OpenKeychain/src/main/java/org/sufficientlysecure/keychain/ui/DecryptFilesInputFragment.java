/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
 * Copyright (C) 2015 Vincent Breitmoser <look@my.amazin.horse>
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
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.FileHelper;

public class DecryptFilesInputFragment extends Fragment {
    public static final String ARG_URI = "uri";
    public static final String ARG_OPEN_DIRECTLY = "open_directly";

    private static final int REQUEST_CODE_INPUT = 0x00007003;

    private TextView mFilename;
    private View mDecryptButton;

    private Uri mInputUri = null;

    public static DecryptFilesInputFragment newInstance(Uri uri, boolean openDirectly) {
        DecryptFilesInputFragment frag = new DecryptFilesInputFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_URI, uri);
        args.putBoolean(ARG_OPEN_DIRECTLY, openDirectly);

        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_files_input_fragment, container, false);

        // hide result view for this fragment
        getActivity().findViewById(R.id.result_main_layout).setVisibility(View.GONE);

        mFilename = (TextView) view.findViewById(R.id.decrypt_files_filename);
        mDecryptButton = view.findViewById(R.id.decrypt_files_action_decrypt);
        view.findViewById(R.id.decrypt_files_browse).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    FileHelper.openDocument(DecryptFilesInputFragment.this, "*/*", REQUEST_CODE_INPUT);
                } else {
                    FileHelper.openFile(DecryptFilesInputFragment.this, mInputUri, "*/*",
                            REQUEST_CODE_INPUT);
                }
            }
        });
        mDecryptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptAction();
            }
        });

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ARG_URI, mInputUri);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle state = savedInstanceState != null ? savedInstanceState : getArguments();
        setInputUri(state.<Uri>getParcelable(ARG_URI));

        // should only come from args
        if (state.getBoolean(ARG_OPEN_DIRECTLY, false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                FileHelper.openDocument(DecryptFilesInputFragment.this, "*/*", REQUEST_CODE_INPUT);
            } else {
                FileHelper.openFile(DecryptFilesInputFragment.this, mInputUri, "*/*", REQUEST_CODE_INPUT);
            }
        }
    }

    private void setInputUri(Uri inputUri) {
        if (inputUri == null) {
            mInputUri = null;
            mFilename.setText("");
            return;
        }

        mInputUri = inputUri;
        mFilename.setText(FileHelper.getFilename(getActivity(), mInputUri));
    }

    private void decryptAction() {
        if (mInputUri == null) {
            Notify.create(getActivity(), R.string.no_file_selected, Notify.Style.ERROR).show();
            return;
        }

        DecryptFilesActivity activity = (DecryptFilesActivity) getActivity();
        activity.displayListFragment(mInputUri);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_INPUT) {
            return;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            setInputUri(data.getData());
        }
    }

}

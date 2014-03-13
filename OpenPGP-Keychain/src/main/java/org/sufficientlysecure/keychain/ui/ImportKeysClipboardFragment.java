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

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import com.beardedhen.androidbootstrap.BootstrapButton;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;

public class ImportKeysClipboardFragment extends Fragment {

    private ImportKeysActivity mImportActivity;
    private BootstrapButton mButton;

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysClipboardFragment newInstance() {
        ImportKeysClipboardFragment frag = new ImportKeysClipboardFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.import_keys_clipboard_fragment, container, false);

        mButton = (BootstrapButton) view.findViewById(R.id.import_clipboard_button);
        mButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                CharSequence clipboardText = ClipboardReflection.getClipboardText(getActivity());
                String sendText = "";
                if (clipboardText != null) {
                    sendText = clipboardText.toString();
                }
                mImportActivity.loadCallback(sendText.getBytes(), null, null, null);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mImportActivity = (ImportKeysActivity) getActivity();
    }

}

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

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.beardedhen.androidbootstrap.BootstrapButton;

import org.sufficientlysecure.keychain.R;

public class ImportKeysNFCFragment extends Fragment {

    private BootstrapButton mButton;

    /**
     * Creates new instance of this fragment
     */
    public static ImportKeysNFCFragment newInstance() {
        ImportKeysNFCFragment frag = new ImportKeysNFCFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.import_keys_nfc_fragment, container, false);

        mButton = (BootstrapButton) view.findViewById(R.id.import_nfc_button);
        mButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                // show nfc help
                Intent intent = new Intent(getActivity(), HelpActivity.class);
                intent.putExtra(HelpActivity.EXTRA_SELECTED_TAB, 2);
                startActivityForResult(intent, 0);
            }
        });

        return view;
    }

}

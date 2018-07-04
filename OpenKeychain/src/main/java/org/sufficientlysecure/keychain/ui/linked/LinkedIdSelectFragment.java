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

package org.sufficientlysecure.keychain.ui.linked;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;


public class LinkedIdSelectFragment extends Fragment {
    public static LinkedIdSelectFragment newInstance() {
        return new LinkedIdSelectFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.linked_select_fragment, container, false);

        view.findViewById(R.id.linked_create_https_button).setOnClickListener(v -> {
            LinkedIdCreateHttpsStep1Fragment frag = LinkedIdCreateHttpsStep1Fragment.newInstance();
            ((LinkedIdWizard) requireActivity()).loadFragment(frag, LinkedIdWizard.FRAG_ACTION_TO_RIGHT);
        });

        view.findViewById(R.id.linked_create_twitter_button).setOnClickListener(v -> {
            LinkedIdCreateTwitterStep1Fragment frag = LinkedIdCreateTwitterStep1Fragment.newInstance();
            ((LinkedIdWizard) requireActivity()).loadFragment(frag, LinkedIdWizard.FRAG_ACTION_TO_RIGHT);
        });

        view.findViewById(R.id.linked_create_github_button).setOnClickListener(v -> {
            LinkedIdCreateGithubFragment frag = LinkedIdCreateGithubFragment.newInstance();
            ((LinkedIdWizard) requireActivity()).loadFragment(frag, LinkedIdWizard.FRAG_ACTION_TO_RIGHT);
        });


        return view;
    }

}

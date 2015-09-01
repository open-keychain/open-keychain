/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;

public class LinkedIdSelectFragment extends Fragment {

    LinkedIdWizard mLinkedIdWizard;

    /**
     * Creates new instance of this fragment
     */
    public static LinkedIdSelectFragment newInstance() {
        LinkedIdSelectFragment frag = new LinkedIdSelectFragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.linked_select_fragment, container, false);

        view.findViewById(R.id.linked_create_https_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinkedIdCreateHttpsStep1Fragment frag =
                                LinkedIdCreateHttpsStep1Fragment.newInstance();

                        mLinkedIdWizard.loadFragment(null, frag, LinkedIdWizard.FRAG_ACTION_TO_RIGHT);
                    }
                });

        /*
        view.findViewById(R.id.linked_create_dns_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinkedIdCreateDnsStep1Fragment frag =
                                LinkedIdCreateDnsStep1Fragment.newInstance();

                        mLinkedIdWizard.loadFragment(null, frag, LinkedIdWizard.FRAG_ACTION_TO_RIGHT);
                    }
                });
        */

        view.findViewById(R.id.linked_create_twitter_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinkedIdCreateTwitterStep1Fragment frag =
                                LinkedIdCreateTwitterStep1Fragment.newInstance();

                        mLinkedIdWizard.loadFragment(null, frag, LinkedIdWizard.FRAG_ACTION_TO_RIGHT);
                    }
                });

        view.findViewById(R.id.linked_create_github_button)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinkedIdCreateGithubFragment frag =
                                LinkedIdCreateGithubFragment.newInstance();

                        mLinkedIdWizard.loadFragment(null, frag, LinkedIdWizard.FRAG_ACTION_TO_RIGHT);
                    }
                });


        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLinkedIdWizard = (LinkedIdWizard) getActivity();
    }

}

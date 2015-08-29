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

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.util.Notify;


public class LinkedIdCreateGithubStep1Fragment extends Fragment {

    LinkedIdWizard mLinkedIdWizard;

    EditText mEditHandle;

    public static LinkedIdCreateGithubStep1Fragment newInstance() {
        LinkedIdCreateGithubStep1Fragment frag = new LinkedIdCreateGithubStep1Fragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mLinkedIdWizard = (LinkedIdWizard) getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.linked_create_github_fragment_step1, container, false);

        view.findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                final String handle = mEditHandle.getText().toString();

                new AsyncTask<Void,Void,Boolean>() {

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        return true; // return checkHandle(handle);
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);

                        if (result == null) {
                            Notify.create(getActivity(),
                                    "Connection error while checking username!", Notify.Style.ERROR).show();
                            return;
                        }

                        if (!result) {
                            Notify.create(getActivity(),
                                    "This handle does not exist on Github!", Notify.Style.ERROR).show();
                            return;
                        }

                        LinkedIdCreateGithubStep2Fragment frag =
                                LinkedIdCreateGithubStep2Fragment.newInstance(handle);

                        mLinkedIdWizard.loadFragment(null, frag, LinkedIdWizard.FRAG_ACTION_TO_RIGHT);
                    }
                }.execute();

            }
        });

        view.findViewById(R.id.back_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mLinkedIdWizard.loadFragment(null, null, LinkedIdWizard.FRAG_ACTION_TO_LEFT);
            }
        });

        mEditHandle = (EditText) view.findViewById(R.id.linked_create_github_handle);

        return view;
    }

    /* not used at this point, too much hassle
    private static Boolean checkHandle(String handle) {
        try {
            HttpURLConnection nection =
                    (HttpURLConnection) new URL("https://api.github.com/" + handle).openConnection();
            nection.setRequestMethod("HEAD");
            return nection.getResponseCode() == 200;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
    */

}

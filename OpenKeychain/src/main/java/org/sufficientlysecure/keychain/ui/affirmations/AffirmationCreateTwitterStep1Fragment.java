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

package org.sufficientlysecure.keychain.ui.affirmations;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.affirmation.LinkedIdentity;
import org.sufficientlysecure.keychain.pgp.affirmation.resources.GenericHttpsResource;
import org.sufficientlysecure.keychain.pgp.affirmation.resources.TwitterResource;
import org.sufficientlysecure.keychain.ui.CreateKeyActivity;
import org.sufficientlysecure.keychain.ui.util.Notify;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

public class AffirmationCreateTwitterStep1Fragment extends Fragment {

    AffirmationWizard mAffirmationWizard;

    EditText mEditHandle;

    /**
     * Creates new instance of this fragment
     */
    public static AffirmationCreateTwitterStep1Fragment newInstance() {
        AffirmationCreateTwitterStep1Fragment frag = new AffirmationCreateTwitterStep1Fragment();

        Bundle args = new Bundle();
        frag.setArguments(args);

        return frag;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mAffirmationWizard = (AffirmationWizard) getActivity();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.affirmation_create_twitter_fragment_step1, container, false);

        view.findViewById(R.id.next_button).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                final String handle = mEditHandle.getText().toString();

                new AsyncTask<Void,Void,Boolean>() {

                    @Override
                    protected Boolean doInBackground(Void... params) {
                        return true; // checkHandle(handle);
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        super.onPostExecute(result);

                        if (result == null) {
                            Notify.showNotify(getActivity(), "Connection error while checking username!", Notify.Style.ERROR);
                            return;
                        }

                        if (!result) {
                            Notify.showNotify(getActivity(), "This handle does not exist on Twitter!", Notify.Style.ERROR);
                            return;
                        }

                        String proofNonce = LinkedIdentity.generateNonce();
                        String proofText = TwitterResource.generateText(getActivity(),
                                mAffirmationWizard.mFingerprint, proofNonce);

                        AffirmationCreateTwitterStep2Fragment frag =
                                AffirmationCreateTwitterStep2Fragment.newInstance(handle, proofNonce, proofText);

                        mAffirmationWizard.loadFragment(null, frag, AffirmationWizard.FRAG_ACTION_TO_RIGHT);
                    }
                }.execute();

            }
        });

        view.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mAffirmationWizard.loadFragment(null, null, AffirmationWizard.FRAG_ACTION_TO_LEFT);
            }
        });

        mEditHandle = (EditText) view.findViewById(R.id.linked_create_twitter_handle);
        mEditHandle.setText("Valodim");

        return view;
    }

    private static Boolean checkHandle(String handle) {
        try {
            HttpURLConnection nection =
                    (HttpURLConnection) new URL("https://twitter.com/" + handle).openConnection();
            nection.setRequestMethod("HEAD");
            return nection.getResponseCode() == 200;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}

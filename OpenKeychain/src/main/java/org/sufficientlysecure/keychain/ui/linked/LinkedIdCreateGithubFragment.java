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


import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import javax.net.ssl.HttpsURLConnection;
import org.json.JSONException;
import org.json.JSONObject;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.linked.LinkedAttribute;
import org.sufficientlysecure.keychain.linked.resources.GithubResource;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.widget.StatusIndicator;
import org.sufficientlysecure.keychain.util.Log;


public class LinkedIdCreateGithubFragment extends CryptoOperationFragment<SaveKeyringParcel,EditKeyResult> {

    ViewAnimator mButtonContainer;

    StatusIndicator mStatus1, mStatus2, mStatus3;

    byte[] mFingerprint;
    long mMasterKeyId;
    private SaveKeyringParcel mSaveKeyringParcel;

    public static LinkedIdCreateGithubFragment newInstance() {
        return new LinkedIdCreateGithubFragment();
    }

    public LinkedIdCreateGithubFragment() {
        super(null);
    }

    @Override @NonNull
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.linked_create_github_fragment, container, false);

        mButtonContainer = (ViewAnimator) view.findViewById(R.id.button_container);

        mStatus1 = (StatusIndicator) view.findViewById(R.id.linked_status_step1);
        mStatus2 = (StatusIndicator) view.findViewById(R.id.linked_status_step2);
        mStatus3 = (StatusIndicator) view.findViewById(R.id.linked_status_step3);

        view.findViewById(R.id.button_send).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                step1GetOAuthToken();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        LinkedIdWizard wizard = (LinkedIdWizard) getActivity();
        mFingerprint = wizard.mFingerprint;
        mMasterKeyId = wizard.mMasterKeyId;

        final String oAuthCode = wizard.oAuthGetCode();
        final String oAuthState = wizard.oAuthGetState();
        if (oAuthCode == null) {
            Log.d(Constants.TAG, "no code");
            return;
        }

        final String gistText = GithubResource.generate(wizard, mFingerprint);

        Log.d(Constants.TAG, "got code: " + oAuthCode);

        new AsyncTask<Void,Void,JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... dummy) {
                try {

                    long timer = System.currentTimeMillis();

                    JSONObject params = new JSONObject();
                    params.put("client_id", "7a011b66275f244d3f21");
                    params.put("client_secret", "eaced8a6655719d8c6848396de97b3f5d7a89fec");
                    params.put("code", oAuthCode);
                    params.put("state", oAuthState);

                    JSONObject result = jsonHttpRequest("https://github.com/login/oauth/access_token", params, null);

                    // ux flow: this operation should take at last a second
                    timer = System.currentTimeMillis() -timer;
                    if (timer < 1000) try {
                        Thread.sleep(1000 -timer);
                    } catch (InterruptedException e) {
                        // never mind
                    }

                    return result;

                } catch (IOException e) {
                    Log.e(Constants.TAG, "error in request", e);
                } catch (JSONException e) {
                    throw new AssertionError("json error, this is a bug!");
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                super.onPostExecute(result);

                Log.d(Constants.TAG, "response: " + result);

                if (result == null || !result.has("access_token")) {
                    mStatus1.setDisplayedChild(3);
                    return;
                }

                mStatus1.setDisplayedChild(2);
                step2PostGist(result.optString("access_token"), gistText);

            }
        }.execute();

    }

    private void step1GetOAuthToken() {

        mStatus1.setDisplayedChild(1);
        mStatus2.setDisplayedChild(0);
        mStatus3.setDisplayedChild(0);

        mButtonContainer.setDisplayedChild(1);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                LinkedIdWizard wizard = (LinkedIdWizard) getActivity();
                if (wizard == null) {
                    return;
                }
                wizard.oAuthRequest("github.com/login/oauth/authorize", "7a011b66275f244d3f21", "gist");

            }
        }, 250);

    }

    private void step2PostGist(final String accessToken, final String gistText) {

        mStatus2.setDisplayedChild(1);

        new AsyncTask<Void,Void,JSONObject>() {
            @Override
            protected JSONObject doInBackground(Void... dummy) {
                try {

                    long timer = System.currentTimeMillis();

                    JSONObject file = new JSONObject();
                    file.put("content", gistText);

                    JSONObject files = new JSONObject();
                    files.put("file1.txt", file);

                    JSONObject params = new JSONObject();
                    params.put("public", true);
                    params.put("description", "OpenKeychain API Tests");
                    params.put("files", files);

                    JSONObject result = jsonHttpRequest("https://api.github.com/gists", params, accessToken);

                    // ux flow: this operation should take at last a second
                    timer = System.currentTimeMillis() -timer;
                    if (timer < 1000) try {
                        Thread.sleep(1000 -timer);
                    } catch (InterruptedException e) {
                        // never mind
                    }

                    return result;

                } catch (IOException e) {
                    Log.e(Constants.TAG, "error in request", e);
                } catch (JSONException e) {
                    throw new AssertionError("json error, this is a bug!");
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                super.onPostExecute(result);

                Log.d(Constants.TAG, "response: " + result);

                if (result == null) {
                    mStatus2.setDisplayedChild(3);
                    return;
                }

                try {
                    String gistId = result.getString("id");
                    JSONObject owner = result.getJSONObject("owner");
                    String gistLogin = owner.getString("login");

                    URI uri = URI.create("https://gist.github.com/" + gistLogin + "/" + gistId);
                    GithubResource resource = GithubResource.create(uri);

                    mStatus2.setDisplayedChild(2);
                    step3EditKey(resource);

                } catch (JSONException e) {
                    mStatus2.setDisplayedChild(3);
                    e.printStackTrace();
                }

            }
        }.execute();

    }

    private void step3EditKey(final GithubResource resource) {

        mStatus3.setDisplayedChild(1);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                WrappedUserAttribute ua = LinkedAttribute.fromResource(resource).toUserAttribute();
                mSaveKeyringParcel = new SaveKeyringParcel(mMasterKeyId, mFingerprint);
                mSaveKeyringParcel.mAddUserAttribute.add(ua);

                cryptoOperation();

            }
        }, 250);

    }

    @Nullable
    @Override
    public SaveKeyringParcel createOperationInput() {
        // if this is null, the cryptoOperation silently aborts - which is what we want in that case
        return mSaveKeyringParcel;
    }

    @Override
    public void onCryptoOperationSuccess(EditKeyResult result) {
        mStatus3.setDisplayedChild(2);
    }

    @Override
    public void onCryptoOperationError(EditKeyResult result) {
        result.createNotify(getActivity()).show(this);
        mStatus3.setDisplayedChild(3);
    }

    @Override
    public void onCryptoOperationCancelled() {
        super.onCryptoOperationCancelled();
        mStatus3.setDisplayedChild(3);
    }

    private static JSONObject jsonHttpRequest(String url, JSONObject params, String accessToken)
            throws IOException {

        HttpsURLConnection nection = (HttpsURLConnection) new URL(url).openConnection();
        nection.setDoInput(true);
        nection.setDoOutput(true);
        nection.setRequestProperty("Content-Type", "application/json");
        nection.setRequestProperty("Accept", "application/json");
        nection.setRequestProperty("User-Agent", "OpenKeychain " + BuildConfig.VERSION_NAME);
        if (accessToken != null) {
            nection.setRequestProperty("Authorization", "token " + accessToken);
        }

        OutputStream os = nection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(params.toString());
        writer.flush();
        writer.close();
        os.close();

        try {

            nection.connect();
            InputStream in = new BufferedInputStream(nection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder response = new StringBuilder();
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                response.append(line);
            }

            try {
                return new JSONObject(response.toString());
            } catch (JSONException e) {
                throw new IOException(e);
            }

        } finally {
            nection.disconnect();
        }

    }

}

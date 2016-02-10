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
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.util.Random;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentActivity;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import javax.net.ssl.HttpsURLConnection;
import org.json.JSONException;
import org.json.JSONObject;
import org.bouncycastle.util.encoders.Hex;
import org.sufficientlysecure.keychain.BuildConfig;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.linked.LinkedAttribute;
import org.sufficientlysecure.keychain.linked.resources.GithubResource;
import org.sufficientlysecure.keychain.operations.results.EditKeyResult;
import org.sufficientlysecure.keychain.pgp.WrappedUserAttribute;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel;
import org.sufficientlysecure.keychain.ui.ViewKeyActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.widget.StatusIndicator;
import org.sufficientlysecure.keychain.ui.widget.StatusIndicator.Status;
import org.sufficientlysecure.keychain.util.Log;


public class LinkedIdCreateGithubFragment extends CryptoOperationFragment<SaveKeyringParcel,EditKeyResult> {

    public static final String ARG_GITHUB_COOKIE = "github_cookie";
    private Button mRetryButton;

    enum State {
        IDLE, AUTH_PROCESS, AUTH_ERROR, POST_PROCESS, POST_ERROR, LID_PROCESS, LID_ERROR, DONE
    }

    ViewAnimator mButtonContainer;

    StatusIndicator mStatus1, mStatus2, mStatus3;

    byte[] mFingerprint;
    long mMasterKeyId;
    private SaveKeyringParcel mSaveKeyringParcel;
    private TextView mLinkedIdTitle, mLinkedIdComment;
    private boolean mFinishOnStop;

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

        mRetryButton = (Button) view.findViewById(R.id.button_retry);

        ((ImageView) view.findViewById(R.id.linked_id_type_icon)).setImageResource(R.drawable.linked_github);
        ((ImageView) view.findViewById(R.id.linked_id_certified_icon)).setImageResource(R.drawable.octo_link_24dp);
        mLinkedIdTitle = (TextView) view.findViewById(R.id.linked_id_title);
        mLinkedIdComment = (TextView) view.findViewById(R.id.linked_id_comment);

        view.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LinkedIdWizard activity = (LinkedIdWizard) getActivity();
                if (activity == null) {
                    return;
                }
                activity.loadFragment(null, null, LinkedIdWizard.FRAG_ACTION_TO_LEFT);
            }
        });

        view.findViewById(R.id.button_send).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                step1GetOAuthCode();
                // for animation testing
                // onCryptoOperationSuccess(null);
            }
        });

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        LinkedIdWizard wizard = (LinkedIdWizard) getActivity();
        mFingerprint = wizard.mFingerprint;
        mMasterKeyId = wizard.mMasterKeyId;
    }

    private void step1GetOAuthCode() {

        setState(State.AUTH_PROCESS);

        mButtonContainer.setDisplayedChild(1);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                oAuthRequest("github.com/login/oauth/authorize", BuildConfig.GITHUB_CLIENT_ID, "gist");
            }
        }, 300);

    }

    private void showRetryForOAuth() {

        mRetryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setOnClickListener(null);
                step1GetOAuthCode();
            }
        });
        mButtonContainer.setDisplayedChild(3);

    }

    private void step1GetOAuthToken() {

        if (mOAuthCode == null) {
            setState(State.AUTH_ERROR);
            showRetryForOAuth();
            return;
        }

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final String gistText = GithubResource.generate(activity, mFingerprint);

        new AsyncTask<Void,Void,JSONObject>() {

            Exception mException;

            @Override
            protected JSONObject doInBackground(Void... dummy) {
                try {

                    JSONObject params = new JSONObject();
                    params.put("client_id", BuildConfig.GITHUB_CLIENT_ID);
                    params.put("client_secret", BuildConfig.GITHUB_CLIENT_SECRET);
                    params.put("code", mOAuthCode);
                    params.put("state", mOAuthState);

                    return jsonHttpRequest("https://github.com/login/oauth/access_token", params, null);

                } catch (IOException | HttpResultException e) {
                    mException = e;
                } catch (JSONException e) {
                    throw new AssertionError("json error, this is a bug!");
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                super.onPostExecute(result);

                Activity activity = getActivity();
                if (activity == null) {
                    // we couldn't show an error anyways
                    return;
                }

                Log.d(Constants.TAG, "response: " + result);

                if (result == null || result.optString("access_token", null) == null) {
                    setState(State.AUTH_ERROR);
                    showRetryForOAuth();

                    if (result != null) {
                        Notify.create(activity, R.string.linked_error_auth_failed, Style.ERROR).show();
                        return;
                    }

                    if (mException instanceof SocketTimeoutException) {
                        Notify.create(activity, R.string.linked_error_timeout, Style.ERROR).show();
                    } else if (mException instanceof HttpResultException) {
                        Notify.create(activity, activity.getString(R.string.linked_error_http,
                                        ((HttpResultException) mException).mResponse),
                                Style.ERROR).show();
                    } else if (mException instanceof IOException) {
                        Notify.create(activity, R.string.linked_error_network, Style.ERROR).show();
                    }

                    return;
                }

                step2PostGist(result.optString("access_token"), gistText);

            }
        }.execute();

    }

    private void step2PostGist(final String accessToken, final String gistText) {

        setState(State.POST_PROCESS);

        new AsyncTask<Void,Void,JSONObject>() {

            Exception mException;

            @Override
            protected JSONObject doInBackground(Void... dummy) {
                try {

                    long timer = System.currentTimeMillis();

                    JSONObject file = new JSONObject();
                    file.put("content", gistText);

                    JSONObject files = new JSONObject();
                    files.put("openpgp.txt", file);

                    JSONObject params = new JSONObject();
                    params.put("public", true);
                    params.put("description", getString(R.string.linked_gist_description));
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

                } catch (IOException | HttpResultException e) {
                    mException = e;
                } catch (JSONException e) {
                    throw new AssertionError("json error, this is a bug!");
                }
                return null;
            }

            @Override
            protected void onPostExecute(JSONObject result) {
                super.onPostExecute(result);

                Log.d(Constants.TAG, "response: " + result);

                Activity activity = getActivity();
                if (activity == null) {
                    // we couldn't show an error anyways
                    return;
                }

                if (result == null) {
                    setState(State.POST_ERROR);
                    showRetryForOAuth();

                    if (mException instanceof SocketTimeoutException) {
                        Notify.create(activity, R.string.linked_error_timeout, Style.ERROR).show();
                    } else if (mException instanceof HttpResultException) {
                        Notify.create(activity, activity.getString(R.string.linked_error_http,
                                        ((HttpResultException) mException).mResponse),
                                Style.ERROR).show();
                    } else if (mException instanceof IOException) {
                        Notify.create(activity, R.string.linked_error_network, Style.ERROR).show();
                    }

                    return;
                }

                GithubResource resource;

                try {
                    String gistId = result.getString("id");
                    JSONObject owner = result.getJSONObject("owner");
                    String gistLogin = owner.getString("login");

                    URI uri = URI.create("https://gist.github.com/" + gistLogin + "/" + gistId);
                    resource = GithubResource.create(uri);
                } catch (JSONException e) {
                    setState(State.POST_ERROR);
                    return;
                }

                View linkedItem = mButtonContainer.getChildAt(2);
                if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                    linkedItem.setTransitionName(resource.toUri().toString());
                }

                // we only need authorization for this one operation, drop it afterwards
                revokeToken(accessToken);

                step3EditKey(resource);
            }

        }.execute();

    }

    private void revokeToken(final String token) {

        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... dummy) {
                try {
                    HttpsURLConnection nection = (HttpsURLConnection) new URL(
                            "https://api.github.com/applications/" + BuildConfig.GITHUB_CLIENT_ID + "/tokens/" + token)
                            .openConnection();
                    nection.setRequestMethod("DELETE");
                    String encoded = Base64.encodeToString(
                            (BuildConfig.GITHUB_CLIENT_ID + ":" + BuildConfig.GITHUB_CLIENT_SECRET).getBytes(), Base64.DEFAULT);
                    nection.setRequestProperty("Authorization", "Basic " + encoded);
                    nection.connect();
                } catch (IOException e) {
                    // nvm
                }
                return null;
            }
        }.execute();

    }

    private void step3EditKey(final GithubResource resource) {

        // set item data while we're there
        {
            Context context = getActivity();
            mLinkedIdTitle.setText(resource.getDisplayTitle(context));
            mLinkedIdComment.setText(resource.getDisplayComment(context));
        }

        setState(State.LID_PROCESS);

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

        setState(State.DONE);

        mButtonContainer.getInAnimation().setDuration(750);
        mButtonContainer.setDisplayedChild(2);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                FragmentActivity activity = getActivity();
                Intent intent = new Intent(activity, ViewKeyActivity.class);
                intent.setData(KeyRings.buildGenericKeyRingUri(mMasterKeyId));
                // intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    intent.putExtra(ViewKeyActivity.EXTRA_LINKED_TRANSITION, true);
                    View linkedItem = mButtonContainer.getChildAt(2);

                    Bundle options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                            activity, linkedItem, linkedItem.getTransitionName()).toBundle();
                    activity.startActivity(intent, options);
                    mFinishOnStop = true;
                } else {
                    activity.startActivity(intent);
                    activity.finish();
                }
            }
        }, 1000);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        // cookies are automatically saved, we don't want that
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie("https://github.com/");
        outState.putString(ARG_GITHUB_COOKIE, cookie);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            String cookie = savedInstanceState.getString(ARG_GITHUB_COOKIE);
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setCookie("https://github.com/", cookie);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        try {
            // cookies are automatically saved, we don't want that
            CookieManager cookieManager = CookieManager.getInstance();
            // noinspection deprecation (replacement is api lvl 21)
            cookieManager.removeAllCookie();
        } catch (Exception e) {
            // no biggie if this fails
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mFinishOnStop) {
            Activity activity = getActivity();
            activity.setResult(Activity.RESULT_OK);
            activity.finish();
        }
    }

    @Override
    public void onCryptoOperationError(EditKeyResult result) {
        result.createNotify(getActivity()).show(this);
        setState(State.LID_ERROR);
    }

    @Override
    public void onCryptoOperationCancelled() {
        mRetryButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                v.setOnClickListener(null);
                mButtonContainer.setDisplayedChild(1);
                setState(State.LID_PROCESS);
                cryptoOperation();
            }
        });
        mButtonContainer.setDisplayedChild(3);
        setState(State.LID_ERROR);
    }

    private String mOAuthCode, mOAuthState;

    public void oAuthRequest(String hostAndPath, String clientId, String scope) {

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        byte[] buf = new byte[16];
        new Random().nextBytes(buf);
        mOAuthState = new String(Hex.encode(buf));
        mOAuthCode = null;

        final Dialog auth_dialog = new Dialog(activity);
        auth_dialog.setContentView(R.layout.oauth_webview);
        WebView web = (WebView) auth_dialog.findViewById(R.id.web_view);
        web.getSettings().setSaveFormData(false);
        web.getSettings().setUserAgentString("OpenKeychain " + BuildConfig.VERSION_NAME);
        web.setWebViewClient(new WebViewClient() {

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri uri = Uri.parse(url);
                if ("oauth-openkeychain".equals(uri.getScheme())) {

                    if (mOAuthCode != null) {
                        return true;
                    }

                    if (uri.getQueryParameter("error") != null) {
                        Log.i(Constants.TAG, "got oauth error: " + uri.getQueryParameter("error"));
                        auth_dialog.dismiss();
                        return true;
                    }

                    // check if mOAuthState == queryParam[state]
                    mOAuthCode = uri.getQueryParameter("code");

                    auth_dialog.dismiss();
                    return true;
                }
                // don't surf away from github!
                if (!"github.com".equals(uri.getHost())) {
                    auth_dialog.dismiss();
                    return true;
                }
                return false;
            }

        });

        auth_dialog.setTitle(R.string.linked_webview_title_github);
        auth_dialog.setCancelable(true);
        auth_dialog.setOnDismissListener(new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                step1GetOAuthToken();
            }
        });
        auth_dialog.show();

        web.loadUrl("https://" + hostAndPath +
                "?client_id=" + clientId +
                "&scope=" + scope +
                "&redirect_uri=oauth-openkeychain://linked/" +
                "&state=" + mOAuthState);

    }

    public void setState(State state) {
        switch (state) {
            case IDLE:
                mStatus1.setDisplayedChild(Status.IDLE);
                mStatus2.setDisplayedChild(Status.IDLE);
                mStatus3.setDisplayedChild(Status.IDLE);
                break;
            case AUTH_PROCESS:
                mStatus1.setDisplayedChild(Status.PROGRESS);
                mStatus2.setDisplayedChild(Status.IDLE);
                mStatus3.setDisplayedChild(Status.IDLE);
                break;
            case AUTH_ERROR:
                mStatus1.setDisplayedChild(Status.ERROR);
                mStatus2.setDisplayedChild(Status.IDLE);
                mStatus3.setDisplayedChild(Status.IDLE);
                break;
            case POST_PROCESS:
                mStatus1.setDisplayedChild(Status.OK);
                mStatus2.setDisplayedChild(Status.PROGRESS);
                mStatus3.setDisplayedChild(Status.IDLE);
                break;
            case POST_ERROR:
                mStatus1.setDisplayedChild(Status.OK);
                mStatus2.setDisplayedChild(Status.ERROR);
                mStatus3.setDisplayedChild(Status.IDLE);
                break;
            case LID_PROCESS:
                mStatus1.setDisplayedChild(Status.OK);
                mStatus2.setDisplayedChild(Status.OK);
                mStatus3.setDisplayedChild(Status.PROGRESS);
                break;
            case LID_ERROR:
                mStatus1.setDisplayedChild(Status.OK);
                mStatus2.setDisplayedChild(Status.OK);
                mStatus3.setDisplayedChild(Status.ERROR);
                break;
            case DONE:
                mStatus1.setDisplayedChild(Status.OK);
                mStatus2.setDisplayedChild(Status.OK);
                mStatus3.setDisplayedChild(Status.OK);
        }
    }

    private static JSONObject jsonHttpRequest(String url, JSONObject params, String accessToken)
            throws IOException, HttpResultException {

        HttpsURLConnection nection = (HttpsURLConnection) new URL(url).openConnection();
        nection.setDoInput(true);
        nection.setDoOutput(true);
        nection.setConnectTimeout(2000);
        nection.setReadTimeout(1000);
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

            int code = nection.getResponseCode();
            if (code != HttpsURLConnection.HTTP_CREATED && code != HttpsURLConnection.HTTP_OK) {
                throw new HttpResultException(nection.getResponseCode(), nection.getResponseMessage());
            }

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

    static class HttpResultException extends Exception {
        final int mCode;
        final String mResponse;

        HttpResultException(int code, String response) {
            mCode = code;
            mResponse = response;
        }

    }

}

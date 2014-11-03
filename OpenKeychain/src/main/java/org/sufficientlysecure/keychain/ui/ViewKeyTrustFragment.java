/*
 * Copyright (C) 2014 Tim Bray <tbray@textuality.com>
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

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.Proof;
import com.textuality.keybase.lib.User;

import org.spongycastle.i18n.MessageBundle;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.KeychainIntentService;
import org.sufficientlysecure.keychain.service.KeychainIntentServiceHandler;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

public class ViewKeyTrustFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private View mStartSearch;
    private TextView mTrustReadout;
    private WebView mSearchReport;

    // for retrieval by provers
    private final Hashtable<String, Proof> mProofs = new Hashtable<String, Proof>();

    // remember the evidence for return from a proof screen
    private final Hashtable<CharSequence, String> mHTML = new Hashtable<CharSequence, String>();

    private static final int LOADER_ID_DATABASE = 1;

    private static final String VERIFY_HOST = "checkproof.sufficientlysecure.org";
    private static final String PROVE_PATH = "/prove/";
    private static final String EVIDENCE_PATH = "/evidence/";

    // for retrieving the key we’re working on
    private Uri mDataUri;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_trust_fragment, getContainer());

        mTrustReadout = (TextView) view.findViewById(R.id.view_key_trust_readout);
        mStartSearch = view.findViewById(R.id.view_key_trust_search_cloud);
        mStartSearch.setEnabled(false);
        mSearchReport = (WebView) view.findViewById(R.id.view_key_trust_cloud_result);
        mSearchReport.setWebViewClient(new ControlDisplay());
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Uri dataUri = getArguments().getParcelable(ViewKeyMainFragment.ARG_DATA_URI);
        if (dataUri == null) {
            Log.e(Constants.TAG, "Data missing. Should be Uri of key!");
            getActivity().finish();
            return;
        }
        mDataUri = dataUri;

        // retrieve the key from the database
        getLoaderManager().initLoader(LOADER_ID_DATABASE, null, this);
    }

    static final String[] TRUST_PROJECTION = new String[]{
            KeyRings._ID, KeyRings.FINGERPRINT, KeyRings.IS_REVOKED, KeyRings.EXPIRY,
            KeyRings.HAS_ANY_SECRET, KeyRings.VERIFIED
    };
    static final int INDEX_TRUST_FINGERPRINT = 1;
    static final int INDEX_TRUST_IS_REVOKED = 2;
    static final int INDEX_TRUST_EXPIRY = 3;
    static final int INDEX_UNIFIED_HAS_ANY_SECRET = 4;
    static final int INDEX_VERIFIED = 5;

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        setContentShown(false);

        switch (id) {
            case LOADER_ID_DATABASE: {
                Uri baseUri = KeyRings.buildUnifiedKeyRingUri(mDataUri);
                return new CursorLoader(getActivity(), baseUri, TRUST_PROJECTION, null, null, null);
            }
            // decided to just use an AsyncTask for keybase, but maybe later
            default:
                return null;
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        /* TODO better error handling? May cause problems when a key is deleted,
         * because the notification triggers faster than the activity closes.
         */
        // Avoid NullPointerExceptions...
        if (data.getCount() == 0) {
            return;
        }

        boolean nothingSpecial = true;
        StringBuilder message = new StringBuilder();

        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        if (data.moveToFirst()) {

            if (data.getInt(INDEX_UNIFIED_HAS_ANY_SECRET) != 0) {
                message.append(getString(R.string.key_trust_it_is_yours)).append("\n");
                nothingSpecial = false;
            } else if (data.getInt(INDEX_VERIFIED) != 0) {
                message.append(getString(R.string.key_trust_already_verified)).append("\n");
                nothingSpecial = false;
            }

            // If this key is revoked, don’t trust it!
            if (data.getInt(INDEX_TRUST_IS_REVOKED) != 0) {
                message.append(getString(R.string.key_trust_revoked)).
                        append(getString(R.string.key_trust_old_keys));

                nothingSpecial = false;
            } else {
                Date expiryDate = new Date(data.getLong(INDEX_TRUST_EXPIRY) * 1000);
                if (!data.isNull(INDEX_TRUST_EXPIRY) && expiryDate.before(new Date())) {

                    // if expired, don’t trust it!
                    message.append(getString(R.string.key_trust_expired)).
                            append(getString(R.string.key_trust_old_keys));

                    nothingSpecial = false;
                }
            }

            if (nothingSpecial) {
                message.append(getString(R.string.key_trust_maybe));
            }

            final byte[] fp = data.getBlob(INDEX_TRUST_FINGERPRINT);
            final String fingerprint = KeyFormattingUtils.convertFingerprintToHex(fp);
            if (fingerprint != null) {

                mStartSearch.setEnabled(true);
                mStartSearch.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        new DescribeKey().execute(fingerprint);
                    }
                });
            }
        }

        mTrustReadout.setText(message);
        setContentShown(true);
    }

    /**
     * This is called when the last Cursor provided to onLoadFinished() above is about to be closed.
     * We need to make sure we are no longer using it.
     */
    public void onLoaderReset(Loader<Cursor> loader) {
        // no-op in this case I think
    }

    // look for evidence from keybase in the background, make HTML version of results,
    //  display in a WebView
    private class DescribeKey extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... args) {
            String fingerprint = args[0];
            StringBuilder sb = new StringBuilder("<html><body><p>");
            sb.append(getActivity().getString(R.string.key_trust_results_prefix)).append("</p>");
            sb.append("<ul>");
            final Hashtable<Integer, ArrayList<Proof>> proofs = new Hashtable<Integer, ArrayList<Proof>>();
            try {
                User keybaseUser = User.findByFingerprint(fingerprint);
                for (Proof proof : keybaseUser.getProofs()) {
                    Integer proofType = proof.getType();
                    appendIfOK(proofs, proofType, proof);

                    // remember proof in case they want to verify it
                    mProofs.put(proof.getId(), proof);
                }

                // a one-liner in a modern programming language
                for (Integer proofType : proofs.keySet()) {
                    Proof[] x = {};
                    Proof[] proofsFor = proofs.get(proofType).toArray(x);
                    if (proofsFor.length > 0) {
                        sb.append("<li><p>");
                        sb.append(getProofNarrative(proofType)).append(": ");
                        StringBuilder joinedHandles = new StringBuilder();
                        int i = 0;
                        while (i < proofsFor.length - 1) {
                            joinedHandles.append(proofLinks(fingerprint, proofsFor[i])).append(", ");
                            i++;
                        }
                        joinedHandles.append(proofLinks(fingerprint, proofsFor[i]));
                        sb.append(joinedHandles);
                        sb.append("</p></li>");
                    }
                }

            } catch (KeybaseException e) {
                return null;
            }
            sb.append("</ul></body></html>");
            String html = sb.toString();
            mHTML.put(fingerprint, html);
            return html;
        }

        // https://host/prove/<key-fingerprint>/<proof-id>
        private String proofLinks(String fingerprint, Proof proof) throws KeybaseException {
            String a = "<a href=\"" + proof.getServiceUrl() + "\">" + proof.getHandle() + "</a>";
            if (haveProofFor(proof.getType())) {
                return a +
                        "&nbsp;[<a href=\"https://" + VERIFY_HOST + PROVE_PATH +
                        fingerprint + "/" + proof.getId() +
                        "\">Verify</a>]";
            } else {
                return a;
            }
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            if (s == null) {
                s = "<html><body><p>" + getActivity().getString(R.string.key_trust_no_cloud_evidence) + "</p></body></html>";
            }
            mStartSearch.setVisibility(View.GONE);
            mSearchReport.loadDataWithBaseURL("file:///android_res/drawable/", s, "text/html", "UTF-8", null);
        }
    }

    private static String evidenceLink(String fingerprint) {
        return "<a href=\"https://" + VERIFY_HOST + EVIDENCE_PATH +
                fingerprint +
                "\"><img style=\"float:right;\" src=\"ic_action_done.png\"/></a>";
    }

    private String getProofNarrative(int proofType) {
        int stringIndex;
        switch (proofType) {
            case Proof.PROOF_TYPE_TWITTER: stringIndex = R.string.keybase_narrative_twitter; break;
            case Proof.PROOF_TYPE_GITHUB: stringIndex = R.string.keybase_narrative_github; break;
            case Proof.PROOF_TYPE_DNS: stringIndex = R.string.keybase_narrative_dns; break;
            case Proof.PROOF_TYPE_WEB_SITE: stringIndex = R.string.keybase_narrative_web_site; break;
            case Proof.PROOF_TYPE_HACKERNEWS: stringIndex = R.string.keybase_narrative_hackernews; break;
            case Proof.PROOF_TYPE_COINBASE: stringIndex = R.string.keybase_narrative_coinbase; break;
            case Proof.PROOF_TYPE_REDDIT: stringIndex = R.string.keybase_narrative_reddit; break;
            default: stringIndex = R.string.keybase_narrative_unknown;
        }
        return getActivity().getString(stringIndex);
    }

    private void appendIfOK(Hashtable<Integer, ArrayList<Proof>> table, Integer proofType, Proof proof) throws KeybaseException {
        if (!proofIsOK(proof)) {
            return;
        }
        ArrayList<Proof> list = table.get(proofType);
        if (list == null) {
            list = new ArrayList<Proof>();
            table.put(proofType, list);
        }
        list.add(proof);
    }

    // We only accept http & https proofs. Maybe whitelist later?
    private boolean proofIsOK(Proof proof) throws KeybaseException {
        Uri uri = Uri.parse(proof.getServiceUrl());
        String scheme = uri.getScheme();
        return ("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme));
    }

    // which proofs do we have working verifiers for?
    private boolean haveProofFor(int proofType) {
        switch (proofType) {
            case Proof.PROOF_TYPE_TWITTER: return true;
            case Proof.PROOF_TYPE_GITHUB: return true;
            case Proof.PROOF_TYPE_DNS: return false;
            case Proof.PROOF_TYPE_WEB_SITE: return true;
            case Proof.PROOF_TYPE_HACKERNEWS: return true;
            case Proof.PROOF_TYPE_COINBASE: return false;
            case Proof.PROOF_TYPE_REDDIT: return false;
            default: return false;
        }
    }

    // eats links that begin with VERIFY_HOST, hands off others to the system
    private class ControlDisplay extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String urlString) {
            Uri uri = Uri.parse(urlString);

            if (VERIFY_HOST.equals(uri.getHost())) {
                String path = uri.getPath();
                if (path.startsWith(PROVE_PATH)) {
                    String[] pieces = path.substring(PROVE_PATH.length()).split("/");
                    String fingerprint = pieces[0];
                    Proof proof = mProofs.get(pieces[1]);
                    verify(proof, fingerprint);

                } else if (path.startsWith(EVIDENCE_PATH)) {
                    // back to the evidence screen from a proof readout
                    String fingerprint = path.substring(EVIDENCE_PATH.length());
                    String html = mHTML.get(fingerprint);

                    if (html != null) {
                        view.loadDataWithBaseURL("file:///android_res/drawable/", html, "text/html", "UTF-8", null);
                    } else {
                        // something went wrong, can’t find evidence, back to view-key
                        Intent viewIntent = new Intent(getActivity(), ViewKeyActivity.class);
                        viewIntent.setData(mDataUri);
                        startActivity(viewIntent);
                    }
                }
                return false;
            } else {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
                startActivity(intent);
                return true;
            }
        }
    }

    private void verify(final Proof proof, final String fingerprint) {
        Intent intent = new Intent(getActivity(), KeychainIntentService.class);
        Bundle data = new Bundle();
        intent.setAction(KeychainIntentService.ACTION_VERIFY_KEYBASE_PROOF);

        data.putString(KeychainIntentService.KEYBASE_PROOF, proof.toString());
        data.putString(KeychainIntentService.KEYBASE_REQUIRED_FINGERPRINT, fingerprint);
        intent.putExtra(KeychainIntentService.EXTRA_DATA, data);

        // Create a new Messenger for the communication back after proof work is done
        // TODO: make progress_proving
        KeychainIntentServiceHandler handler = new KeychainIntentServiceHandler(getActivity(),
                getString(R.string.progress_decrypting), ProgressDialog.STYLE_HORIZONTAL) {
            public void handleMessage(Message message) {
                // handle messages by standard KeychainIntentServiceHandler first
                super.handleMessage(message);

                if (message.arg1 == KeychainIntentServiceHandler.MESSAGE_OKAY) {
                    Bundle returnData = message.getData();
                    String msg = returnData.getString(KeychainIntentServiceHandler.DATA_MESSAGE);
                    StringBuilder html = new StringBuilder();

                    if ((msg != null) && msg.equals("OK")) {
                        //yay
                        String serviceUrl = null;
                        String urlLabel = null;
                        String postUrl = null;
                        try {
                            serviceUrl = proof.getServiceUrl();
                            if (serviceUrl.startsWith("https://")) {
                                urlLabel = serviceUrl.substring("https://".length());
                            } else if (serviceUrl.startsWith("http://")) {
                                urlLabel = serviceUrl.substring("http://".length());
                            } else {
                                urlLabel = serviceUrl;
                            }
                            postUrl = proof.getHumanUrl();

                        } catch (KeybaseException e) {
                            throw new RuntimeException(e);
                        }
                        html.append("<html><body><p><strong>")
                                .append(getString(R.string.keybase_proof_succeeded))
                                .append("</strong></p><p><a href=\"")
                                .append(postUrl)
                                .append("\">")
                                .append(getString(R.string.keybase_a_post))
                                .append("</a> ")
                                .append(getString(R.string.keybase_fetched_from))
                                .append(" <a href=\"")
                                .append(serviceUrl)
                                .append("\">")
                                .append(urlLabel)
                                .append("</a> ")
                                .append(getString(R.string.keybase_contained_signature))
                                .append("</p>")
                                .append(evidenceLink(fingerprint))
                                .append("</body></html>");

                    } else {
                        msg = returnData.getString(KeychainIntentServiceHandler.DATA_ERROR);
                        if (msg == null) {
                            msg = getString(R.string.keybase_unknown_proof_failure);
                        }
                        html.append("<html><body><p><strong>")
                                .append(getString(R.string.keybase_proof_failure))
                                .append("</strong></p><p>")
                                .append(msg)
                                .append(evidenceLink(fingerprint))
                                .append("</p></body></html>");
                    }
                    mSearchReport.loadDataWithBaseURL("file:///android_res/drawable/", html.toString(), "text/html", "UTF-8", null);
                }
            }
        };

        // Create a new Messenger for the communication back
        Messenger messenger = new Messenger(handler);
        intent.putExtra(KeychainIntentService.EXTRA_MESSENGER, messenger);

        // show progress dialog
        handler.showProgressDialog(getActivity());

        // start service with intent
        getActivity().startService(intent);
    }
}

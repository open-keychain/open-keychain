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

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.textuality.keybase.lib.KeybaseException;
import com.textuality.keybase.lib.Proof;
import com.textuality.keybase.lib.User;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.KeybaseVerificationResult;
import org.sufficientlysecure.keychain.provider.KeychainContract.KeyRings;
import org.sufficientlysecure.keychain.service.KeybaseVerificationParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.Log;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class ViewKeyTrustFragment extends LoaderFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        CryptoOperationHelper.Callback<KeybaseVerificationParcel, KeybaseVerificationResult> {

    public static final String ARG_DATA_URI = "uri";

    private View mStartSearch;
    private TextView mTrustReadout;
    private TextView mReportHeader;
    private TableLayout mProofListing;
    private LayoutInflater mInflater;
    private View mProofVerifyHeader;
    private TextView mProofVerifyDetail;

    private static final int LOADER_ID_DATABASE = 1;

    // for retrieving the key we’re working on
    private Uri mDataUri;

    private Proof mProof;

    // for CryptoOperationHelper,Callback
    private String mKeybaseProof;
    private String mKeybaseFingerprint;
    private CryptoOperationHelper<KeybaseVerificationParcel, KeybaseVerificationResult>
            mKeybaseOpHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, superContainer, savedInstanceState);
        View view = inflater.inflate(R.layout.view_key_adv_keybase_fragment, getContainer());
        mInflater = inflater;

        mTrustReadout = (TextView) view.findViewById(R.id.view_key_trust_readout);
        mStartSearch = view.findViewById(R.id.view_key_trust_search_cloud);
        mStartSearch.setEnabled(false);
        mReportHeader = (TextView) view.findViewById(R.id.view_key_trust_cloud_narrative);
        mProofListing = (TableLayout) view.findViewById(R.id.view_key_proof_list);
        mProofVerifyHeader = view.findViewById(R.id.view_key_proof_verify_header);
        mProofVerifyDetail = (TextView) view.findViewById(R.id.view_key_proof_verify_detail);
        mReportHeader.setVisibility(View.GONE);
        mProofListing.setVisibility(View.GONE);
        mProofVerifyHeader.setVisibility(View.GONE);
        mProofVerifyDetail.setVisibility(View.GONE);

        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Uri dataUri = getArguments().getParcelable(ARG_DATA_URI);
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
            KeyRings._ID, KeyRings.FINGERPRINT, KeyRings.IS_REVOKED, KeyRings.IS_EXPIRED,
            KeyRings.HAS_ANY_SECRET, KeyRings.VERIFIED
    };
    static final int INDEX_TRUST_FINGERPRINT = 1;
    static final int INDEX_TRUST_IS_REVOKED = 2;
    static final int INDEX_TRUST_IS_EXPIRED = 3;
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
                if (data.getInt(INDEX_TRUST_IS_EXPIRED) != 0) {

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
                        mStartSearch.setEnabled(false);
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

    class ResultPage {
        String mHeader;
        final List<CharSequence> mProofs;

        public ResultPage(String header, List<CharSequence> proofs) {
            mHeader = header;
            mProofs = proofs;
        }
    }

    // look for evidence from keybase in the background, make tabular version of result
    //
    private class DescribeKey extends AsyncTask<String, Void, ResultPage> {

        @Override
        protected ResultPage doInBackground(String... args) {
            String fingerprint = args[0];

            final ArrayList<CharSequence> proofList = new ArrayList<CharSequence>();
            final Hashtable<Integer, ArrayList<Proof>> proofs = new Hashtable<Integer, ArrayList<Proof>>();
            try {
                User keybaseUser = User.findByFingerprint(fingerprint);
                for (Proof proof : keybaseUser.getProofs()) {
                    Integer proofType = proof.getType();
                    appendIfOK(proofs, proofType, proof);
                }

                // a one-liner in a modern programming language
                for (Integer proofType : proofs.keySet()) {
                    Proof[] x = {};
                    Proof[] proofsFor = proofs.get(proofType).toArray(x);
                    if (proofsFor.length > 0) {
                        SpannableStringBuilder ssb = new SpannableStringBuilder();
                        ssb.append(getProofNarrative(proofType)).append(" ");

                        int i = 0;
                        while (i < proofsFor.length - 1) {
                            appendProofLinks(ssb, fingerprint, proofsFor[i]);
                            ssb.append(", ");
                            i++;
                        }
                        appendProofLinks(ssb, fingerprint, proofsFor[i]);
                        proofList.add(ssb);
                    }
                }

            } catch (KeybaseException ignored) {
            }

            return new ResultPage(getString(R.string.key_trust_results_prefix), proofList);
        }

        private SpannableStringBuilder appendProofLinks(SpannableStringBuilder ssb, final String fingerprint, final Proof proof) throws KeybaseException {
            int startAt = ssb.length();
            String handle = proof.getHandle();
            ssb.append(handle);
            ssb.setSpan(new URLSpan(proof.getServiceUrl()), startAt, startAt + handle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (haveProofFor(proof.getType())) {
                ssb.append("\u00a0[");
                startAt = ssb.length();
                String verify = getString(R.string.keybase_verify);
                ssb.append(verify);
                ClickableSpan clicker = new ClickableSpan() {
                    @Override
                    public void onClick(View view) {
                        verify(proof, fingerprint);
                    }
                };
                ssb.setSpan(clicker, startAt, startAt + verify.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append("]");
            }
            return ssb;
        }

        @Override
        protected void onPostExecute(ResultPage result) {
            super.onPostExecute(result);
            if (result.mProofs.isEmpty()) {
                result.mHeader = getActivity().getString(R.string.key_trust_no_cloud_evidence);
            }

            mStartSearch.setVisibility(View.GONE);
            mReportHeader.setVisibility(View.VISIBLE);
            mProofListing.setVisibility(View.VISIBLE);
            mReportHeader.setText(result.mHeader);

            int rowNumber = 1;
            for (CharSequence s : result.mProofs) {
                TableRow row = (TableRow) mInflater.inflate(R.layout.view_key_adv_keybase_proof, null);
                TextView number = (TextView) row.findViewById(R.id.proof_number);
                TextView text = (TextView) row.findViewById(R.id.proof_text);
                number.setText(Integer.toString(rowNumber++) + ". ");
                text.setText(s);
                text.setMovementMethod(LinkMovementMethod.getInstance());
                mProofListing.addView(row);
            }

            // mSearchReport.loadDataWithBaseURL("file:///android_res/drawable/", s, "text/html", "UTF-8", null);
        }
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
        ArrayList<Proof> list = table.get(proofType);
        if (list == null) {
            list = new ArrayList<Proof>();
            table.put(proofType, list);
        }
        list.add(proof);
    }

    // which proofs do we have working verifiers for?
    private boolean haveProofFor(int proofType) {
        switch (proofType) {
            case Proof.PROOF_TYPE_TWITTER: return true;
            case Proof.PROOF_TYPE_GITHUB: return true;
            case Proof.PROOF_TYPE_DNS: return true;
            case Proof.PROOF_TYPE_WEB_SITE: return true;
            case Proof.PROOF_TYPE_HACKERNEWS: return true;
            case Proof.PROOF_TYPE_COINBASE: return true;
            case Proof.PROOF_TYPE_REDDIT: return true;
            default: return false;
        }
    }

    private void verify(final Proof proof, final String fingerprint) {

        mProof = proof;
        mKeybaseProof = proof.toString();
        mKeybaseFingerprint = fingerprint;

        mProofVerifyDetail.setVisibility(View.GONE);

        mKeybaseOpHelper = new CryptoOperationHelper<>(this, this,
                R.string.progress_verifying_signature);
        mKeybaseOpHelper.cryptoOperation();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (mKeybaseOpHelper != null) {
            mKeybaseOpHelper.handleActivityResult(requestCode, resultCode, data);
        }
    }

    // CryptoOperationHelper.Callback methods
    @Override
    public KeybaseVerificationParcel createOperationInput() {
        return new KeybaseVerificationParcel(mKeybaseProof, mKeybaseFingerprint);
    }

    @Override
    public void onCryptoOperationSuccess(KeybaseVerificationResult result) {

        result.createNotify(getActivity()).show();

        String proofUrl = result.mProofUrl;
        String presenceUrl = result.mPresenceUrl;
        String presenceLabel = result.mPresenceLabel;

        Proof proof = mProof; // TODO: should ideally be contained in result

        String proofLabel;
        switch (proof.getType()) {
            case Proof.PROOF_TYPE_TWITTER:
                proofLabel = getString(R.string.keybase_twitter_proof);
                break;
            case Proof.PROOF_TYPE_DNS:
                proofLabel = getString(R.string.keybase_dns_proof);
                break;
            case Proof.PROOF_TYPE_WEB_SITE:
                proofLabel = getString(R.string.keybase_web_site_proof);
                break;
            case Proof.PROOF_TYPE_GITHUB:
                proofLabel = getString(R.string.keybase_github_proof);
                break;
            case Proof.PROOF_TYPE_REDDIT:
                proofLabel = getString(R.string.keybase_reddit_proof);
                break;
            default:
                proofLabel = getString(R.string.keybase_a_post);
                break;
        }

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        ssb.append(getString(R.string.keybase_proof_succeeded));
        StyleSpan bold = new StyleSpan(Typeface.BOLD);
        ssb.setSpan(bold, 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append("\n\n");
        int length = ssb.length();
        ssb.append(proofLabel);
        if (proofUrl != null) {
            URLSpan postLink = new URLSpan(proofUrl);
            ssb.setSpan(postLink, length, length + proofLabel.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        if (Proof.PROOF_TYPE_DNS == proof.getType()) {
            ssb.append(" ").append(getString(R.string.keybase_for_the_domain)).append(" ");
        } else {
            ssb.append(" ").append(getString(R.string.keybase_fetched_from)).append(" ");
        }
        length = ssb.length();
        URLSpan presenceLink = new URLSpan(presenceUrl);
        ssb.append(presenceLabel);
        ssb.setSpan(presenceLink, length, length + presenceLabel.length(), Spanned
                .SPAN_EXCLUSIVE_EXCLUSIVE);
        if (Proof.PROOF_TYPE_REDDIT == proof.getType()) {
            ssb.append(", ").
                    append(getString(R.string.keybase_reddit_attribution)).
                    append(" “").append(proof.getHandle()).append("”, ");
        }
        ssb.append(" ").append(getString(R.string.keybase_contained_signature));

        displaySpannableResult(ssb);
    }

    @Override
    public void onCryptoOperationCancelled() {

    }

    @Override
    public void onCryptoOperationError(KeybaseVerificationResult result) {

        result.createNotify(getActivity()).show();

        SpannableStringBuilder ssb = new SpannableStringBuilder();

        ssb.append(getString(R.string.keybase_proof_failure));
        String msg = getString(result.getLog().getLast().mType.mMsgId);
        if (msg == null) {
            msg = getString(R.string.keybase_unknown_proof_failure);
        }
        StyleSpan bold = new StyleSpan(Typeface.BOLD);
        ssb.setSpan(bold, 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append("\n\n").append(msg);

        displaySpannableResult(ssb);
    }

    private void displaySpannableResult(SpannableStringBuilder ssb) {
        mProofVerifyHeader.setVisibility(View.VISIBLE);
        mProofVerifyDetail.setVisibility(View.VISIBLE);
        mProofVerifyDetail.setMovementMethod(LinkMovementMethod.getInstance());
        mProofVerifyDetail.setText(ssb);
    }
}

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

package org.sufficientlysecure.keychain.ui;


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
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
import com.textuality.keybase.lib.KeybaseQuery;
import com.textuality.keybase.lib.Proof;
import com.textuality.keybase.lib.User;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.network.OkHttpKeybaseClient;
import org.sufficientlysecure.keychain.network.orbot.OrbotHelper;
import org.sufficientlysecure.keychain.operations.results.KeybaseVerificationResult;
import org.sufficientlysecure.keychain.service.KeybaseVerificationParcel;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.keyview.UnifiedKeyInfoViewModel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.util.ParcelableProxy;
import org.sufficientlysecure.keychain.util.Preferences;

public class ViewKeyKeybaseFragment extends Fragment implements
        CryptoOperationHelper.Callback<KeybaseVerificationParcel, KeybaseVerificationResult> {
    private TextView mReportHeader;
    private TableLayout mProofListing;
    private LayoutInflater mInflater;
    private View mProofVerifyHeader;
    private TextView mProofVerifyDetail;

    private Proof mProof;

    // for CryptoOperationHelper,Callback
    private String mKeybaseProof;
    private String mKeybaseFingerprint;
    private CryptoOperationHelper<KeybaseVerificationParcel, KeybaseVerificationResult> mKeybaseOpHelper;

    /**
     * Creates new instance of this fragment
     */
    public static ViewKeyKeybaseFragment newInstance() {
        return new ViewKeyKeybaseFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup viewGroup, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.view_key_adv_keybase_fragment, viewGroup, false);
        mInflater = inflater;

        mReportHeader = view.findViewById(R.id.view_key_trust_cloud_narrative);
        mProofListing = view.findViewById(R.id.view_key_proof_list);
        mProofVerifyHeader = view.findViewById(R.id.view_key_proof_verify_header);
        mProofVerifyDetail = view.findViewById(R.id.view_key_proof_verify_detail);
        mReportHeader.setVisibility(View.GONE);
        mProofListing.setVisibility(View.GONE);
        mProofVerifyHeader.setVisibility(View.GONE);
        mProofVerifyDetail.setVisibility(View.GONE);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        UnifiedKeyInfoViewModel viewKeyViewModel = ViewModelProviders.of(requireActivity()).get(UnifiedKeyInfoViewModel.class);
        viewKeyViewModel.getUnifiedKeyInfoLiveData(requireContext()).observe(this, this::onLoadUnifiedKeyInfo);
    }

    private void onLoadUnifiedKeyInfo(UnifiedKeyInfo unifiedKeyInfo) {
        if (unifiedKeyInfo == null) {
            return;
        }
        String fingerprint = KeyFormattingUtils.convertFingerprintToHex(unifiedKeyInfo.fingerprint());
        startSearch(fingerprint);
    }

    private void startSearch(final String fingerprint) {
        final ParcelableProxy parcelableProxy =
                Preferences.getPreferences(getActivity()).getParcelableProxy();

        OrbotHelper.DialogActions dialogActions = new OrbotHelper.DialogActions() {
            @Override
            public void onOrbotStarted() {
                new DescribeKey(parcelableProxy).execute(fingerprint);
            }

            @Override
            public void onNeutralButton() {
                new DescribeKey(ParcelableProxy.getForNoProxy())
                        .execute(fingerprint);
            }

            @Override
            public void onCancel() {

            }
        };

        if (OrbotHelper.putOrbotInRequiredState(dialogActions, getActivity())) {
            new DescribeKey(parcelableProxy).execute(fingerprint);
        }
    }

    class ResultPage {
        String mHeader;
        final List<CharSequence> mProofs;

        ResultPage(String header, List<CharSequence> proofs) {
            mHeader = header;
            mProofs = proofs;
        }
    }

    /**
     * look for evidence from keybase in the background, make tabular version of result
     */
    private class DescribeKey extends AsyncTask<String, Void, ResultPage> {
        ParcelableProxy mParcelableProxy;

        DescribeKey(ParcelableProxy parcelableProxy) {
            mParcelableProxy = parcelableProxy;
        }

        @Override
        protected ResultPage doInBackground(String... args) {
            String fingerprint = args[0];

            final ArrayList<CharSequence> proofList = new ArrayList<>();
            final Hashtable<Integer, ArrayList<Proof>> proofs = new Hashtable<>();
            try {
                KeybaseQuery keybaseQuery = new KeybaseQuery(new OkHttpKeybaseClient());
                keybaseQuery.setProxy(mParcelableProxy.getProxy());
                User keybaseUser = User.findByFingerprint(keybaseQuery, fingerprint);
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
                        int i = 0;
                        while (i < proofsFor.length - 1) {
                            appendProofLinks(ssb, fingerprint, proofsFor[i]);
                            ssb.append(", ");
                            i++;
                        }
                        appendProofLinks(ssb, fingerprint, proofsFor[i]);
                        proofList.add(formatSpannableString(ssb, getProofNarrative(proofType)));
                    }
                }

            } catch (KeybaseException ignored) {
            }

            String prefix = "";
            if (isAdded()) {
                prefix = getString(R.string.key_trust_results_prefix);
            }

            return new ResultPage(prefix, proofList);
        }

        private SpannableStringBuilder formatSpannableString(SpannableStringBuilder proofLinks, String proofType) {
            //Formatting SpannableStringBuilder with String.format() causes the links to stop working.
            //This method is to insert the links while reserving the links

            SpannableStringBuilder ssb = new SpannableStringBuilder();
            ssb.append(proofType);
            if (proofType.contains("%s")) {
                int i = proofType.indexOf("%s");
                ssb.replace(i, i + 2, proofLinks);
            } else ssb.append(proofLinks);

            return ssb;
        }

        private void appendProofLinks(SpannableStringBuilder ssb, final String fingerprint, final Proof proof) throws KeybaseException {
            int startAt = ssb.length();
            String handle = proof.getHandle();
            ssb.append(handle);
            ssb.setSpan(new URLSpan(proof.getServiceUrl()), startAt, startAt + handle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            if (haveProofFor(proof.getType())) {
                ssb.append("\u00a0[");
                startAt = ssb.length();
                String verify = "";
                if (isAdded()) {
                    verify = getString(R.string.keybase_verify);
                }
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
        }

        @Override
        protected void onPostExecute(ResultPage result) {
            super.onPostExecute(result);
            // stop if fragment is no longer added to an activity
            if(!isAdded()) {
                return;
            }

            if (result.mProofs.isEmpty()) {
                result.mHeader = requireActivity().getString(R.string.key_trust_no_cloud_evidence);
            }

            mReportHeader.setVisibility(View.VISIBLE);
            mProofListing.setVisibility(View.VISIBLE);
            mReportHeader.setText(result.mHeader);

            int rowNumber = 1;
            for (CharSequence s : result.mProofs) {
                TableRow row = (TableRow) mInflater.inflate(R.layout.view_key_adv_keybase_proof, null);
                TextView number = row.findViewById(R.id.proof_number);
                TextView text = row.findViewById(R.id.proof_text);
                number.setText(Integer.toString(rowNumber++) + ". ");
                text.setText(s);
                text.setMovementMethod(LinkMovementMethod.getInstance());
                mProofListing.addView(row);
            }
        }
    }

    private String getProofNarrative(int proofType) {
        int stringIndex;
        switch (proofType) {
            case Proof.PROOF_TYPE_TWITTER:
                stringIndex = R.string.keybase_narrative_twitter;
                break;
            case Proof.PROOF_TYPE_GITHUB:
                stringIndex = R.string.keybase_narrative_github;
                break;
            case Proof.PROOF_TYPE_DNS:
                stringIndex = R.string.keybase_narrative_dns;
                break;
            case Proof.PROOF_TYPE_WEB_SITE:
                stringIndex = R.string.keybase_narrative_web_site;
                break;
            case Proof.PROOF_TYPE_HACKERNEWS:
                stringIndex = R.string.keybase_narrative_hackernews;
                break;
            case Proof.PROOF_TYPE_COINBASE:
                stringIndex = R.string.keybase_narrative_coinbase;
                break;
            case Proof.PROOF_TYPE_REDDIT:
                stringIndex = R.string.keybase_narrative_reddit;
                break;
            default:
                stringIndex = R.string.keybase_narrative_unknown;
        }

        if (isAdded()) {
            return getString(stringIndex);
        } else {
            return "";
        }
    }

    private void appendIfOK(Hashtable<Integer, ArrayList<Proof>> table, Integer proofType, Proof proof) {
        ArrayList<Proof> list = table.get(proofType);
        if (list == null) {
            list = new ArrayList<>();
            table.put(proofType, list);
        }
        list.add(proof);
    }

    // which proofs do we have working verifiers for?
    private boolean haveProofFor(int proofType) {
        switch (proofType) {
            case Proof.PROOF_TYPE_TWITTER:
                return true;
            case Proof.PROOF_TYPE_GITHUB:
                return true;
            case Proof.PROOF_TYPE_DNS:
                return true;
            case Proof.PROOF_TYPE_WEB_SITE:
                return true;
            case Proof.PROOF_TYPE_HACKERNEWS:
                return true;
            case Proof.PROOF_TYPE_COINBASE:
                return true;
            case Proof.PROOF_TYPE_REDDIT:
                return true;
            default:
                return false;
        }
    }

    private void verify(final Proof proof, final String fingerprint) {

        mProof = proof;
        mKeybaseProof = proof.toString();
        mKeybaseFingerprint = fingerprint;

        mProofVerifyDetail.setVisibility(View.GONE);

        mKeybaseOpHelper = new CryptoOperationHelper<>(1, this, this,
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

    @Override
    public boolean onCryptoSetProgress(String msg, int progress, int max) {
        return false;
    }

    private void displaySpannableResult(SpannableStringBuilder ssb) {
        mProofVerifyHeader.setVisibility(View.VISIBLE);
        mProofVerifyDetail.setVisibility(View.VISIBLE);
        mProofVerifyDetail.setMovementMethod(LinkMovementMethod.getInstance());
        mProofVerifyDetail.setText(ssb);
    }
}

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

package org.sufficientlysecure.keychain.remote.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BulletSpan;
import android.text.style.StyleSpan;
import android.view.View;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;

import java.util.ArrayList;

public class RemoteSelectPubKeyActivity extends BaseActivity {

    public static final String EXTRA_SELECTED_MASTER_KEY_IDS = "master_key_ids";
    public static final String EXTRA_MISSING_EMAILS = "missing_emails";
    public static final String EXTRA_DUPLICATE_EMAILS = "dublicate_emails";
    public static final String EXTRA_NO_USER_IDS_CHECK = "no_user_ids";

    public static final String EXTRA_DATA = "data";

    // select pub keys view
    private SelectPublicKeyFragment mSelectFragment;

    @Override
    protected void initLayout() {
        setContentView(R.layout.api_remote_select_pub_keys);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Bundle extras = getIntent().getExtras();

        long[] selectedMasterKeyIds = getIntent().getLongArrayExtra(EXTRA_SELECTED_MASTER_KEY_IDS);
        boolean noUserIdsCheck = getIntent().getBooleanExtra(EXTRA_NO_USER_IDS_CHECK, true);
        ArrayList<String> missingEmails = getIntent()
                .getStringArrayListExtra(EXTRA_MISSING_EMAILS);
        ArrayList<String> duplicateEmails = getIntent()
                .getStringArrayListExtra(EXTRA_DUPLICATE_EMAILS);

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        final SpannableString textIntro = new SpannableString(
                noUserIdsCheck ? getString(R.string.api_select_pub_keys_text_no_user_ids)
                        : getString(R.string.api_select_pub_keys_text)
        );
        textIntro.setSpan(new StyleSpan(Typeface.BOLD), 0, textIntro.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        ssb.append(textIntro);

        if (missingEmails != null && missingEmails.size() > 0) {
            ssb.append("\n\n");
            ssb.append(getString(R.string.api_select_pub_keys_missing_text));
            ssb.append("\n");
            for (String emails : missingEmails) {
                SpannableString ss = new SpannableString(emails + "\n");
                ss.setSpan(new BulletSpan(15, Color.BLACK), 0, ss.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append(ss);
            }
        }
        if (duplicateEmails != null && duplicateEmails.size() > 0) {
            ssb.append("\n\n");
            ssb.append(getString(R.string.api_select_pub_keys_dublicates_text));
            ssb.append("\n");
            for (String email : duplicateEmails) {
                SpannableString ss = new SpannableString(email + "\n");
                ss.setSpan(new BulletSpan(15, Color.BLACK), 0, ss.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                ssb.append(ss);
            }
        }

        // Inflate a "Done"/"Cancel" custom action bar view
        setFullScreenDialogDoneClose(R.string.btn_okay,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // add key ids to params Bundle for new request
                        Intent resultData = extras.getParcelable(EXTRA_DATA);
                        resultData.putExtra(OpenPgpApi.EXTRA_KEY_IDS_SELECTED,
                                mSelectFragment.getSelectedMasterKeyIds());

                        RemoteSelectPubKeyActivity.this.setResult(RESULT_OK, resultData);
                        RemoteSelectPubKeyActivity.this.finish();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // cancel
                        RemoteSelectPubKeyActivity.this.setResult(RESULT_CANCELED);
                        RemoteSelectPubKeyActivity.this.finish();
                    }
                });

        // set text on view
        TextView textView = (TextView) findViewById(R.id.api_select_pub_keys_text);
        textView.setText(ssb, TextView.BufferType.SPANNABLE);

        // Load select pub keys fragment
        // Check that the activity is using the layout version with
        // the fragment_container FrameLayout
        if (findViewById(R.id.api_select_pub_keys_fragment_container) != null) {

            // However, if we're being restored from a previous state,
            // then we don't need to do anything and should return or else
            // we could end up with overlapping fragments.
            if (savedInstanceState != null) {
                return;
            }

            // Create an instance of the fragment
            mSelectFragment = SelectPublicKeyFragment.newInstance(selectedMasterKeyIds);

            // Add the fragment to the 'fragment_container' FrameLayout
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.api_select_pub_keys_fragment_container, mSelectFragment).commit();
        }
    }

}

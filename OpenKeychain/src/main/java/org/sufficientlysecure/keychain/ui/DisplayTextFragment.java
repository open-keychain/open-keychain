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

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;

public class DisplayTextFragment extends DecryptFragment {

    public static final String ARG_PLAINTEXT = "plaintext";

    // view
    private TextView mText;

    // model (no state to persist though, that's all in arguments!)
    private boolean mShowMenuOptions = false;

    public static DisplayTextFragment newInstance(String plaintext, DecryptVerifyResult result) {
        DisplayTextFragment frag = new DisplayTextFragment();

        Bundle args = new Bundle();
        args.putString(ARG_PLAINTEXT, plaintext);
        args.putParcelable(ARG_DECRYPT_VERIFY_RESULT, result);

        frag.setArguments(args);

        return frag;
    }

    private Intent createSendIntent(String text) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        return sendIntent;
    }

    private void copyToClipboard(String text) {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        ClipboardManager clipMan = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipMan == null) {
            Notify.create(activity, R.string.error_clipboard_copy, Style.ERROR).show();
            return;
        }

        clipMan.setPrimaryClip(ClipData.newPlainText(Constants.CLIPBOARD_LABEL, text));
        Notify.create(activity, R.string.text_copied_to_clipboard, Style.OK).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_text_fragment, container, false);
        mText = view.findViewById(R.id.decrypt_text_plaintext);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();

        String plaintext = args.getString(ARG_PLAINTEXT);
        DecryptVerifyResult result =  args.getParcelable(ARG_DECRYPT_VERIFY_RESULT);

        if (result == null) {
            throw new IllegalStateException("Missing decryption result argument!");
        }

        // display signature result in activity
        setAutoLinkFromSignatureResult(result.getSignatureResult());
        mText.setText(plaintext);
        loadVerifyResult(result);
    }

    private void setAutoLinkFromSignatureResult(@Nullable OpenPgpSignatureResult signatureResult) {
        if (signatureResult == null) {
            mText.setAutoLinkMask(0);
            return;
        }

        switch (signatureResult.getResult()) {
            case OpenPgpSignatureResult.RESULT_VALID_KEY_CONFIRMED:
            case OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED: {
                mText.setAutoLinkMask(Linkify.ALL);
                break;
            }

            default: {
                mText.setAutoLinkMask(0);
                break;
            }
        }
    }

    @Override
    protected void onVerifyLoaded(boolean hideErrorOverlay) {
        mShowMenuOptions = hideErrorOverlay;
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (mShowMenuOptions) {
            inflater.inflate(R.menu.decrypt_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.decrypt_share: {
                startActivity(Intent.createChooser(createSendIntent(mText.getText().toString()),
                        getString(R.string.title_share_message)));
                break;
            }
            case R.id.decrypt_copy: {
                copyToClipboard(mText.getText().toString());
                break;
            }
            case R.id.decrypt_view_log: {
                startDisplayLogActivity();
                break;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }

        return true;
    }

}

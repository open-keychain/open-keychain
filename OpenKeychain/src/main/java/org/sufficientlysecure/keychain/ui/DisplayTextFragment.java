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

package org.sufficientlysecure.keychain.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.operations.results.DecryptVerifyResult;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.util.ShareHelper;

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

    /**
     * Create Intent Chooser but exclude decrypt activites
     */
    private Intent sendWithChooserExcludingDecrypt(String text) {
        Intent prototype = createSendIntent(text);
        String title = getString(R.string.title_share_message);

        // we don't want to decrypt the decrypted, no inception ;)
        String[] blacklist = new String[]{
                Constants.PACKAGE_NAME + ".ui.DecryptActivity",
                "org.thialfihar.android.apg.ui.DecryptActivity"
        };

        return new ShareHelper(getActivity()).createChooserExcluding(prototype, title, blacklist);
    }

    private Intent createSendIntent(String text) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, text);
        sendIntent.setType("text/plain");
        return sendIntent;
    }

    private void copyToClipboard(String text) {
        ClipboardReflection.copyToClipboard(getActivity(), text);
        Notify.create(getActivity(), R.string.text_copied_to_clipboard, Notify.Style.OK).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.decrypt_text_fragment, container, false);
        mText = (TextView) view.findViewById(R.id.decrypt_text_plaintext);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();

        String plaintext = args.getString(ARG_PLAINTEXT);
        DecryptVerifyResult result = args.getParcelable(ARG_DECRYPT_VERIFY_RESULT);

        // display signature result in activity
        mText.setText(plaintext);
        loadVerifyResult(result);

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
                startActivity(sendWithChooserExcludingDecrypt(mText.getText().toString()));
                break;
            }
            case R.id.decrypt_copy: {
                copyToClipboard(mText.getText().toString());
                break;
            }
            default: {
                return super.onOptionsItemSelected(item);
            }
        }

        return true;
    }

}

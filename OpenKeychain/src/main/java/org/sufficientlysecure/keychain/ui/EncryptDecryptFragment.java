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


import java.util.regex.Matcher;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.compatibility.ClipboardReflection;
import org.sufficientlysecure.keychain.pgp.PgpHelper;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.util.Notify.Style;
import org.sufficientlysecure.keychain.ui.util.SubtleAttentionSeeker;
import org.sufficientlysecure.keychain.util.FileHelper;
import timber.log.Timber;

public class EncryptDecryptFragment extends Fragment {

    View mClipboardIcon;

    private static final int REQUEST_CODE_INPUT = 0x00007003;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.encrypt_decrypt_fragment, container, false);

        View mEncryptFile = view.findViewById(R.id.encrypt_files);
        View mEncryptText = view.findViewById(R.id.encrypt_text);
        View mDecryptFile = view.findViewById(R.id.decrypt_files);
        View mDecryptFromClipboard = view.findViewById(R.id.decrypt_from_clipboard);
        mClipboardIcon = view.findViewById(R.id.clipboard_icon);

        mEncryptFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent encrypt = new Intent(getActivity(), EncryptFilesActivity.class);
                startActivity(encrypt);
            }
        });

        mEncryptText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent encrypt = new Intent(getActivity(), EncryptTextActivity.class);
                startActivity(encrypt);
            }
        });

        mDecryptFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FileHelper.openDocument(EncryptDecryptFragment.this, "*/*", false, REQUEST_CODE_INPUT);
            }
        });

        mDecryptFromClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decryptFromClipboard();
            }
        });

        return view;
    }

    private void decryptFromClipboard() {

        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        final CharSequence clipboardText = ClipboardReflection.getClipboardText(activity);
        if (TextUtils.isEmpty(clipboardText)) {
            Notify.create(activity, R.string.error_clipboard_empty, Style.ERROR).show();
            return;
        }

        ClipboardManager clipMan = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipMan == null) {
            Timber.e("Couldn't get ClipboardManager instance!");
            return;
        }

        ClipData clip = clipMan.getPrimaryClip();
        if (clip == null) {
            Timber.e("Couldn't get clipboard data!");
            return;
        }

        Intent clipboardDecrypt = new Intent(getActivity(), DecryptActivity.class);
        clipboardDecrypt.putExtra(DecryptActivity.EXTRA_CLIPDATA, clip);
        clipboardDecrypt.setAction(DecryptActivity.ACTION_DECRYPT_FROM_CLIPBOARD);
        startActivityForResult(clipboardDecrypt, 0);
    }

    @Override
    public void onResume() {
        super.onResume();

        checkClipboardForEncryptedText();
    }

    private void checkClipboardForEncryptedText() {
        CharSequence clipboardText = ClipboardReflection.getClipboardText(getActivity());

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... voids) {
                if (clipboardText == null) {
                    return false;
                }

                // see if it looks like a pgp thing
                Matcher matcher = PgpHelper.PGP_MESSAGE.matcher(clipboardText);
                boolean animate = matcher.matches();

                // see if it looks like another pgp thing
                if (!animate) {
                    matcher = PgpHelper.PGP_CLEARTEXT_SIGNATURE.matcher(clipboardText);
                    animate = matcher.matches();
                }
                return animate;
            }

            @Override
            protected void onPostExecute(Boolean animate) {
                super.onPostExecute(animate);

                // if so, animate the clipboard icon just a bit~
                if (animate) {
                    SubtleAttentionSeeker.tada(mClipboardIcon, 1.5f).start();
                }
            }
        }.execute();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != REQUEST_CODE_INPUT) {
            return;
        }

        if (resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri == null) {
                Notify.create(getActivity(), R.string.no_file_selected, Notify.Style.ERROR).show();
                return;
            }

            Intent intent = new Intent(getActivity(), DecryptActivity.class);
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(uri);
            startActivity(intent);

        }
    }

}

/*
 * Copyright (C) 2013-2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.sufficientlysecure.keychain.ui.adapter;

import android.content.Context;
import android.content.res.Resources;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.openintents.openpgp.util.OpenPgpUtils;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.databinding.ImportKeysListItemBinding;
import org.sufficientlysecure.keychain.keyimport.ImportKeysListEntry;
import org.sufficientlysecure.keychain.keyimport.ParcelableKeyRing;
import org.sufficientlysecure.keychain.keyimport.processing.BytesLoaderState;
import org.sufficientlysecure.keychain.keyimport.processing.CloudLoaderState;
import org.sufficientlysecure.keychain.keyimport.processing.ImportKeysListener;
import org.sufficientlysecure.keychain.keyimport.processing.LoaderState;
import org.sufficientlysecure.keychain.operations.ImportOperation;
import org.sufficientlysecure.keychain.pgp.KeyRing;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Highlighter;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class ImportKeysAdapter extends RecyclerView.Adapter<ImportKeysAdapter.ViewHolder> {

    private Context mContext;
    private ImportKeysListener mListener;
    private boolean mNonInteractive;

    private LoaderState mLoaderState;
    private List<ImportKeysListEntry> mData;

    public ImportKeysAdapter(Context mContext, ImportKeysListener listener, boolean mNonInteractive) {
        this.mContext = mContext;
        this.mListener = listener;
        this.mNonInteractive = mNonInteractive;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImportKeysListItemBinding binding;

        public ViewHolder(View view) {
            super(view);
            binding = DataBindingUtil.bind(view);
        }
    }

    public void clearData() {
        mData = null;
        notifyDataSetChanged();
    }

    public void setLoaderState(LoaderState loaderState) {
        this.mLoaderState = loaderState;
    }

    public void setData(List<ImportKeysListEntry> data) {
        this.mData = data;
        notifyDataSetChanged();
    }

    /**
     * This method returns a list of all selected entries, with public keys sorted
     * before secret keys, see ImportOperation for specifics.
     *
     * @see ImportOperation
     */
    public List<ImportKeysListEntry> getEntries() {
        ArrayList<ImportKeysListEntry> result = new ArrayList<>();
        ArrayList<ImportKeysListEntry> secrets = new ArrayList<>();
        if (mData == null) {
            return result;
        }
        for (ImportKeysListEntry entry : mData) {
            // add this entry to either the secret or the public list
            (entry.isSecretKey() ? secrets : result).add(entry);
        }
        // add secret keys at the end of the list
        result.addAll(secrets);
        return result;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View v = inflater.inflate(R.layout.import_keys_list_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        final ImportKeysListItemBinding b = holder.binding;
        final ImportKeysListEntry entry = mData.get(position);

        Resources resources = mContext.getResources();
        Highlighter highlighter = new Highlighter(mContext, entry.getQuery());
        b.setStandardColor(FormattingUtils.getColorFromAttr(mContext, R.attr.colorText));
        b.setRevokedExpiredColor(resources.getColor(R.color.key_flag_gray));
        b.setSecretColor(Color.RED);
        b.setHighlighter(highlighter);

        b.setSecret(entry.isSecretKey());
        b.setExpired(entry.isExpired());
        b.setRevoked(entry.isRevoked());

        String userId = entry.getUserIds().get(0); // main user id
        OpenPgpUtils.UserId userIdSplit = KeyRing.splitUserId(userId);

        b.setAlgorithm(entry.getAlgorithm());
        b.setUserId(userIdSplit.name);
        b.setUserIdEmail(userIdSplit.email);
        b.setKeyId(KeyFormattingUtils.beautifyKeyIdWithPrefix(mContext, entry.getKeyIdHex()));

        if (entry.isRevoked()) {
            KeyFormattingUtils.setStatusImage(mContext, b.status, null, State.REVOKED, R.color.key_flag_gray);
        } else if (entry.isExpired()) {
            KeyFormattingUtils.setStatusImage(mContext, b.status, null, State.EXPIRED, R.color.key_flag_gray);
        }

        b.importKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mLoaderState instanceof BytesLoaderState) {
                    mListener.importKey(new ParcelableKeyRing(entry.getEncodedRing()));
                } else if (mLoaderState instanceof CloudLoaderState) {
                    mListener.importKey(new ParcelableKeyRing(entry.getFingerprintHex(), entry.getKeyIdHex(),
                            entry.getKeybaseName(), entry.getFbUsername()));
                }
            }
        });
        b.expand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean hidden = b.extraContainer.getVisibility() == View.GONE;
                b.extraContainer.setVisibility(hidden ? View.VISIBLE : View.GONE);
                b.expand.animate().rotation(hidden ? 180 : 0).start();
            }
        });

        if (entry.getUserIds().size() == 1) {
            b.userIdsList.setVisibility(View.GONE);
        } else {
            b.userIdsList.setVisibility(View.VISIBLE);

            // destroyLoader view from holder
            b.userIdsList.removeAllViews();

            // we want conventional gpg UserIDs first, then Keybase ”proofs”
            HashMap<String, HashSet<String>> mergedUserIds = entry.getMergedUserIds();
            ArrayList<Map.Entry<String, HashSet<String>>> sortedIds = new ArrayList<Map.Entry<String, HashSet<String>>>(mergedUserIds.entrySet());
            Collections.sort(sortedIds, new java.util.Comparator<Map.Entry<String, HashSet<String>>>() {
                @Override
                public int compare(Map.Entry<String, HashSet<String>> entry1, Map.Entry<String, HashSet<String>> entry2) {

                    // sort keybase UserIds after non-Keybase
                    boolean e1IsKeybase = entry1.getKey().contains(":");
                    boolean e2IsKeybase = entry2.getKey().contains(":");
                    if (e1IsKeybase != e2IsKeybase) {
                        return (e1IsKeybase) ? 1 : -1;
                    }
                    return entry1.getKey().compareTo(entry2.getKey());
                }
            });

            for (Map.Entry<String, HashSet<String>> pair : sortedIds) {
                String cUserId = pair.getKey();
                HashSet<String> cEmails = pair.getValue();

                LayoutInflater inflater = LayoutInflater.from(mContext);

                TextView uidView = (TextView) inflater.inflate(
                        R.layout.import_keys_list_entry_user_id, null);
                uidView.setText(highlighter.highlight(cUserId));
                uidView.setPadding(0, 0, FormattingUtils.dpToPx(mContext, 8), 0);

                if (entry.isRevoked() || entry.isExpired()) {
                    uidView.setTextColor(mContext.getResources().getColor(R.color.key_flag_gray));
                } else {
                    uidView.setTextColor(FormattingUtils.getColorFromAttr(mContext, R.attr.colorText));
                }

                b.userIdsList.addView(uidView);

                for (String email : cEmails) {
                    TextView emailView = (TextView) inflater.inflate(
                            R.layout.import_keys_list_entry_user_id, null);
                    emailView.setPadding(
                            FormattingUtils.dpToPx(mContext, 16), 0,
                            FormattingUtils.dpToPx(mContext, 8), 0);
                    emailView.setText(highlighter.highlight(email));

                    if (entry.isRevoked() || entry.isExpired()) {
                        emailView.setTextColor(mContext.getResources().getColor(R.color.key_flag_gray));
                    } else {
                        emailView.setTextColor(FormattingUtils.getColorFromAttr(mContext, R.attr.colorText));
                    }

                    b.userIdsList.addView(emailView);
                }
            }
        }
    }

    @Override
    public int getItemCount() {
        return mData != null ? mData.size() : 0;
    }

}

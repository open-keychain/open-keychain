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

package org.sufficientlysecure.keychain.ui.adapter;


import java.util.List;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.linked.UriAttribute;
import org.sufficientlysecure.keychain.ui.adapter.IdentityAdapter.ViewHolder;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.AutocryptPeerInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.IdentityInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.LinkedIdInfo;
import org.sufficientlysecure.keychain.ui.keyview.loader.IdentityDao.UserIdInfo;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils.State;
import org.sufficientlysecure.keychain.ui.util.SubtleAttentionSeeker;


public class IdentityAdapter extends RecyclerView.Adapter<ViewHolder> {
    private static final int VIEW_TYPE_USER_ID = 0;
    private static final int VIEW_TYPE_LINKED_ID = 1;


    private final Context context;
    private final LayoutInflater layoutInflater;
    private final IdentityClickListener identityClickListener;

    private List<IdentityInfo> data;
    private boolean isSecret;


    public IdentityAdapter(Context context, IdentityClickListener identityClickListener) {
        super();
        this.layoutInflater = LayoutInflater.from(context);
        this.context = context;
        this.identityClickListener = identityClickListener;
    }

    public void setData(List<IdentityInfo> data, boolean isSecret) {
        this.data = data;
        this.isSecret = isSecret;

        notifyDataSetChanged();
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        IdentityInfo info = data.get(position);

        int viewType = getItemViewType(position);
        if (viewType == VIEW_TYPE_USER_ID) {
            if (info instanceof AutocryptPeerInfo) {
                ((UserIdViewHolder) holder).bind((AutocryptPeerInfo) info);
            } else {
                ((UserIdViewHolder) holder).bind((UserIdInfo) info);
            }
        } else if (viewType == VIEW_TYPE_LINKED_ID) {
            ((LinkedIdViewHolder) holder).bind(context, (LinkedIdInfo) info, isSecret);
        } else {
            throw new IllegalStateException("unhandled identitytype!");
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER_ID) {
            return new UserIdViewHolder(
                    layoutInflater.inflate(R.layout.view_key_identity_user_id, parent, false), identityClickListener);
        } else if (viewType == VIEW_TYPE_LINKED_ID) {
            return new LinkedIdViewHolder(layoutInflater.inflate(R.layout.linked_id_item, parent, false),
                    identityClickListener);
        } else {
            throw new IllegalStateException("unhandled identitytype!");
        }
    }

    @Override
    public int getItemViewType(int position) {
        IdentityInfo info = data.get(position);
        if (info instanceof UserIdInfo || info instanceof AutocryptPeerInfo) {
            return VIEW_TYPE_USER_ID;
        } else if (info instanceof LinkedIdInfo) {
            return VIEW_TYPE_LINKED_ID;
        } else {
            throw new IllegalStateException("unhandled identitytype!");
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public IdentityInfo getInfo(int position) {
        return data.get(position);
    }

    abstract static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    public static class LinkedIdViewHolder extends ViewHolder {
        public final ImageView vVerified;
        final private ImageView vIcon;
        final private TextView vTitle;
        final private TextView vComment;

        public LinkedIdViewHolder(View view, final IdentityClickListener identityClickListener) {
            super(view);

            vVerified = view.findViewById(R.id.linked_id_certified_icon);
            vIcon = view.findViewById(R.id.linked_id_type_icon);
            vTitle = view.findViewById(R.id.linked_id_title);
            vComment = view.findViewById(R.id.linked_id_comment);

            view.setOnClickListener(v -> {
                if (identityClickListener != null) {
                    identityClickListener.onClickIdentity(getAdapterPosition());
                }
            });
        }

        public void bind(Context context, LinkedIdInfo info, boolean isSecret) {
            bindVerified(context, info, isSecret);

            UriAttribute uriAttribute = info.getLinkedAttribute();
            bind(context, uriAttribute);
        }

        public void bind(Context context, UriAttribute uriAttribute) {
            vTitle.setText(uriAttribute.getDisplayTitle(context));

            String comment = uriAttribute.getDisplayComment(context);
            if (comment != null) {
                vComment.setVisibility(View.VISIBLE);
                vComment.setText(comment);
            } else {
                vComment.setVisibility(View.GONE);
            }

            vIcon.setImageResource(uriAttribute.getDisplayIcon());
        }

        private void bindVerified(Context context, IdentityInfo info, boolean isSecret) {
            if (!isSecret) {
                if (info.isVerified()) {
                    KeyFormattingUtils.setStatusImage(context, vVerified,
                            null, State.VERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                } else {
                    KeyFormattingUtils.setStatusImage(context, vVerified,
                            null, State.UNVERIFIED, KeyFormattingUtils.DEFAULT_COLOR);
                }
            }
        }

        public void seekAttention() {
            if (Build.VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
                ObjectAnimator anim = SubtleAttentionSeeker.tintText(vComment, 1000);
                anim.setStartDelay(200);
                anim.start();
            }
        }
    }

    private static class UserIdViewHolder extends ViewHolder {
        private final TextView vName;
        private final TextView vAddress;
        private final TextView vComment;
        private final ImageView vIcon;
        private final ImageView vMore;

        private UserIdViewHolder(View view, final IdentityClickListener identityClickListener) {
            super(view);

            vName = view.findViewById(R.id.user_id_item_name);
            vAddress = view.findViewById(R.id.user_id_item_address);
            vComment = view.findViewById(R.id.user_id_item_comment);

            vIcon = view.findViewById(R.id.trust_id_app_icon);
            vMore = view.findViewById(R.id.user_id_item_more);

            view.setOnClickListener(v -> identityClickListener.onClickIdentity(getAdapterPosition()));
            vMore.setOnClickListener(v -> identityClickListener.onClickIdentityMore(getAdapterPosition(), v));
        }

        public void bind(AutocryptPeerInfo info) {
            if (info.getUserIdInfo() != null) {
                bindUserIdInfo(info.getUserIdInfo());
            } else {
                vName.setVisibility(View.GONE);
                vComment.setVisibility(View.GONE);

                vAddress.setText(info.getIdentity());
                vAddress.setTypeface(null, Typeface.NORMAL);
            }

            vIcon.setImageDrawable(info.getAppIcon());

            vIcon.setVisibility(View.VISIBLE);
            vMore.setVisibility(View.VISIBLE);

            itemView.setClickable(info.getAutocryptPeerIntent() != null);
        }

        public void bind(UserIdInfo info) {
            bindUserIdInfo(info);

            vIcon.setVisibility(View.GONE);
            vMore.setVisibility(View.GONE);
        }

        private void bindUserIdInfo(UserIdInfo info) {
            if (info.getName() != null) {
                vName.setText(info.getName());
            } else {
                vName.setText(R.string.user_id_no_name);
            }
            if (info.getEmail() != null) {
                vAddress.setText(info.getEmail());
                vAddress.setVisibility(View.VISIBLE);
            } else {
                vAddress.setVisibility(View.GONE);
            }
            if (info.getComment() != null) {
                vComment.setText(info.getComment());
                vComment.setVisibility(View.VISIBLE);
            } else {
                vComment.setVisibility(View.GONE);
            }

            if (info.isPrimary()) {
                vName.setTypeface(null, Typeface.BOLD);
                vAddress.setTypeface(null, Typeface.BOLD);
            } else {
                vName.setTypeface(null, Typeface.NORMAL);
                vAddress.setTypeface(null, Typeface.NORMAL);
            }
        }

    }

    public interface IdentityClickListener {
        void onClickIdentity(int position);
        void onClickIdentityMore(int position, View anchor);
    }
}

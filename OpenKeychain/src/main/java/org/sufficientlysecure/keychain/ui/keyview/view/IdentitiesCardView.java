/*
 * Copyright (C) 2017 Vincent Breitmoser <v.breitmoser@mugenguild.com>
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

package org.sufficientlysecure.keychain.ui.keyview.view;


import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.IdentityAdapter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.IdentitiesPresenter.IdentitiesCardListener;
import org.sufficientlysecure.keychain.ui.keyview.presenter.IdentitiesPresenter.IdentitiesMvpView;
import org.sufficientlysecure.keychain.ui.util.recyclerview.DividerItemDecoration;


public class IdentitiesCardView extends CardView implements IdentitiesMvpView {
    private final RecyclerView vIdentities;

    private IdentitiesCardListener identitiesCardListener;

    public IdentitiesCardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(R.layout.identities_card, this, true);

        vIdentities = (RecyclerView) view.findViewById(R.id.view_key_user_ids);
        vIdentities.setLayoutManager(new LinearLayoutManager(context));
        vIdentities.addItemDecoration(new DividerItemDecoration(context, DividerItemDecoration.VERTICAL_LIST));

        Button userIdsEditButton = (Button) view.findViewById(R.id.view_key_card_user_ids_edit);
        userIdsEditButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (identitiesCardListener != null) {
                    identitiesCardListener.onClickEditIdentities();
                }
            }
        });

        Button linkedIdsAddButton = (Button) view.findViewById(R.id.view_key_card_linked_ids_add);

        linkedIdsAddButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (identitiesCardListener != null) {
                    identitiesCardListener.onClickAddIdentity();
                }
            }
        });
    }

    @Override
    public void setIdentitiesAdapter(IdentityAdapter identityAdapter) {
        vIdentities.setAdapter(identityAdapter);
    }

    @Override
    public void setIdentitiesCardListener(IdentitiesCardListener identitiesCardListener) {
        this.identitiesCardListener = identitiesCardListener;
    }

    @Override
    public void setEditIdentitiesButtonVisible(boolean show) {
        findViewById(R.id.view_key_card_user_ids_buttons).setVisibility(show ? View.VISIBLE : View.GONE);
    }
}

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
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.adapter.LinkedIdsAdapter;
import org.sufficientlysecure.keychain.ui.adapter.TrustIdsAdapter;
import org.sufficientlysecure.keychain.ui.adapter.UserIdsAdapter;
import org.sufficientlysecure.keychain.ui.keyview.presenter.IdentitiesPresenter.IdentitiesCardListener;
import org.sufficientlysecure.keychain.ui.keyview.presenter.IdentitiesPresenter.IdentitiesMvpView;
import org.sufficientlysecure.keychain.ui.keyview.presenter.LinkedIdentitiesPresenter.LinkedIdsClickListener;
import org.sufficientlysecure.keychain.ui.keyview.presenter.LinkedIdentitiesPresenter.LinkedIdsMvpView;
import org.sufficientlysecure.keychain.ui.keyview.presenter.TrustIdsPresenter.TrustIdsClickListener;
import org.sufficientlysecure.keychain.ui.keyview.presenter.TrustIdsPresenter.TrustIdsMvpView;


public class TrustIdsIdCardView extends CardView implements TrustIdsMvpView {
    private ListView vTrustIds;

    private TrustIdsClickListener trustIdsClickListener;

    public TrustIdsIdCardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(R.layout.trust_ids_card, this, true);

        vTrustIds = (ListView) view.findViewById(R.id.view_key_trust_ids);
        vTrustIds.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (trustIdsClickListener != null) {
                    trustIdsClickListener.onTrustIdItemClick(position);
                }
            }
        });
    }

    @Override
    public void setTrustIdAdapter(TrustIdsAdapter trustIdsAdapter) {
        vTrustIds.setAdapter(trustIdsAdapter);
    }

    @Override
    public void showCard(boolean show) {
        setVisibility(show ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setTrustIdClickListener(TrustIdsClickListener trustIdsClickListener) {
        this.trustIdsClickListener = trustIdsClickListener;
    }
}

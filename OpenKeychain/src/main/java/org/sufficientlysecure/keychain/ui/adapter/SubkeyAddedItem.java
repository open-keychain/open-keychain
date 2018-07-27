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


import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.View;

import eu.davidea.flexibleadapter.FlexibleAdapter;
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem;
import eu.davidea.flexibleadapter.items.IFlexible;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.CanonicalizedSecretKey.SecretKeyType;
import org.sufficientlysecure.keychain.service.SaveKeyringParcel.SubkeyAdd;
import org.sufficientlysecure.keychain.ui.SubKeyItem.SubkeyViewHolder;
import org.sufficientlysecure.keychain.ui.ViewKeyAdvSubkeysFragment;
import org.sufficientlysecure.keychain.ui.ViewKeyAdvSubkeysFragment.SubkeyEditViewModel;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


public class SubkeyAddedItem extends AbstractFlexibleItem<SubkeyViewHolder> {
    private SubkeyAdd subkeyAdd;
    private final SubkeyEditViewModel viewModel;

    public SubkeyAddedItem(SubkeyAdd newSubkey, SubkeyEditViewModel viewModel) {
        this.subkeyAdd = newSubkey;
        this.viewModel = viewModel;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SubkeyAddedItem && subkeyAdd == o;
    }

    @Override
    public int hashCode() {
        return subkeyAdd.hashCode();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.view_key_adv_subkey_item;
    }

    @Override
    public SubkeyViewHolder createViewHolder(View view, FlexibleAdapter<IFlexible> adapter) {
        return new SubkeyViewHolder(view, adapter);
    }

    @Override
    public int getItemViewType() {
        return ViewKeyAdvSubkeysFragment.SUBKEY_TYPE_ADDED;
    }

    @Override
    public void bindViewHolder(FlexibleAdapter<IFlexible> adapter, SubkeyViewHolder holder, int position,
            List<Object> payloads) {
        Date expiry = subkeyAdd.getExpiry() != null && subkeyAdd.getExpiry() != 0L ? new Date(subkeyAdd.getExpiry() * 1000) : null;

        holder.bindKeyId(null, false);
        holder.bindKeyDetails(subkeyAdd.getAlgorithm(), subkeyAdd.getKeySize(), subkeyAdd.getCurve(), SecretKeyType.PASSPHRASE);
        holder.bindKeyStatus(null, expiry, false, true);
        holder.bindKeyFlags(subkeyAdd.canCertify(), subkeyAdd.canSign(), subkeyAdd.canEncrypt(), subkeyAdd.canAuthenticate());
        holder.bindSubkeyAction(R.string.subkey_action_create, v -> {
            viewModel.skpBuilder.getMutableAddSubKeys().remove(subkeyAdd);
            adapter.removeItem(position);
        });
    }

}

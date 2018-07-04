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

package org.sufficientlysecure.keychain.ui.keyview.view;


import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;


public class SystemContactCardView extends CardView {
    private LinearLayout vSystemContactLayout;
    private ImageView vSystemContactPicture;
    private TextView vSystemContactName;

    public SystemContactCardView(Context context, AttributeSet attrs) {
        super(context, attrs);

        View view = LayoutInflater.from(context).inflate(R.layout.system_contact_card, this, true);

        vSystemContactLayout = view.findViewById(R.id.system_contact_layout);
        vSystemContactName = view.findViewById(R.id.system_contact_name);
        vSystemContactPicture = view.findViewById(R.id.system_contact_picture);
    }

    public void setSystemContactClickListener(OnClickListener onClickListener) {
        vSystemContactLayout.setOnClickListener(onClickListener);
    }

    public void hideLinkedSystemContact() {
        setVisibility(View.GONE);
    }

    public void showLinkedSystemContact(String contactName, Bitmap picture) {
        vSystemContactName.setText(contactName);
        if (picture != null) {
            vSystemContactPicture.setImageBitmap(picture);
        } else {
            vSystemContactPicture.setImageResource(R.drawable.ic_person_grey_48dp);
        }

        setVisibility(View.VISIBLE);
    }
}

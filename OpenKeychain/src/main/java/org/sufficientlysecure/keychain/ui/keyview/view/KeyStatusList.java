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
import android.support.annotation.ColorRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyview.presenter.KeyHealthPresenter.KeyStatusMvpView;


public class KeyStatusList extends LinearLayout implements KeyStatusMvpView {
    private final TextView vCertText, vSignText, vDecryptText;
    private final ImageView vCertIcon, vSignIcon, vDecryptIcon;
    private final View vCertToken, vSignToken, vDecryptToken;
    private final View vCertifyLayout, vSignLayout, vDecryptLayout;

    public KeyStatusList(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(VERTICAL);

        View view = LayoutInflater.from(context).inflate(R.layout.subkey_status_card_content, this, true);

        vCertifyLayout = view.findViewById(R.id.cap_certify);
        vSignLayout = view.findViewById(R.id.cap_sign);
        vDecryptLayout = view.findViewById(R.id.cap_decrypt);

        vCertText = (TextView) view.findViewById(R.id.cap_cert_text);
        vSignText = (TextView) view.findViewById(R.id.cap_sign_text);
        vDecryptText = (TextView) view.findViewById(R.id.cap_decrypt_text);

        vCertIcon = (ImageView) view.findViewById(R.id.cap_cert_icon);
        vSignIcon = (ImageView) view.findViewById(R.id.cap_sign_icon);
        vDecryptIcon = (ImageView) view.findViewById(R.id.cap_decrypt_icon);

        vCertToken = view.findViewById(R.id.cap_cert_security_token);
        vSignToken = view.findViewById(R.id.cap_sign_security_token);
        vDecryptToken = view.findViewById(R.id.cap_decrypt_security_token);

    }

    // this is just a list of statuses a key can be in, which we can also display
    public enum KeyDisplayStatus {
        OK (R.color.android_green_light, R.color.primary,
                R.string.cap_cert_ok, R.string.cap_sign_ok, R.string.cap_decrypt_ok, false),
        DIVERT (R.color.android_green_light, R.color.primary,
                R.string.cap_cert_divert, R.string.cap_sign_divert, R.string.cap_decrypt_divert, true),
        REVOKED (R.color.android_red_light, R.color.android_red_light,
                R.string.cap_sign_revoked, R.string.cap_decrypt_revoked, false),
        EXPIRED (R.color.android_red_light, R.color.android_red_light,
                R.string.cap_sign_expired, R.string.cap_decrypt_expired, false),
        STRIPPED (R.color.android_red_light, R.color.android_red_light,
                R.string.cap_cert_stripped, R.string.cap_sign_stripped, R.string.cap_decrypt_stripped, false),
        INSECURE (R.color.android_red_light, R.color.android_red_light,
                R.string.cap_sign_insecure, R.string.cap_sign_insecure, false),
        UNAVAILABLE (R.color.android_red_light, R.color.android_red_light,
                R.string.cap_cert_unavailable, R.string.cap_sign_unavailable, R.string.cap_decrypt_unavailable, false);

        @ColorRes final int mColor, mTextColor;
        @StringRes final Integer mCertifyStr, mSignStr, mDecryptStr;
        final boolean mIsDivert;

        KeyDisplayStatus(@ColorRes int color, @ColorRes int textColor,
                @StringRes int signStr, @StringRes int encryptStr, boolean isDivert) {
            mColor = color;
            mTextColor = textColor;
            mCertifyStr = null;
            mSignStr = signStr;
            mDecryptStr = encryptStr;
            mIsDivert = isDivert;
        }

        KeyDisplayStatus(@ColorRes int color, @ColorRes int textColor,
                @StringRes int certifyStr, @StringRes int signStr, @StringRes int encryptStr, boolean isDivert) {
            mColor = color;
            mTextColor = textColor;
            mCertifyStr = certifyStr;
            mSignStr = signStr;
            mDecryptStr = encryptStr;
            mIsDivert = isDivert;
        }

    }

    @Override
    public void setCertifyStatus(KeyDisplayStatus keyDisplayStatus) {
        if (keyDisplayStatus == null) {
            vCertifyLayout.setVisibility(View.GONE);
            return;
        }

        vCertIcon.setColorFilter(ContextCompat.getColor(getContext(), keyDisplayStatus.mColor));
        vCertText.setText(keyDisplayStatus.mCertifyStr);
        vCertText.setTextColor(ContextCompat.getColor(getContext(), keyDisplayStatus.mTextColor));
        vCertToken.setVisibility(keyDisplayStatus.mIsDivert ? View.VISIBLE : View.GONE);
        vCertifyLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void setSignStatus(KeyDisplayStatus keyDisplayStatus) {
        if (keyDisplayStatus == null) {
            vSignLayout.setVisibility(View.GONE);
            return;
        }
        vSignIcon.setColorFilter(ContextCompat.getColor(getContext(), keyDisplayStatus.mColor));
        vSignText.setText(keyDisplayStatus.mSignStr);
        vSignText.setTextColor(ContextCompat.getColor(getContext(), keyDisplayStatus.mTextColor));
        vSignToken.setVisibility(keyDisplayStatus.mIsDivert ? View.VISIBLE : View.GONE);
        vSignLayout.setVisibility(View.VISIBLE);
    }

    @Override
    public void setDecryptStatus(KeyDisplayStatus keyDisplayStatus) {
        if (keyDisplayStatus == null) {
            vDecryptLayout.setVisibility(View.GONE);
            return;
        }
        vDecryptIcon.setColorFilter(ContextCompat.getColor(getContext(), keyDisplayStatus.mColor));
        vDecryptText.setText(keyDisplayStatus.mDecryptStr);
        vDecryptText.setTextColor(ContextCompat.getColor(getContext(), keyDisplayStatus.mTextColor));
        vDecryptToken.setVisibility(keyDisplayStatus.mIsDivert ? View.VISIBLE : View.GONE);
        vDecryptLayout.setVisibility(View.VISIBLE);
    }
}

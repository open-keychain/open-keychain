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


import java.util.Date;

import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.v4.content.ContextCompat;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.InsecureBitStrength;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.KeySecurityProblem;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.NotWhitelistedCurve;
import org.sufficientlysecure.keychain.pgp.SecurityProblem.UnidentifiedKeyProblem;
import org.sufficientlysecure.keychain.ui.keyview.presenter.KeyHealthPresenter.KeyHealthClickListener;
import org.sufficientlysecure.keychain.ui.keyview.presenter.KeyHealthPresenter.KeyHealthMvpView;
import org.sufficientlysecure.keychain.ui.keyview.presenter.KeyHealthPresenter.KeyHealthStatus;
import org.sufficientlysecure.keychain.ui.keyview.view.KeyStatusList.KeyDisplayStatus;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;


public class KeyHealthView extends LinearLayout implements KeyHealthMvpView, OnClickListener {
    private final View vLayout;
    private final TextView vTitle, vSubtitle;
    private final ImageView vIcon;
    private final ImageView vExpander;
    private final KeyStatusList vKeyStatusList;
    private final View vKeyStatusDivider;
    private final View vInsecureLayout;
    private final TextView vInsecureProblem;
    private final TextView vInsecureSolution;
    private final View vExpiryLayout;
    private final TextView vExpiryText;

    private KeyHealthClickListener keyHealthClickListener;

    public KeyHealthView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setOrientation(VERTICAL);

        View view = LayoutInflater.from(context).inflate(R.layout.key_health_card_content, this, true);

        vLayout = view.findViewById(R.id.key_health_layout);
        vTitle = (TextView) view.findViewById(R.id.key_health_title);
        vSubtitle = (TextView) view.findViewById(R.id.key_health_subtitle);
        vIcon = (ImageView) view.findViewById(R.id.key_health_icon);
        vExpander = (ImageView) view.findViewById(R.id.key_health_expander);

        vLayout.setOnClickListener(this);

        vKeyStatusDivider = view.findViewById(R.id.key_health_divider);
        vKeyStatusList = (KeyStatusList) view.findViewById(R.id.key_health_status_list);

        vInsecureLayout = view.findViewById(R.id.key_insecure_layout);
        vInsecureProblem = (TextView) view.findViewById(R.id.key_insecure_problem);
        vInsecureSolution = (TextView) view.findViewById(R.id.key_insecure_solution);

        vExpiryLayout = view.findViewById(R.id.key_expiry_layout);
        vExpiryText = (TextView) view.findViewById(R.id.key_expiry_text);
    }

    private enum KeyHealthDisplayStatus {
        OK (R.string.key_health_ok_title, R.string.key_health_ok_subtitle,
                R.drawable.ic_check_black_24dp, R.color.android_green_light),
        DIVERT (R.string.key_health_divert_title, R.string.key_health_divert_subtitle,
                R.drawable.yubi_icon_24dp, R.color.md_black_1000),
        REVOKED (R.string.key_health_revoked_title, R.string.key_health_revoked_subtitle,
                R.drawable.ic_close_black_24dp, R.color.android_red_light),
        EXPIRED (R.string.key_health_expired_title, R.string.key_health_expired_subtitle,
                R.drawable.status_signature_expired_cutout_24dp, R.color.android_red_light),
        INSECURE (R.string.key_health_insecure_title, R.string.key_health_insecure_subtitle,
                R.drawable.ic_close_black_24dp, R.color.android_red_light),
        BROKEN(R.string.key_health_broken_title, R.string.key_health_broken_subtitle,
                R.drawable.broken_heart_24dp, R.color.android_red_light),
        SIGN_ONLY (R.string.key_health_sign_only_title, R.string.key_health_sign_only_subtitle,
                R.drawable.ic_check_black_24dp, R.color.android_green_light),
        STRIPPED (R.string.key_health_stripped_title, R.string.key_health_stripped_subtitle,
                R.drawable.ic_check_black_24dp, R.color.android_green_light),
        PARTIAL_STRIPPED (R.string.key_health_partial_stripped_title, R.string.key_health_partial_stripped_subtitle,
                R.drawable.ic_check_black_24dp, R.color.android_green_light);

        @StringRes
        private final int title, subtitle;
        @DrawableRes
        private final int icon;
        @ColorRes
        private final int iconColor;

        KeyHealthDisplayStatus(@StringRes int title, @StringRes int subtitle,
                @DrawableRes int icon, @ColorRes int iconColor) {
            this.title = title;
            this.subtitle = subtitle;
            this.icon = icon;
            this.iconColor = iconColor;
        }
    }

    @Override
    public void setKeyStatus(KeyHealthStatus keyHealthStatus) {
        switch (keyHealthStatus) {
            case OK:
                setKeyStatus(KeyHealthDisplayStatus.OK);
                break;
            case DIVERT:
                setKeyStatus(KeyHealthDisplayStatus.DIVERT);
                break;
            case REVOKED:
                setKeyStatus(KeyHealthDisplayStatus.REVOKED);
                break;
            case EXPIRED:
                setKeyStatus(KeyHealthDisplayStatus.EXPIRED);
                break;
            case INSECURE:
                setKeyStatus(KeyHealthDisplayStatus.INSECURE);
                break;
            case BROKEN:
                setKeyStatus(KeyHealthDisplayStatus.BROKEN);
                break;
            case STRIPPED:
                setKeyStatus(KeyHealthDisplayStatus.STRIPPED);
                break;
            case SIGN_ONLY:
                setKeyStatus(KeyHealthDisplayStatus.SIGN_ONLY);
                break;
            case PARTIAL_STRIPPED:
                setKeyStatus(KeyHealthDisplayStatus.PARTIAL_STRIPPED);
                break;
        }
    }

    @Override
    public void setPrimarySecurityProblem(KeySecurityProblem securityProblem) {
        if (securityProblem == null) {
            vInsecureLayout.setVisibility(View.GONE);
            return;
        }
        vInsecureLayout.setVisibility(View.VISIBLE);

        if (securityProblem instanceof InsecureBitStrength) {
            InsecureBitStrength insecureBitStrength = (InsecureBitStrength) securityProblem;
            vInsecureProblem.setText(getResources().getString(R.string.key_insecure_bitstrength_2048_problem,
                    KeyFormattingUtils.getAlgorithmInfo(insecureBitStrength.algorithm),
                    Integer.toString(insecureBitStrength.bitStrength)));
            vInsecureSolution.setText(R.string.key_insecure_bitstrength_2048_solution);
        } else if (securityProblem instanceof NotWhitelistedCurve) {
            NotWhitelistedCurve notWhitelistedCurve = (NotWhitelistedCurve) securityProblem;

            String curveName = KeyFormattingUtils.getCurveInfo(getContext(), notWhitelistedCurve.curveOid);
            vInsecureProblem.setText(getResources().getString(R.string.key_insecure_unknown_curve_problem, curveName));
            vInsecureSolution.setText(R.string.key_insecure_unknown_curve_solution);
        } else if (securityProblem instanceof UnidentifiedKeyProblem) {
            vInsecureProblem.setText(R.string.key_insecure_unidentified_problem);
            vInsecureSolution.setText(R.string.key_insecure_unknown_curve_solution);
        } else {
            throw new IllegalArgumentException("all subclasses of KeySecurityProblem must be handled!");
        }

    }

    @Override
    public void setPrimaryExpiryDate(Date expiry) {
        if (expiry == null) {
            vExpiryLayout.setVisibility(View.GONE);
            return;
        }
        vExpiryLayout.setVisibility(View.VISIBLE);

        String expiryText = DateFormat.getMediumDateFormat(getContext()).format(expiry);
        vExpiryText.setText(getResources().getString(R.string.key_expiry_text, expiryText));
    }

    @Override
    public void onClick(View view) {
        if (keyHealthClickListener != null) {
            keyHealthClickListener.onKeyHealthClick();
        }
    }

    @Override
    public void setOnHealthClickListener(KeyHealthClickListener keyHealthClickListener) {
        this.keyHealthClickListener = keyHealthClickListener;
        vLayout.setClickable(keyHealthClickListener != null);
    }

    @Override
    public void setShowExpander(boolean showExpander) {
        vLayout.setClickable(showExpander);
        vExpander.setVisibility(showExpander ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showExpandedState(KeyDisplayStatus certifyStatus, KeyDisplayStatus signStatus,
            KeyDisplayStatus encryptStatus) {
        if (certifyStatus == null && signStatus == null && encryptStatus == null) {
            vKeyStatusList.setVisibility(View.GONE);
            vKeyStatusDivider.setVisibility(View.GONE);
            vExpander.setImageResource(R.drawable.ic_expand_more_black_24dp);
        } else {
            vKeyStatusList.setVisibility(View.VISIBLE);
            vKeyStatusDivider.setVisibility(View.VISIBLE);
            vExpander.setImageResource(R.drawable.ic_expand_less_black_24dp);

            vKeyStatusList.setCertifyStatus(certifyStatus);
            vKeyStatusList.setSignStatus(signStatus);
            vKeyStatusList.setDecryptStatus(encryptStatus);
        }

    }

    @Override
    public void hideExpandedInfo() {
        showExpandedState(null, null, null);
    }

    private void setKeyStatus(KeyHealthDisplayStatus keyHealthDisplayStatus) {
        vTitle.setText(keyHealthDisplayStatus.title);
        vSubtitle.setText(keyHealthDisplayStatus.subtitle);
        vIcon.setImageResource(keyHealthDisplayStatus.icon);
        vIcon.setColorFilter(ContextCompat.getColor(getContext(), keyHealthDisplayStatus.iconColor));

        setVisibility(View.VISIBLE);
    }
}

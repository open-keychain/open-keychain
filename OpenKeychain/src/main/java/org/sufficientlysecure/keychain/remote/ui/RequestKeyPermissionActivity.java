/*
 * Copyright (C) 2017 Vincent Breitmoser <look@my.amazin.horse>
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

package org.sufficientlysecure.keychain.remote.ui;


import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewAnimator;

import org.openintents.openpgp.util.OpenPgpUtils.UserId;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.remote.ui.RequestKeyPermissionPresenter.RequestKeyPermissionMvpView;
import org.sufficientlysecure.keychain.ui.base.BaseActivity;


public class RequestKeyPermissionActivity extends BaseActivity {
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_REQUESTED_KEY_IDS = "requested_key_ids";


    private RequestKeyPermissionPresenter presenter;

    private View keyInfoLayout;
    private ViewAnimator viewAnimator;
    private TextView titleText;
    private ImageView iconClientApp;
    private TextView keyUserIdView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setFullScreenDialogClose(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        presenter.onClickCancelDialog();
                    }
                });

        Intent intent = getIntent();
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        long[] keyIds = intent.getLongArrayExtra(EXTRA_REQUESTED_KEY_IDS);

        presenter = RequestKeyPermissionPresenter.createRequestKeyPermissionPresenter(getBaseContext(), view);
        presenter.setupFromIntentData(packageName, keyIds);
    }

    @Override
    protected void initLayout() {
        setContentView(R.layout.api_remote_request_key_permission);

        keyUserIdView = (TextView) findViewById(R.id.select_key_item_name);
        iconClientApp = (ImageView) findViewById(R.id.icon_client_app);
        titleText = (TextView) findViewById(R.id.select_identity_key_title);
        viewAnimator = (ViewAnimator) findViewById(R.id.status_animator);
        keyInfoLayout = findViewById(R.id.key_info_layout);

        findViewById(R.id.button_allow).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onClickAllow();
            }
        });

        findViewById(R.id.button_deny).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onClickDeny();
            }
        });

        findViewById(R.id.display_key).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.onClickDisplayKey();
            }
        });
    }

    RequestKeyPermissionMvpView view = new RequestKeyPermissionMvpView() {
        @Override
        public void switchToLayoutRequestKeyChoice() {
            keyInfoLayout.setVisibility(View.VISIBLE);
            viewAnimator.setDisplayedChild(0);
        }

        @Override
        public void switchToLayoutNoSecret() {
            keyInfoLayout.setVisibility(View.VISIBLE);
            viewAnimator.setDisplayedChild(1);
        }

        @Override
        public void switchToLayoutUnknownKey() {
            keyInfoLayout.setVisibility(View.GONE);
            viewAnimator.setDisplayedChild(2);
        }

        @Override
        public void displayKeyInfo(UserId userId) {
            keyUserIdView.setText(userId.name);
        }

        @Override
        public void finish() {
            setResult(Activity.RESULT_OK);
            RequestKeyPermissionActivity.this.finish();
        }

        @Override
        public void finishAsCancelled() {
            setResult(Activity.RESULT_CANCELED);
            RequestKeyPermissionActivity.this.finish();
        }

        @Override
        public void startActivity(Intent intent) {
            RequestKeyPermissionActivity.this.startActivity(intent);
        }

        @Override
        public void setTitleText(String text) {
            titleText.setText(text);
        }

        @Override
        public void setTitleClientIcon(Drawable drawable) {
            iconClientApp.setImageDrawable(drawable);
        }
    };

}

/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.remote.ui.RemoteRegisterPresenter.RemoteRegisterView;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;


public class RemoteRegisterActivity extends FragmentActivity {
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_PACKAGE_SIGNATURE = "package_signature";
    public static final String EXTRA_DATA = "data";


    private RemoteRegisterPresenter presenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.presenter = new RemoteRegisterPresenter(getBaseContext());

        if (savedInstanceState == null) {
            RemoteRegisterDialogFragment frag = new RemoteRegisterDialogFragment();
            frag.show(getSupportFragmentManager(), "requestKeyDialog");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        Intent resultData = intent.getParcelableExtra(EXTRA_DATA);
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        byte[] packageSignature = intent.getByteArrayExtra(EXTRA_PACKAGE_SIGNATURE);

        presenter.setupFromIntentData(resultData, packageName, packageSignature);
    }

    public static class RemoteRegisterDialogFragment extends DialogFragment {
        private RemoteRegisterPresenter presenter;
        private RemoteRegisterView mvpView;

        private Button buttonAllow;
        private Button buttonCancel;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);
            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

            @SuppressLint("InflateParams")
            View view = LayoutInflater.from(theme).inflate(R.layout.api_remote_register_app, null, false);
            alert.setView(view);

            buttonAllow = (Button) view.findViewById(R.id.button_allow);
            buttonCancel = (Button) view.findViewById(R.id.button_cancel);

            setupListenersForPresenter();
            mvpView = createMvpView(view);

            return alert.create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            presenter = ((RemoteRegisterActivity) getActivity()).presenter;
            presenter.setView(mvpView);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);

            if (presenter != null) {
                presenter.onCancel();
            }
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            super.onDismiss(dialog);

            if (presenter != null) {
                presenter.setView(null);
                presenter = null;
            }
        }

        @NonNull
        private RemoteRegisterView createMvpView(View view) {
            final TextView titleText = (TextView) view.findViewById(R.id.api_register_text);
            final ImageView iconClientApp = (ImageView) view.findViewById(R.id.icon_client_app);

            return new RemoteRegisterView() {
                @Override
                public void finishWithResult(Intent resultIntent) {
                    FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    activity.setResult(RESULT_OK, resultIntent);
                    activity.finish();
                }

                @Override
                public void finishAsCancelled() {
                    FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    activity.setResult(RESULT_CANCELED);
                    activity.finish();
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

        private void setupListenersForPresenter() {
            buttonAllow.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.onClickAllow();
                }
            });

            buttonCancel.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.onClickCancel();
                }
            });
        }
    }

}

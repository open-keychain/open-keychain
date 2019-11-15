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

package org.sufficientlysecure.keychain.remote.ui;


import java.io.Serializable;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.remote.ui.SecurityProblemPresenter.RemoteSecurityProblemView;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.KeyFormattingUtils;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.widget.ToolableViewAnimator;


public class RemoteSecurityProblemDialogActivity extends FragmentActivity {
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_SECURITY_PROBLEM = "security_problem";
    public static final String EXTRA_SUPPORT_OVERRIDE = "support_override";


    private SecurityProblemPresenter presenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.presenter = new SecurityProblemPresenter(getBaseContext());

        if (savedInstanceState == null) {
            RemoteRegisterDialogFragment frag = new RemoteRegisterDialogFragment();
            frag.show(getSupportFragmentManager(), "requestKeyDialog");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        Serializable keySecurityProblem = intent.getSerializableExtra(EXTRA_SECURITY_PROBLEM);
        boolean supportOverride = intent.getBooleanExtra(EXTRA_SUPPORT_OVERRIDE, false);

        presenter.setupFromIntentData(packageName, keySecurityProblem, supportOverride);
    }

    public static class RemoteRegisterDialogFragment extends DialogFragment {
        public static final int SECONDARY_CHILD_NONE = 0;
        public static final int SECONDARY_CHILD_RECOMMENDATION = 1;
        public static final int SECONDARY_CHILD_OVERRIDE = 2;
        public static final int BUTTON_BAR_REGULAR = 0;
        public static final int BUTTON_BAR_OVERRIDE = 1;

        private SecurityProblemPresenter presenter;
        private RemoteSecurityProblemView mvpView;

        private Button buttonGotIt;
        private Button buttonViewKey;
        private Button buttonOverride;
        private Button buttonOverrideUndo;
        private Button buttonOverrideBack;
        private Button buttonOverrideConfirm;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);
            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

            @SuppressLint("InflateParams")
            View view = LayoutInflater.from(theme).inflate(R.layout.remote_security_issue_dialog, null, false);
            alert.setView(view);

            buttonGotIt = view.findViewById(R.id.button_allow);
            buttonViewKey = view.findViewById(R.id.button_view_key);
            buttonOverride = view.findViewById(R.id.button_override);
            buttonOverrideUndo = view.findViewById(R.id.button_override_undo);
            buttonOverrideBack = view.findViewById(R.id.button_override_back);
            buttonOverrideConfirm = view.findViewById(R.id.button_override_confirm);

            setupListenersForPresenter();
            mvpView = createMvpView(view);

            return alert.create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            presenter = ((RemoteSecurityProblemDialogActivity) getActivity()).presenter;
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
        private RemoteSecurityProblemView createMvpView(View view) {
            final LinearLayout insecureWarningLayout = view.findViewById(R.id.insecure_warning_layout);
            final ImageView iconClientApp = view.findViewById(R.id.icon_client_app);
            final TextView explanationText = insecureWarningLayout.findViewById(R.id.dialog_insecure_text);
            final TextView recommendText =
                    insecureWarningLayout.findViewById(R.id.dialog_insecure_recommend_text);
            final TextView overrideText =
                    insecureWarningLayout.findViewById(R.id.dialog_insecure_override_text);
            final ToolableViewAnimator secondaryLayoutAnimator =
                    insecureWarningLayout.findViewById(R.id.dialog_insecure_secondary_layout);
            final ToolableViewAnimator buttonBarAnimator =
                    view.findViewById(R.id.dialog_insecure_button_bar);

            return new RemoteSecurityProblemView() {
                private boolean layoutInitialized = false;

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
                public void finishAsSuppressed() {
                    FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    activity.setResult(RESULT_OK);
                    activity.finish();
                }

                @Override
                public void setTitleClientIcon(Drawable drawable) {
                    iconClientApp.setImageDrawable(drawable);
                }

                /* specialized layouts, for later?
                private void inflateWarningContentLayout(int dialog_insecure_mdc) {
                    insecureWarningLayout.removeAllViews();
                    getLayoutInflater(null).inflate(dialog_insecure_mdc, insecureWarningLayout);
                }
                */

                private void showGeneric(String explanationString) {
                    explanationText.setText(explanationString);
                    secondaryLayoutAnimator.setDisplayedChild(SECONDARY_CHILD_NONE, layoutInitialized);
                    buttonBarAnimator.setDisplayedChild(BUTTON_BAR_REGULAR, layoutInitialized);
                    layoutInitialized = true;
                }

                private void showGenericWithRecommendation(
                        @StringRes int explanationStringRes, @StringRes int recommendationStringRes) {
                    explanationText.setText(explanationStringRes);
                    recommendText.setText(recommendationStringRes);
                    secondaryLayoutAnimator.setDisplayedChild(SECONDARY_CHILD_RECOMMENDATION, layoutInitialized);
                    buttonBarAnimator.setDisplayedChild(BUTTON_BAR_REGULAR, layoutInitialized);
                    layoutInitialized = true;
                }

                private void showGenericWithRecommendation(
                        String explanationString, @StringRes int recommendationStringRes) {
                    explanationText.setText(explanationString);
                    recommendText.setText(recommendationStringRes);
                    secondaryLayoutAnimator.setDisplayedChild(SECONDARY_CHILD_RECOMMENDATION, layoutInitialized);
                    buttonBarAnimator.setDisplayedChild(BUTTON_BAR_REGULAR, layoutInitialized);
                    layoutInitialized = true;
                }

                @Override
                public void showLayoutMissingMdc() {
                    showGenericWithRecommendation(R.string.insecure_mdc, R.string.insecure_mdc_suggestion);
                }

                @Override
                public void showLayoutInsecureSymmetric(int symmetricAlgorithm) {
                    showGeneric(getString(R.string.insecure_symmetric_algo,
                            KeyFormattingUtils.getSymmetricCipherName(symmetricAlgorithm)));
                }

                @Override
                public void showLayoutInsecureHashAlgorithm(int hashAlgorithm) {
                    showGeneric(getString(R.string.insecure_hash_algo,
                            KeyFormattingUtils.getHashAlgoName(hashAlgorithm)));
                }

                @Override
                public void showLayoutEncryptInsecureBitsize(int algorithmId, int bitStrength) {
                    String algorithmName = KeyFormattingUtils.getAlgorithmInfo(algorithmId, null, null);

                    showGenericWithRecommendation(
                            getString(R.string.insecure_encrypt_bitstrength, algorithmName),
                            R.string.insecure_encrypt_bitstrength_suggestion);
                }

                @Override
                public void showLayoutSignInsecureBitsize(int algorithmId, int bitStrength) {
                    String algorithmName = KeyFormattingUtils.getAlgorithmInfo(algorithmId, null, null);

                    showGenericWithRecommendation(
                            getString(R.string.insecure_sign_bitstrength, algorithmName),
                            R.string.insecure_sign_bitstrength_suggestion);
                }

                @Override
                public void showLayoutEncryptNotWhitelistedCurve(String curveOid) {
                    showGenericWithRecommendation(
                            getString(R.string.insecure_encrypt_not_whitelisted_curve, curveOid),
                            R.string.insecure_report_suggestion
                    );
                }

                @Override
                public void showLayoutSignNotWhitelistedCurve(String curveOid) {
                    showGenericWithRecommendation(
                            getString(R.string.insecure_sign_not_whitelisted_curve, curveOid),
                            R.string.insecure_report_suggestion
                    );
                }

                @Override
                public void showLayoutEncryptUnidentifiedKeyProblem() {
                    showGenericWithRecommendation(
                            R.string.insecure_encrypt_unidentified,
                            R.string.insecure_report_suggestion
                    );
                }

                @Override
                public void showLayoutSignUnidentifiedKeyProblem() {
                    showGenericWithRecommendation(
                            R.string.insecure_sign_unidentified,
                            R.string.insecure_report_suggestion
                    );
                }

                @Override
                public void showOverrideMessage(int countdown) {
                    secondaryLayoutAnimator.setDisplayedChild(SECONDARY_CHILD_OVERRIDE, true);
                    buttonBarAnimator.setDisplayedChild(BUTTON_BAR_OVERRIDE, true);
                    overrideText.setText(getString(R.string.dialog_insecure_override, countdown));
                    buttonOverrideConfirm.setText(
                            getString(R.string.dialog_insecure_button_override_confirm, countdown));
                }

                @Override
                public void showViewKeyButton() {
                    buttonViewKey.setVisibility(View.VISIBLE);
                }

                @Override
                public void showOverrideButton() {
                    buttonOverride.setVisibility(View.VISIBLE);
                    buttonOverrideUndo.setVisibility(View.GONE);
                }

                @Override
                public void showOverrideUndoButton() {
                    buttonOverride.setVisibility(View.GONE);
                    buttonOverrideUndo.setVisibility(View.VISIBLE);
                }
            };
        }

        private void setupListenersForPresenter() {
            buttonGotIt.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.onClickGotIt();
                }
            });
            buttonViewKey.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.onClickViewKey();
                }
            });
            buttonOverride.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.onClickOverride();
                }
            });
            buttonOverrideUndo.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.onClickOverrideUndo();
                }
            });
            buttonOverrideBack.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.onClickOverrideBack();
                }
            });
            buttonOverrideConfirm.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    presenter.onClickOverrideConfirm();
                }
            });
        }
    }

}

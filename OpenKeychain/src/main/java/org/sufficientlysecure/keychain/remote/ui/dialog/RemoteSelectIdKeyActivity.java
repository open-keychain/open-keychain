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

package org.sufficientlysecure.keychain.remote.ui.dialog;


import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.transition.Fade;
import android.support.transition.Transition;
import android.support.transition.TransitionListenerAdapter;
import android.support.transition.TransitionManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.text.format.DateUtils;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.mikepenz.materialdrawer.util.KeyboardUtil;
import org.openintents.openpgp.util.OpenPgpApi;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.livedata.KeyInfoInteractor.KeyInfo;
import org.sufficientlysecure.keychain.operations.results.ImportKeyResult;
import org.sufficientlysecure.keychain.remote.ui.dialog.RemoteSelectIdentityKeyPresenter.RemoteSelectIdentityKeyView;
import org.sufficientlysecure.keychain.service.ImportKeyringParcel;
import org.sufficientlysecure.keychain.ui.MainActivity;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper;
import org.sufficientlysecure.keychain.ui.base.CryptoOperationHelper.AbstractCallback;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.util.recyclerview.DividerItemDecoration;
import org.sufficientlysecure.keychain.ui.util.recyclerview.RecyclerItemClickListener;
import org.sufficientlysecure.keychain.ui.widget.ToolableViewAnimator;
import timber.log.Timber;


public class RemoteSelectIdKeyActivity extends FragmentActivity {
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_PACKAGE_SIGNATURE = "package_signature";
    public static final String EXTRA_USER_ID = "user_id";
    public static final String EXTRA_SHOW_AUTOCRYPT_HINT = "show_autocrypt_hint";
    public static final String EXTRA_CURRENT_MASTER_KEY_ID = "current_master_key_id";


    private RemoteSelectIdentityKeyPresenter presenter;
    private Parcelable currentlyImportingParcel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RemoteSelectIdViewModel viewModel =
                ViewModelProviders.of(this).get(RemoteSelectIdViewModel.class);

        presenter = new RemoteSelectIdentityKeyPresenter(getBaseContext(), viewModel, this);

        KeyboardUtil.hideKeyboard(this);

        if (savedInstanceState == null) {
            RemoteSelectIdentityKeyDialogFragment frag = new RemoteSelectIdentityKeyDialogFragment();
            frag.show(getSupportFragmentManager(), "requestKeyDialog");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        String userId = intent.getStringExtra(EXTRA_USER_ID);
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
        byte[] packageSignature = intent.getByteArrayExtra(EXTRA_PACKAGE_SIGNATURE);
        boolean showAutocryptHint = intent.getBooleanExtra(EXTRA_SHOW_AUTOCRYPT_HINT, false);

        presenter.setupFromIntentData(packageName, packageSignature, userId, showAutocryptHint);
    }

    public static class RemoteSelectIdentityKeyDialogFragment extends DialogFragment {
        private RemoteSelectIdentityKeyPresenter presenter;
        private RemoteSelectIdentityKeyView mvpView;

        private RecyclerView keyChoiceList;
        private View buttonKeyListCancel;
        private View buttonNoKeysNew;
        private View buttonExplBack;
        private View buttonExplGotIt;
        private View buttonGenOkBack;
        private View buttonGenOkFinish;
        private View buttonNoKeysCancel;
        private View buttonNoKeysExisting;
        private View buttonKeyListOther;
        private View buttonOverflow;
        private View buttonGotoOpenKeychain;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Activity activity = getActivity();

            ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);
            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

            LayoutInflater layoutInflater = LayoutInflater.from(theme);
            @SuppressLint("InflateParams")
            ViewGroup view = (ViewGroup) layoutInflater.inflate(R.layout.api_select_identity_key, null, false);
            alert.setView(view);

            buttonOverflow = view.findViewById(R.id.overflow_menu);

            buttonKeyListCancel = view.findViewById(R.id.button_key_list_cancel);
            buttonKeyListOther = view.findViewById(R.id.button_key_list_other);

            buttonNoKeysNew = view.findViewById(R.id.button_no_keys_new);
            buttonNoKeysExisting = view.findViewById(R.id.button_no_keys_existing);
            buttonNoKeysCancel = view.findViewById(R.id.button_no_keys_cancel);

            buttonExplBack = view.findViewById(R.id.button_expl_back);
            buttonExplGotIt = view.findViewById(R.id.button_expl_got_it);

            buttonGenOkBack = view.findViewById(R.id.button_genok_back);
            buttonGenOkFinish = view.findViewById(R.id.button_genok_finish);

            buttonGotoOpenKeychain = view.findViewById(R.id.button_goto_openkeychain);

            keyChoiceList = view.findViewById(R.id.identity_key_list);
            keyChoiceList.setLayoutManager(new LinearLayoutManager(activity));
            keyChoiceList.addItemDecoration(
                    new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST, true));

            setupListenersForPresenter();
            mvpView = createMvpView(view, layoutInflater);

            return alert.create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            presenter = ((RemoteSelectIdKeyActivity) getActivity()).presenter;
            presenter.setView(mvpView);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);

            if (presenter != null) {
                presenter.onDialogCancel();
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
        private RemoteSelectIdentityKeyView createMvpView(final ViewGroup rootView, LayoutInflater layoutInflater) {
            // final ImageView iconClientApp = rootView.findViewById(R.id.icon_client_app);
            final KeyChoiceAdapter keyChoiceAdapter = new KeyChoiceAdapter(layoutInflater, getResources());
            final TextView titleText = rootView.findViewById(R.id.text_title_select_key);
            final TextView addressText = rootView.findViewById(R.id.text_user_id);
            final TextView autocryptHint = rootView.findViewById(R.id.key_import_autocrypt_hint);
            final ToolableViewAnimator layoutAnimator = rootView.findViewById(R.id.layout_animator);
            keyChoiceList.setAdapter(keyChoiceAdapter);

            return new RemoteSelectIdentityKeyView() {
                @Override
                public void finishAndReturn(long masterKeyId) {
                    FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    Intent resultData = new Intent();
                    resultData.putExtra(OpenPgpApi.RESULT_SIGN_KEY_ID, masterKeyId);
                    activity.setResult(RESULT_OK, resultData);
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
                public void setTitleClientIconAndName(Drawable drawable, CharSequence name) {
                    titleText.setText(getString(R.string.title_select_key, name));
                    autocryptHint.setText(getString(R.string.key_import_text_autocrypt_setup_msg, name));
                    // iconClientApp.setImageDrawable(drawable);
                    setSelectionIcons(drawable);
                }

                @Override
                public void setShowAutocryptHint(boolean showAutocryptHint) {
                    autocryptHint.setVisibility(showAutocryptHint ? View.VISIBLE : View.GONE);
                }

                private void setSelectionIcons(Drawable drawable) {
                    ConstantState constantState = drawable.getConstantState();
                    if (constantState == null) {
                        return;
                    }

                    Resources resources = getResources();
                    Drawable iconSelected = constantState.newDrawable(resources);
                    Drawable iconUnselected = constantState.newDrawable(resources);
                    DrawableCompat.setTint(iconUnselected.mutate(), ResourcesCompat.getColor(resources, R.color.md_grey_600, null));

                    keyChoiceAdapter.setSelectionDrawables(iconSelected, iconUnselected);
                }

                @Override
                public void setAddressText(String text) {
                    addressText.setText(text);
                }

                @Override
                public void showLayoutEmpty() {
                    layoutAnimator.setDisplayedChildId(R.id.select_key_layout_empty);
                }

                @Override
                public void showLayoutSelectNoKeys() {
                    layoutAnimator.setDisplayedChildId(R.id.select_key_layout_no_keys);
                    buttonOverflow.setVisibility(View.GONE);
                }

                @Override
                public void showLayoutSelectKeyList() {
                    layoutAnimator.setDisplayedChildId(R.id.select_key_layout_key_list);
                    buttonOverflow.setVisibility(View.VISIBLE);
                }

                @Override
                public void showLayoutImportExplanation() {
                    layoutAnimator.setDisplayedChildId(R.id.select_key_layout_import_expl);
                    buttonOverflow.setVisibility(View.VISIBLE);
                }

                @Override
                public void showLayoutGenerateProgress() {
                    layoutAnimator.setDisplayedChildId(R.id.select_key_layout_generate_progress);
                    buttonOverflow.setVisibility(View.GONE);
                }

                @Override
                public void showLayoutGenerateOk() {
                    layoutAnimator.setDisplayedChildId(R.id.select_key_layout_generate_ok);
                    buttonOverflow.setVisibility(View.GONE);
                }

                @Override
                public void showLayoutGenerateSave() {
                    layoutAnimator.setDisplayedChildId(R.id.select_key_layout_generate_save);
                    buttonOverflow.setVisibility(View.GONE);
                }

                @Override
                public void setKeyListData(List<KeyInfo> data) {
                    keyChoiceAdapter.setData(data);
                }

                @Override
                public void highlightKey(int position) {
                    Transition transition = new Fade().setDuration(450)
                            .addTarget(LinearLayout.class)
                            .addTarget(ImageView.class)
                            .addListener(new TransitionListenerAdapter() {
                                @Override
                                public void onTransitionEnd(@NonNull Transition transition) {
                                    presenter.onHighlightFinished();
                                }
                            });
                    TransitionManager.beginDelayedTransition(rootView, transition);

                    buttonKeyListOther.setVisibility(View.INVISIBLE);
                    buttonKeyListCancel.setVisibility(View.INVISIBLE);
                    keyChoiceAdapter.setActiveItem(position);
                }

                @Override
                public void showImportInternalError() {
                    Toast.makeText(getContext(), R.string.error_save_key_internal, Toast.LENGTH_LONG).show();
                }

                @Override
                public void launchImportOperation(ImportKeyringParcel importKeyringParcel) {
                    RemoteSelectIdKeyActivity activity = (RemoteSelectIdKeyActivity) getActivity();
                    if (activity == null) {
                        return;
                    }
                    activity.launchImportOperation(importKeyringParcel);
                }

                @Override
                public void displayOverflowMenu() {
                    ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(getActivity());
                    PopupMenu menu = new PopupMenu(theme, buttonOverflow);
                    menu.inflate(R.menu.select_identity_menu);
                    menu.setOnMenuItemClickListener(item -> {
                        presenter.onClickMenuListAllKeys();
                        return false;
                    });
                    menu.show();
                }

                @Override
                public void showOpenKeychainIntent() {
                    Activity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    Intent intent = new Intent(activity.getApplicationContext(), MainActivity.class);
                    startActivity(intent);
                }
            };
        }

        private void setupListenersForPresenter() {
            buttonOverflow.setOnClickListener(view -> presenter.onClickOverflowMenu());

            buttonKeyListOther.setOnClickListener(view -> presenter.onClickKeyListOther());
            buttonKeyListCancel.setOnClickListener(view -> presenter.onClickKeyListCancel());

            buttonNoKeysNew.setOnClickListener(view -> presenter.onClickNoKeysGenerate());
            buttonNoKeysExisting.setOnClickListener(view -> presenter.onClickNoKeysExisting());
            buttonNoKeysCancel.setOnClickListener(view -> presenter.onClickNoKeysCancel());

            buttonExplBack.setOnClickListener(view -> presenter.onClickExplanationBack());
            buttonExplGotIt.setOnClickListener(view -> presenter.onClickExplanationGotIt());

            buttonGenOkBack.setOnClickListener(view -> presenter.onClickGenerateOkBack());
            buttonGenOkFinish.setOnClickListener(view -> presenter.onClickGenerateOkFinish());

            buttonGotoOpenKeychain.setOnClickListener(view -> presenter.onClickGoToOpenKeychain());

            keyChoiceList.addOnItemTouchListener(new RecyclerItemClickListener(getContext(),
                    (view, position) -> presenter.onKeyItemClick(position)));
        }
    }

    private void launchImportOperation(ImportKeyringParcel importKeyringParcel) {
        if (currentlyImportingParcel != null) {
            Timber.e("Starting import while already running? Inconsistent state!");
            return;
        }

        currentlyImportingParcel = importKeyringParcel;
        importOpHelper.cryptoOperation();
    }

    private static class KeyChoiceAdapter extends Adapter<KeyChoiceViewHolder> {
        private final LayoutInflater layoutInflater;
        private final Resources resources;
        private List<KeyInfo> data;
        private Drawable iconUnselected;
        private Drawable iconSelected;
        private Integer activeItem;

        KeyChoiceAdapter(LayoutInflater layoutInflater, Resources resources) {
            this.layoutInflater = layoutInflater;
            this.resources = resources;
        }

        @Override
        public KeyChoiceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View keyChoiceItemView = layoutInflater.inflate(R.layout.api_select_identity_item, parent, false);
            return new KeyChoiceViewHolder(keyChoiceItemView);
        }

        void setActiveItem(Integer activeItem) {
            this.activeItem = activeItem;
            notifyDataSetChanged();
        }

        @Override
        public void onBindViewHolder(KeyChoiceViewHolder holder, int position) {
            KeyInfo keyInfo = data.get(position);
            boolean hasActiveItem = activeItem != null;
            boolean isActiveItem = hasActiveItem && position == activeItem;

            Drawable icon = isActiveItem ? iconSelected : iconUnselected;
            holder.bind(keyInfo, icon);

            holder.itemView.setVisibility(!hasActiveItem || isActiveItem ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        public int getItemCount() {
            return data != null ? data.size() : 0;
        }

        public void setData(List<KeyInfo> data) {
            this.data = data;
            notifyDataSetChanged();
        }

        void setSelectionDrawables(Drawable iconSelected, Drawable iconUnselected) {
            this.iconSelected = iconSelected;
            this.iconUnselected = iconUnselected;

            notifyDataSetChanged();
        }
    }

    private static class KeyChoiceViewHolder extends RecyclerView.ViewHolder {
        private final TextView vName;
        private final TextView vCreation = (TextView) itemView.findViewById(R.id.key_list_item_creation);
        private final ImageView vIcon;

        KeyChoiceViewHolder(View itemView) {
            super(itemView);

            vName = itemView.findViewById(R.id.key_list_item_name);
            vIcon = itemView.findViewById(R.id.key_list_item_icon);
        }

        void bind(KeyInfo keyInfo, Drawable selectionIcon) {
            Context context = vCreation.getContext();

            String email = keyInfo.getEmail();
            String name = keyInfo.getName();
            if (email != null) {
                vName.setText(context.getString(R.string.use_key, email));
            } else if (name != null) {
                vName.setText(context.getString(R.string.use_key, name));
            } else {
                vName.setText(context.getString(R.string.use_key_no_name));
            }

            String dateTime = DateUtils.formatDateTime(context, keyInfo.getCreationDate(),
                    DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_TIME |
                            DateUtils.FORMAT_SHOW_YEAR | DateUtils.FORMAT_ABBREV_MONTH);
            vCreation.setText(context.getString(R.string.label_key_created, dateTime));

            vIcon.setImageDrawable(selectionIcon);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (importOpHelper.handleActivityResult(requestCode, resultCode, data)) {
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private CryptoOperationHelper<Parcelable, ImportKeyResult> importOpHelper =
            new CryptoOperationHelper<>(0, this,
                    new AbstractCallback<Parcelable, ImportKeyResult>() {
                        @Override
                        public Parcelable createOperationInput() {
                            return currentlyImportingParcel;
                        }

                        @Override
                        public void onCryptoOperationError(ImportKeyResult result) {
                            currentlyImportingParcel = null;
                            presenter.onImportOpError();
                        }

                        @Override
                        public void onCryptoOperationSuccess(ImportKeyResult result) {
                            currentlyImportingParcel = null;
                            presenter.onImportOpSuccess(result);
                        }
                    }, null);

}

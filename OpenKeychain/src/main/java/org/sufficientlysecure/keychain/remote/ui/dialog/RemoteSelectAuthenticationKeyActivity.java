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
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Drawable.ConstantState;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.mikepenz.materialdrawer.util.KeyboardUtil;
import org.openintents.ssh.authentication.SshAuthenticationApi;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.daos.ApiAppDao;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.remote.ui.RemoteSecurityTokenOperationActivity;
import org.sufficientlysecure.keychain.remote.ui.dialog.RemoteSelectAuthenticationKeyPresenter.RemoteSelectAuthenticationKeyView;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.util.recyclerview.DividerItemDecoration;
import org.sufficientlysecure.keychain.ui.util.recyclerview.RecyclerItemClickListener;


public class RemoteSelectAuthenticationKeyActivity extends FragmentActivity {
    public static final String EXTRA_PACKAGE_NAME = "package_name";


    private RemoteSelectAuthenticationKeyPresenter presenter;
    private String packageName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter = new RemoteSelectAuthenticationKeyPresenter(getBaseContext(), this);

        KeyboardUtil.hideKeyboard(this);

        if (savedInstanceState == null) {
            RemoteSelectAuthenticationKeyDialogFragment frag = new RemoteSelectAuthenticationKeyDialogFragment();
            frag.show(getSupportFragmentManager(), "selectAuthenticationKeyDialog");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);

        SelectAuthKeyViewModel viewModel = ViewModelProviders.of(this).get(SelectAuthKeyViewModel.class);
        viewModel.setPackageName(packageName);

        presenter.setupFromViewModel(viewModel);
    }

    public static class SelectAuthKeyViewModel extends ViewModel {
        private LiveData<List<UnifiedKeyInfo>> keyInfoLiveData;
        private String packageName;

        public LiveData<List<UnifiedKeyInfo>> getKeyInfoLiveData(Context context) {
            if (keyInfoLiveData == null) {
                keyInfoLiveData = new GenericLiveData<>(context, () -> {
                    KeyRepository keyRepository = KeyRepository.create(context);
                    return keyRepository.getAllUnifiedKeyInfoWithAuthKeySecret();
                });
            }
            return keyInfoLiveData;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageName() {
            return packageName;
        }
    }

    private void onKeySelected(long masterKeyId) {
        Intent callingIntent = getIntent();
        Intent originalIntent = callingIntent.getParcelableExtra(
                RemoteSecurityTokenOperationActivity.EXTRA_DATA);

        ApiAppDao apiAppDao = ApiAppDao.getInstance(getBaseContext());
        apiAppDao.addAllowedKeyIdForApp(packageName, masterKeyId);

        originalIntent.putExtra(SshAuthenticationApi.EXTRA_KEY_ID, String.valueOf(masterKeyId));

        setResult(RESULT_OK, originalIntent);
        finish();
    }

    public static class RemoteSelectAuthenticationKeyDialogFragment extends DialogFragment {
        private RemoteSelectAuthenticationKeyPresenter presenter;
        private RemoteSelectAuthenticationKeyView mvpView;

        private Button buttonSelect;
        private Button buttonCancel;
        private RecyclerView keyChoiceList;

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Activity activity = requireActivity();

            ContextThemeWrapper theme = ThemeChanger.getDialogThemeWrapper(activity);
            CustomAlertDialogBuilder alert = new CustomAlertDialogBuilder(theme);

            LayoutInflater layoutInflater = LayoutInflater.from(theme);
            @SuppressLint("InflateParams")
            View view = layoutInflater.inflate(R.layout.api_remote_select_authentication_key, null, false);
            alert.setView(view);

            buttonSelect = view.findViewById(R.id.button_select);
            buttonCancel = view.findViewById(R.id.button_cancel);

            keyChoiceList = view.findViewById(R.id.authentication_key_list);
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

            presenter = ((RemoteSelectAuthenticationKeyActivity) requireActivity()).presenter;
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
        private RemoteSelectAuthenticationKeyView createMvpView(View view, LayoutInflater layoutInflater) {
            final ImageView iconClientApp = view.findViewById(R.id.icon_client_app);
            final DialogKeyChoiceAdapter keyChoiceAdapter = new DialogKeyChoiceAdapter(requireContext(), layoutInflater);
            keyChoiceList.setAdapter(keyChoiceAdapter);

            return new RemoteSelectAuthenticationKeyView() {
                @Override
                public void finish(long masterKeyId) {
                    FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    ((RemoteSelectAuthenticationKeyActivity)activity).onKeySelected(masterKeyId);
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
                public void setTitleClientIcon(Drawable drawable) {
                    iconClientApp.setImageDrawable(drawable);

                    Resources resources = getResources();
                    ConstantState constantState = drawable.getConstantState();
                    Drawable iconSelected = constantState.newDrawable(resources);
                    Drawable iconUnselected = constantState.newDrawable(resources);
                    DrawableCompat.setTint(iconUnselected.mutate(), ResourcesCompat.getColor(resources, R.color.md_grey_300, null));

                    keyChoiceAdapter.setSelectionDrawables(iconSelected, iconUnselected);
                }

                @Override
                public void setKeyListData(List<UnifiedKeyInfo> data) {
                    keyChoiceAdapter.setData(data);
                }

                @Override
                public void setActiveItem(Integer position) {
                    keyChoiceAdapter.setActiveItem(position);
                }

                @Override
                public void setEnableSelectButton(boolean enabled) {
                    buttonSelect.setEnabled(enabled);
                }
            };
        }

        private void setupListenersForPresenter() {
            buttonSelect.setOnClickListener(view -> presenter.onClickSelect());
            buttonCancel.setOnClickListener(view -> presenter.onClickCancel());
            keyChoiceList.addOnItemTouchListener(new RecyclerItemClickListener(getContext(),
                    (view, position) -> presenter.onKeyItemClick(position)));
        }
    }
}

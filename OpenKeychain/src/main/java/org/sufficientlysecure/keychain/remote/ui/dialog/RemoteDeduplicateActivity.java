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
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.livedata.GenericLiveData;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.remote.ui.RemoteSecurityTokenOperationActivity;
import org.sufficientlysecure.keychain.remote.ui.dialog.RemoteDeduplicatePresenter.RemoteDeduplicateView;
import org.sufficientlysecure.keychain.ui.dialog.CustomAlertDialogBuilder;
import org.sufficientlysecure.keychain.ui.util.ThemeChanger;
import org.sufficientlysecure.keychain.ui.util.recyclerview.DividerItemDecoration;


public class RemoteDeduplicateActivity extends FragmentActivity {
    public static final String EXTRA_PACKAGE_NAME = "package_name";
    public static final String EXTRA_DUPLICATE_EMAILS = "duplicate_emails";


    private RemoteDeduplicatePresenter presenter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        presenter = new RemoteDeduplicatePresenter(getBaseContext(), this);

        if (savedInstanceState == null) {
            RemoteDeduplicateDialogFragment frag = new RemoteDeduplicateDialogFragment();
            frag.show(getSupportFragmentManager(), "requestKeyDialog");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        Intent intent = getIntent();
        List<String> dupAddresses = intent.getStringArrayListExtra(EXTRA_DUPLICATE_EMAILS);
        String duplicateAddress = dupAddresses.get(0);
        String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);

        DeduplicateViewModel viewModel = ViewModelProviders.of(this).get(DeduplicateViewModel.class);
        viewModel.setDuplicateAddress(duplicateAddress);
        viewModel.setPackageName(packageName);

        presenter.setupFromViewModel(viewModel);
    }

    public static class DeduplicateViewModel extends ViewModel {
        private String duplicateAddress;
        private LiveData<List<UnifiedKeyInfo>> keyInfoLiveData;
        private String packageName;

        public LiveData<List<UnifiedKeyInfo>> getKeyInfoLiveData(Context context) {
            if (keyInfoLiveData == null) {
                keyInfoLiveData = new GenericLiveData<>(context, () -> {
                    KeyRepository keyRepository = KeyRepository.create(context);
                    return keyRepository.getUnifiedKeyInfosByMailAddress(duplicateAddress);
                });
            }
            return keyInfoLiveData;
        }

        public void setDuplicateAddress(String duplicateAddress) {
            this.duplicateAddress = duplicateAddress;
        }

        public void setPackageName(String packageName) {
            this.packageName = packageName;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getDuplicateAddress() {
            return duplicateAddress;
        }
    }

    public static class RemoteDeduplicateDialogFragment extends DialogFragment {
        private RemoteDeduplicatePresenter presenter;
        private RemoteDeduplicateView mvpView;

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
            View view = layoutInflater.inflate(R.layout.api_remote_deduplicate, null, false);
            alert.setView(view);

            buttonSelect = view.findViewById(R.id.button_select);
            buttonCancel = view.findViewById(R.id.button_cancel);

            keyChoiceList = view.findViewById(R.id.duplicate_key_list);
            keyChoiceList.setLayoutManager(new LinearLayoutManager(activity));
            keyChoiceList.addItemDecoration(
                    new DividerItemDecoration(activity, DividerItemDecoration.VERTICAL_LIST, true));

            setupListenersForPresenter();
            mvpView = createMvpView(view);

            return alert.create();
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            presenter = ((RemoteDeduplicateActivity) requireActivity()).presenter;
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
        private RemoteDeduplicateView createMvpView(View view) {
            final TextView addressText = view.findViewById(R.id.select_key_item_name);

            return new RemoteDeduplicateView() {
                @Override
                public void finish() {
                    FragmentActivity activity = getActivity();
                    if (activity == null) {
                        return;
                    }

                    Intent passthroughData = activity.getIntent().getParcelableExtra(
                            RemoteSecurityTokenOperationActivity.EXTRA_DATA);
                    activity.setResult(RESULT_OK, passthroughData);
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
                public void showNoSelectionError() {
                    Toast.makeText(getContext(), "No key selected!", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void setAddressText(String text) {
                    addressText.setText(text);
                }

                @Override
                public void setKeyListAdapter(Adapter adapter) {
                    keyChoiceList.setAdapter(adapter);
                }
            };
        }

        private void setupListenersForPresenter() {
            buttonSelect.setOnClickListener(view -> presenter.onClickSelect());
            buttonCancel.setOnClickListener(view -> presenter.onClickCancel());
        }
    }

}

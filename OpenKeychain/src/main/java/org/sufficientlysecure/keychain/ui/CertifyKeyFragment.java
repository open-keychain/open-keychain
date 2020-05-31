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

package org.sufficientlysecure.keychain.ui;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.app.Activity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProviders;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;

import org.sufficientlysecure.keychain.Constants;
import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.model.SubKey.UnifiedKeyInfo;
import org.sufficientlysecure.keychain.operations.results.CertifyResult;
import org.sufficientlysecure.keychain.operations.results.OperationResult;
import org.sufficientlysecure.keychain.daos.KeyRepository;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel;
import org.sufficientlysecure.keychain.service.CertifyActionsParcel.CertifyAction;
import org.sufficientlysecure.keychain.service.input.CryptoInputParcel;
import org.sufficientlysecure.keychain.ui.base.CachingCryptoOperationFragment;
import org.sufficientlysecure.keychain.ui.keyview.GenericViewModel;
import org.sufficientlysecure.keychain.ui.util.FormattingUtils;
import org.sufficientlysecure.keychain.ui.util.Notify;
import org.sufficientlysecure.keychain.ui.widget.KeySpinner;
import org.sufficientlysecure.keychain.util.Preferences;


public class CertifyKeyFragment extends CachingCryptoOperationFragment<CertifyActionsParcel, CertifyResult> {
    private KeyRepository keyRepository;

    private CheckBox uploadKeyCheckbox;
    private KeySpinner certifyKeySpinner;
    private MultiUserIdsFragment multiUserIdsFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        keyRepository = KeyRepository.create(requireContext());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = requireActivity();

        GenericViewModel viewModel = ViewModelProviders.of(this).get(GenericViewModel.class);
        LiveData<List<UnifiedKeyInfo>> liveData = viewModel.getGenericLiveData(
                requireContext(), keyRepository::getAllUnifiedKeyInfoWithSecret);
        liveData.observe(this, certifyKeySpinner::setData);

        if (savedInstanceState == null) {
            // preselect certify key id if given
            long certifyKeyId = activity.getIntent()
                    .getLongExtra(CertifyKeyActivity.EXTRA_CERTIFY_KEY_ID, Constants.key.none);
            if (certifyKeyId != Constants.key.none) {
                UnifiedKeyInfo unifiedKeyInfo = keyRepository.getUnifiedKeyInfo(certifyKeyId);
                if (unifiedKeyInfo != null && unifiedKeyInfo.can_certify()) {
                    certifyKeySpinner.setPreSelectedKeyId(certifyKeyId);
                }
            }
        }

        OperationResult result = activity.getIntent().getParcelableExtra(CertifyKeyActivity.EXTRA_RESULT);
        if (result != null) {
            // display result from import
            result.createNotify(activity).show();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup superContainer, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.certify_key_fragment, superContainer, false);

        certifyKeySpinner = view.findViewById(R.id.certify_key_spinner);
        uploadKeyCheckbox = view.findViewById(R.id.sign_key_upload_checkbox);
        multiUserIdsFragment = (MultiUserIdsFragment)
                getChildFragmentManager().findFragmentById(R.id.multi_user_ids_fragment);

        certifyKeySpinner.setShowNone(R.string.choice_select_cert);

        // make certify image gray, like action icons
        ImageView vActionCertifyImage =
                view.findViewById(R.id.certify_key_action_certify_image);
        vActionCertifyImage.setColorFilter(FormattingUtils.getColorFromAttr(requireActivity(), R.attr.colorTertiaryText),
                PorterDuff.Mode.SRC_IN);

        View vCertifyButton = view.findViewById(R.id.certify_key_certify_button);
        vCertifyButton.setOnClickListener(v -> {
            long selectedKeyId = certifyKeySpinner.getSelectedKeyId();
            if (selectedKeyId == Constants.key.none) {
                Notify.create(getActivity(), getString(R.string.select_key_to_certify),
                        Notify.Style.ERROR).show();
            } else {
                cryptoOperation(CryptoInputParcel.createCryptoInputParcel(new Date()));
            }
        });

        return view;
    }

    @Override
    public CertifyActionsParcel createOperationInput() {
        // Bail out if there is not at least one user id selected
        ArrayList<CertifyAction> certifyActions = multiUserIdsFragment.getSelectedCertifyActions();
        if (certifyActions.isEmpty()) {
            Notify.create(getActivity(), "No identities selected!", Notify.Style.ERROR).show();
            return null;
        }

        long selectedKeyId = certifyKeySpinner.getSelectedKeyId();

        // fill values for this action
        CertifyActionsParcel.Builder actionsParcel = CertifyActionsParcel.builder(selectedKeyId);
        actionsParcel.addActions(certifyActions);

        if (uploadKeyCheckbox.isChecked()) {
            actionsParcel.setParcelableKeyServer(Preferences.getPreferences(getActivity()).getPreferredKeyserver());
        }

        // cache for next cryptoOperation loop
        CertifyActionsParcel certifyActionsParcel = actionsParcel.build();
        cacheActionsParcel(certifyActionsParcel);
        return certifyActionsParcel;
    }

    @Override
    public void onQueuedOperationSuccess(CertifyResult result) {
        // protected by Queueing*Fragment
        Activity activity = requireActivity();

        Intent intent = new Intent();
        intent.putExtra(CertifyResult.EXTRA_RESULT, result);
        activity.setResult(Activity.RESULT_OK, intent);
        activity.finish();

    }

}

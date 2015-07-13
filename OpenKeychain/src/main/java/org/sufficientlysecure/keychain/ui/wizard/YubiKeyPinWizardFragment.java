package org.sufficientlysecure.keychain.ui.wizard;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.base.WizardFragment;


public class YubiKeyPinWizardFragment extends WizardFragment implements
        YubiKeyPinWizardFragmentViewModel.OnViewModelEventBind {

    private TextView mCreateYubiKeyPin;
    private TextView mCreateYubiKeyAdminPin;
    private YubiKeyPinWizardFragmentViewModel mYubiKeyPinWizardFragmentViewModel;

    /**
     * Creates new instance of this fragment
     */
    public static YubiKeyPinWizardFragment newInstance() {
        return new YubiKeyPinWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mYubiKeyPinWizardFragmentViewModel = new YubiKeyPinWizardFragmentViewModel(this);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wizard_yubi_pin_fragment, container, false);
        mCreateYubiKeyAdminPin = (TextView) view.findViewById(R.id.create_yubi_key_admin_pin);
        mCreateYubiKeyPin = (TextView) view.findViewById(R.id.create_yubi_key_pin);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mYubiKeyPinWizardFragmentViewModel.prepareViewModel(savedInstanceState, getArguments(),
                getActivity());
        mYubiKeyPinWizardFragmentViewModel.onYubiKeyPinDataSet(mWizardFragmentListener.getYubiKeyPin(),
                mWizardFragmentListener.getYubiKeyAdminPin());
    }

    @Override
    public void updatePinText(CharSequence text) {
        mCreateYubiKeyPin.setText(text);
    }

    @Override
    public void updateAdminPinText(CharSequence text) {
        mCreateYubiKeyAdminPin.setText(text);
    }
}

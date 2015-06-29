package org.sufficientlysecure.keychain.ui.keyunlock.options;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.base.UnlockOptionFragment;

public class NFCUnlockOptionFragment extends UnlockOptionFragment {

    public static PinUnlockOptionFragment newInstance() {
        return new PinUnlockOptionFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.wizard_nfc_option_fragment, container, false);
    }
}

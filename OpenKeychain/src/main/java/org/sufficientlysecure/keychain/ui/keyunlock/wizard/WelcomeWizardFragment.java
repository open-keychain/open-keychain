package org.sufficientlysecure.keychain.ui.keyunlock.wizard;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.activities.WizardCommonListener;
import org.sufficientlysecure.keychain.ui.keyunlock.base.WizardFragment;


public class WelcomeWizardFragment extends WizardFragment {
    private TextView mCreatekeycreatekeybutton;
    private TextView mCreatekeycancel;
    private RelativeLayout mCreatekeybuttons;
    private WizardCommonListener mWizardCommonListener;

    public static WelcomeWizardFragment newInstance() {
        return new WelcomeWizardFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.wizard_welcome_fragment, container, false);
        mCreatekeybuttons = (RelativeLayout) view.findViewById(R.id.create_key_buttons);
        mCreatekeycancel = (TextView) view.findViewById(R.id.create_key_cancel);
        mCreatekeycreatekeybutton = (TextView) view.findViewById(R.id.create_key_create_key_button);
        mCreatekeycreatekeybutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mWizardCommonListener != null)
                {
                    mWizardCommonListener.onAdvanceToNextWizardStep();
                }
            }
        });

        if(mWizardCommonListener != null) {
            mWizardCommonListener.onHideNavigationButtons(true);
        }
        return view;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mWizardCommonListener = (WizardCommonListener)activity;
        }catch(ClassCastException e) {
            e.printStackTrace();
        }
    }
}

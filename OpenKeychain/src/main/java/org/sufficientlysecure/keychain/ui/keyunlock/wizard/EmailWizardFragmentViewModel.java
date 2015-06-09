package org.sufficientlysecure.keychain.ui.keyunlock.wizard;

import android.content.Context;
import android.os.Bundle;

import org.sufficientlysecure.keychain.ui.keyunlock.adapter.WizardEmailAdapter;
import org.sufficientlysecure.keychain.ui.keyunlock.base.BaseViewModel;

import java.util.ArrayList;
import java.util.regex.Pattern;


/**
 * Email Wizard Fragment View Model
 */
public class EmailWizardFragmentViewModel implements BaseViewModel {
    private ArrayList<WizardEmailAdapter.ViewModel> mAdditionalEmailModels;
    private Context mContext;

    // NOTE: Do not use more complicated pattern like defined in android.util.Patterns.EMAIL_ADDRESS
    // EMAIL_ADDRESS fails for mails with umlauts for example
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\S]+@[\\S]+\\.[a-z]+$");

    @Override
    public void prepareViewModel(Bundle savedInstanceState, Bundle arguments, Context context) {
        mContext = context;
        mAdditionalEmailModels = new ArrayList<>();
    }

    @Override
    public void saveViewModelState(Bundle outState) {

    }

    @Override
    public void onViewModelCreated() {

    }

    /**
     * Checks the email format
     * Uses the default Android Email Pattern
     *
     * @param email
     * @return
     */
    public boolean isEmailFormatValid(String email) {
        // check for email format or if the user did any input
        return !(email.length() == 0 || !EMAIL_PATTERN.matcher(email).matches());
    }

    public ArrayList<WizardEmailAdapter.ViewModel> getAdditionalEmailModels() {
        return mAdditionalEmailModels;
    }

    public void setAdditionalEmailModels(ArrayList<WizardEmailAdapter.ViewModel>
                                                 additionalEmailModels) {
        mAdditionalEmailModels = additionalEmailModels;
    }
}

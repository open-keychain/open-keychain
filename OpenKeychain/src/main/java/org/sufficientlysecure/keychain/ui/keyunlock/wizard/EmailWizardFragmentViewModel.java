package org.sufficientlysecure.keychain.ui.keyunlock.wizard;

import android.content.Context;
import android.os.Bundle;
import android.widget.EditText;

import org.sufficientlysecure.keychain.R;
import org.sufficientlysecure.keychain.ui.keyunlock.adapter.WizardEmailAdapter;
import org.sufficientlysecure.keychain.ui.keyunlock.base.BaseViewModel;
import org.sufficientlysecure.keychain.ui.util.Notify;

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
    public void restoreViewModelState(Bundle savedInstanceState) {

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


    /**
     * Checks for duplicated emails inside the additional email adapter.
     *
     * @param email
     * @return
     */
    public boolean isEmailDuplicatedInsideAdapter(String email) {
        //check for duplicated emails inside the adapter
        for (WizardEmailAdapter.ViewModel model : mAdditionalEmailModels) {
            if (email.equals(model.getEmail())) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns all additional emails.
     *
     * @return
     */
    public ArrayList<String> getAdditionalEmails() {
        ArrayList<String> emails = new ArrayList<>();
        for (WizardEmailAdapter.ViewModel holder : mAdditionalEmailModels) {
            emails.add(holder.toString());
        }
        return emails;
    }


    public ArrayList<WizardEmailAdapter.ViewModel> getAdditionalEmailModels() {
        return mAdditionalEmailModels;
    }

    public void setAdditionalEmailModels(ArrayList<WizardEmailAdapter.ViewModel>
                                                 additionalEmailModels) {
        mAdditionalEmailModels = additionalEmailModels;
    }
}
